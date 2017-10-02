/**
 * 
 */
package com.tazhi.rose.event.mongo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.event.EventStore;
import com.tazhi.rose.exception.ConcurrencyViolationException;
import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.util.JsonUtils;

/**
 * Mongodb事件仓库实现。
 * 
 * @author Evan Wu
 *
 */
public class MongoEventStore implements EventStore {
	private static final Logger logger = LoggerFactory.getLogger(MongoEventStore.class);
	
	private MongoClient mongo;
	private MongoDatabase db;
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;
	private int eventSavingTimeout = 2000; // 2 seconds
	private int eventResaveInterval = 2000; // 2 seconds
	private int eventSendingTimeout = 2000;
	private long eventResentInterval = 2000;
	@Value("${spring.application.name}")
	private String applicationName;
	private EventBus eventBus;
	
	public MongoEventStore() {}
	
	public MongoEventStore(MongoClient mongo, String database, EventBus eventBus) {
		this.mongo = mongo;
		this.db = mongo.getDatabase(database);
		this.eventBus = eventBus;
	}
	
	@PostConstruct
	public void init() {
		if (mongo == null) {
			if (username != null) {
				MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());
				mongo = new MongoClient(new ServerAddress(host, port), Arrays.asList(credential));
			} else {
				mongo = new MongoClient(new ServerAddress(host, port));
			}
			db = mongo.getDatabase(database);
		}
		
		// start the daemon thread to scan and re-save the batched events.
		Thread daemon = new Thread(new Runnable() {
			public void run() {
				do {
					try {
						Thread.sleep(eventResaveInterval);
					} catch (InterruptedException e) {
						//
					}
					
					try {
						MongoIterable<String> colls = db.listCollectionNames();
						for (String coll : colls) {
							if (coll.startsWith("event_trans_")) {
								String entityName = coll.substring("event_trans_".length());
								MongoCollection<Document> eventTrans = db.getCollection(coll);
								Calendar timeout = Calendar.getInstance();
								timeout.add(Calendar.MILLISECOND, - eventSavingTimeout);
								FindIterable<Document> docs = eventTrans.find(Filters.and(Filters.eq("state", "pending"), Filters.lt("lastUpdated", timeout.getTime()))).sort(Sorts.ascending("lastUpdated"));
								
								MongoCollection<Document> entityEvents = db.getCollection("events_" + entityName);
								for (Document outdated : docs) {
									@SuppressWarnings("unchecked")
									List<Document> listOfEvents = outdated.get("events", List.class);
									for (Document doc : listOfEvents) {
										// ordered insert, ignore already inserted ones
										try {
											doc.append("transId", outdated.get("_id"));
											doc.append("eventSent", false);
											doc.append("lastUpdated", new Date());
											entityEvents.insertOne(doc);
										} catch (MongoWriteException iwex) {
											// if is duplicate key exception ignore
											if (iwex.getCode() != 11000)
												logger.error("Can not re-save timeout events", iwex);
											else if (logger.isDebugEnabled())
												logger.debug("Event " + doc + " already re-saved.");
										}
									}
									eventTrans.updateOne(Filters.eq(outdated.get("_id")),
											Updates.combine(Updates.set("state", "saved"), 
													Updates.currentDate("lastUpdated"))
									);
								}
							}
						}
					} catch (Exception e) {
						logger.error("MongoDB error", e);
					}
				} while (true);
			}
		}, "EventStore-events-saving-processor");
		
		daemon.setDaemon(true);
		daemon.start();
		
		Thread eventDaemon = new Thread(new Runnable(){
			public void run() {
				do {
					try {
						Thread.sleep(eventResentInterval);
					} catch (InterruptedException e) {
						//
					}
					
					try {
						MongoIterable<String> colls = db.listCollectionNames();
						for (String coll : colls) {
							if (coll.startsWith("events_")) {
								MongoCollection<Document> entityEvents = db.getCollection(coll);
								Calendar timeout = Calendar.getInstance();
								timeout.add(Calendar.MILLISECOND, - eventSendingTimeout);
								FindIterable<Document> docs = entityEvents.find(Filters.and(Filters.eq("eventSent", false), Filters.lt("lastUpdated", timeout.getTime()))).sort(Sorts.ascending("lastUpdated"));
								for (Document doc : docs) {
									@SuppressWarnings("unchecked")
									Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(doc.getString("eventType"));
									DomainEvent evt = JsonUtils.fromJson(doc.toJson(), clazz);
									eventBus.publish(evt);
									entityEvents.updateOne(Filters.eq(doc.get("_id")),
											Updates.combine(Updates.set("eventSent", true), 
													Updates.currentDate("lastUpdated"))
									);
								}
							}
						}
					} catch (Exception e) {
						logger.error("Failed to re-publish event", e);
					}
				} while (true);
			}
		}, "EventStore-events-unsent-processor");
		eventDaemon.setDaemon(true);
		eventDaemon.start();
	}
	
	@PreDestroy
	public void destroy() {
		if (mongo != null)
			mongo.close();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void markEventPublished(Class<? extends AbstractEntity> entityClass, DomainEvent... events) {
		Assert.notNull(events, "event(s) can not be null");
		DomainEvent first = events[0];
		MongoCollection<Document> entityEvents = db.getCollection("events_" + entityClass.getSimpleName());
		entityEvents.updateMany(Filters.and(Filters.eq("entityId", first.getEntityId()), Filters.eq("version", first.getVersion())), Updates.combine(Updates.set("eventSent", true), Updates.currentDate("lastUpdated")));
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void append(Class<? extends AbstractEntity> entityClass, DomainEvent... events)
			throws ConcurrencyViolationException {
		Assert.notNull(events, "event(s) can not be null");
		DomainEvent first = events[0];
		
		/*
		 * collection: event_trans_entityName: { "_id": "", "entityId": id, "version": 1,  "state": "pending|saved", "events": [], lastUpdated": new Date() }
		 * unique index on: entityId, version(starting version, events may contain multiple version events)
		 *
		 *
		 * collection: events_entityName: { "_id": "", "transId": "", "eventType": eventName, "entityId": "", "version": 1, "eventSent": false }
		 * unique index on: entityId, version XXX one event for one version
		 */
		MongoCollection<Document> eventTrans = db.getCollection("event_trans_" + entityClass.getSimpleName());
		eventTrans.createIndex(Indexes.ascending("entityId", "version"), new IndexOptions().unique(true));
		
		MongoCollection<Document> entityEvents = db.getCollection("events_" + entityClass.getSimpleName());
		entityEvents.createIndex(Indexes.ascending("entityId", "version"), new IndexOptions().unique(true));
		
		Document trans = new Document();
		trans.put("entityId", first.getEntityId());
		trans.put("version", first.getVersion());
		trans.put("state", "pending");
		List<Document> evts = new ArrayList<Document>();
		for (DomainEvent de : events) {
			if (applicationName != null)
				de.setOriginator(applicationName);
			Document doc = Document.parse(JsonUtils.toJson(de));
			doc.put("eventType", de.getClass().getName());
			evts.add(doc);
		}
		trans.put("events", evts);
		trans.put("lastUpdated", new Date());
		
		try {
			eventTrans.insertOne(trans);
		} catch (MongoWriteException wex) {
			if (wex.getCode() != 11000) //E11000 duplicate key error
				throw wex;
			else
				// if key violation, somebody already started the same transaction. 1. concurrency problem 2.failed adding events
				throw new ConcurrencyViolationException("There is a newer version of " + entityClass.getName() + " with id " + first.getEntityId());
		}
		
		for (DomainEvent evt : events) {
			Document doc = Document.parse(JsonUtils.toJson(evt));
			doc.append("eventType", evt.getClass().getName());
			doc.append("transId", trans.get("_id"));
			doc.append("eventSent", false);
			doc.append("lastUpdated", new Date());
			try {
				entityEvents.insertOne(doc);
			} catch (MongoWriteException wex) {
				logger.error("Should not happen!!", wex);
			}
		}
		
		eventTrans.updateOne(Filters.eq("_id", trans.get("_id")),
				Updates.combine(Updates.set("state", "saved"), 
						Updates.currentDate("lastUpdated"))
		);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, boolean entityNotDeleted) {
		Assert.notNull(entityClass);
		Assert.notNull(entityId);
		List<DomainEvent> events = new ArrayList<DomainEvent>();
		Document lastestEvent = getLatestSavedEvent(entityClass, entityId);
		if (lastestEvent == null)
			return events.iterator();
		if (entityNotDeleted) {
		    Boolean isDeleted = lastestEvent.getBoolean("delete");
		    if (isDeleted != null && isDeleted)
		        return events.iterator();
		}
		
		MongoCollection<Document> entityEvents = db.getCollection("events_" + entityClass.getSimpleName());
		
		Block<Document> docToEvent = new Block<Document>(){
			public void apply(final Document doc) {
				String eventType = (String)doc.get("eventType");
				try {
					Assert.notNull(eventType, "eventType should not be null!");
					@SuppressWarnings("unchecked")
					Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(eventType);
					DomainEvent evt = JsonUtils.fromJson(doc.toJson(), clazz);
					events.add(evt);
				} catch (IOException e) {
					throw new RoseException(eventType + " deserialization error: " + doc.toJson(), e);
				} catch (ClassNotFoundException e) {
					throw new RoseException(eventType + " not found!", e);
				}
			}
		};
		// only 'saved' events
		entityEvents.find(Filters.and(Filters.eq("entityId", entityId), Filters.lte("version", lastestEvent.getInteger("version")))).sort(Sorts.ascending("version", "timestamp")).forEach(docToEvent);
		return events.iterator();
	}

	@SuppressWarnings("rawtypes")
	private Document getLatestSavedEvent(Class<? extends AbstractEntity> entityClass, Object entityId) {
		MongoCollection<Document> eventTrans = db.getCollection("event_trans_" + entityClass.getSimpleName());
		//find the latest completed event trans version
		FindIterable<Document> verIterator = eventTrans.find(Filters.and(Filters.eq("entityId", entityId), Filters.eq("state", "saved"))).sort(Sorts.descending("version")).limit(1);
		Document ver = verIterator.first();
		if (ver == null)
			return null;
		
		@SuppressWarnings("unchecked")
		List<Document> events = ver.get("events", List.class);
		return events.get(events.size()-1);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public DomainEvent loadEvent(Class<? extends AbstractEntity> entityClass, Object entityId, Integer version) {
		Document latestEvent = getLatestSavedEvent(entityClass, entityId);
		if (latestEvent == null)
			return null;
		
		MongoCollection<Document> entityEvents = db.getCollection("events_" + entityClass.getSimpleName());
		Document doc = entityEvents.find(Filters.and(Filters.eq("entityId", entityId), Filters.eq("version", version), Filters.lte("version", latestEvent.getInteger("version")))).limit(1).first();
		if (doc == null)
			return null;
		
		String eventType = (String)doc.get("eventType");
		try {
			Assert.notNull(eventType, "eventType should not be null!");
			@SuppressWarnings("unchecked")
			Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(eventType);
			DomainEvent evt = JsonUtils.fromJson(doc.toJson(), clazz);
			return evt;
		} catch (IOException e) {
			throw new RoseException(eventType + " deserialization error: " + doc.toJson(), e);
		} catch (ClassNotFoundException e) {
			throw new RoseException(eventType + " not found!", e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, Integer sinceVersion) {
		List<DomainEvent> events = new ArrayList<DomainEvent>();
		Document latestEvent = getLatestSavedEvent(entityClass, entityId);
		if (latestEvent == null)
			return events.iterator();
		
		MongoCollection<Document> entityEvents = db.getCollection("events_" + entityClass.getSimpleName());
		
		Block<Document> docToEvent = new Block<Document>(){
			public void apply(final Document doc) {
				String eventType = (String)doc.get("eventType");
				try {
					Assert.notNull(eventType, "eventType should not be null!");
					@SuppressWarnings("unchecked")
					Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(eventType);
					DomainEvent evt = JsonUtils.fromJson(doc.toJson(), clazz);
					events.add(evt);
				} catch (IOException e) {
					throw new RoseException(eventType + " deserialization error: " + doc.toJson(), e);
				} catch (ClassNotFoundException e) {
					throw new RoseException(eventType + " not found!", e);
				}
			}
		};
		entityEvents.find(Filters.and(Filters.eq("entityId", entityId), Filters.gte("version", sinceVersion), Filters.lte("version", latestEvent.getInteger("version")))).sort(Sorts.ascending("version", "timestamp")).forEach(docToEvent);
		return events.iterator();
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public int getEventSavingTimeout() {
		return eventSavingTimeout;
	}

	public void setEventSavingTimeout(int eventSavingTimeout) {
		this.eventSavingTimeout = eventSavingTimeout;
	}

	public int getEventResaveInterval() {
		return eventResaveInterval;
	}

	public void setEventResaveInterval(int eventResaveInterval) {
		this.eventResaveInterval = eventResaveInterval;
	}

	public int getEventSendingTimeout() {
		return eventSendingTimeout;
	}

	public void setEventSendingTimeout(int eventSendingTimeout) {
		this.eventSendingTimeout = eventSendingTimeout;
	}

	public long getEventResentInterval() {
		return eventResentInterval;
	}

	public void setEventResentInterval(long eventResentInterval) {
		this.eventResentInterval = eventResentInterval;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
