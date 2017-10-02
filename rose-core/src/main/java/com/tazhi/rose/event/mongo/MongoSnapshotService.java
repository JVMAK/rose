/**
 * 
 */
package com.tazhi.rose.event.mongo;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.repository.SnapshotService;
import com.tazhi.rose.util.JsonUtils;

/**
 * Mongodb实现的快照服务。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public class MongoSnapshotService implements SnapshotService {
	private MongoClient mongo;
	private MongoDatabase db;
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;
	
	public MongoSnapshotService() {}
	
	public MongoSnapshotService(MongoClient mongo, String database) {
		this.mongo = mongo;
		this.db = mongo.getDatabase(database);
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
		/**
		 * snapshots: {"_id": xxx "entityType": "xxxx", "id": "xxxx", "version": 1, ... }
		 * unique index: entityType, id
		 */
		MongoCollection<Document> snapshots = db.getCollection("snapshots");
		snapshots.createIndex(Indexes.ascending("entityType", "id"), new IndexOptions().unique(true));
	}
	
	@PreDestroy
	public void destroy() {
		if (mongo != null)
			mongo.close();
	}
	
	@Override
	public <T extends AbstractEntity> void updateSnapshot(T entity) {
		MongoCollection<Document> snapshots = db.getCollection("snapshots");
		// be careful with the 'id' unique index, deleted entity's id can not be re-used.
		Document doc = Document.parse(JsonUtils.toJson(entity));
		doc.append("entityType", entity.getClass().getName());
		/*
		Document update = new Document();
		update.append("$set", doc);
		snapshots.updateOne(Filters.and(Filters.eq("entityType", entity.getClass().getName()), Filters.eq("id", entity.getId())), 
				update, 
				new UpdateOptions().upsert(true));
		*/
		
		// update/insert only if no newer one exists
		Document ret = snapshots.findOneAndReplace(Filters.and(Filters.eq("entityType", entity.getClass().getName()), Filters.eq("id", entity.getId()), Filters.lt("version", entity.getVersion())), doc);
		if (ret == null) { // null: 1. the same/newer version exist, 2. doesn't exist
			try {
				snapshots.insertOne(doc);
			} catch (MongoWriteException wex) { 
				if (wex.getCode() != 11000) // newer one exists, duplicate key error
					throw wex;
			}
		}
	}

	@Override
    public <T extends AbstractEntity> void deleteSnapshot(T entity) {
	    MongoCollection<Document> snapshots = db.getCollection("snapshots");
	    snapshots.deleteOne(Filters.and(Filters.eq("entityType", entity.getClass().getName()), Filters.eq("id", entity.getId())));
    }
	
	@Override
	public <T extends AbstractEntity> T get(Class<T> entityClass, Object entityId) {
		MongoCollection<Document> snapshots = db.getCollection("snapshots");
		Document doc = snapshots.find(Filters.and(Filters.eq("entityType", entityClass.getName()), Filters.eq("id", entityId))).sort(Sorts.descending("version")).limit(1).first();
		if (doc == null)
			return null;
		
		try {
			T entity = JsonUtils.fromJson(doc.toJson(), entityClass);
			return entity;
		} catch (IOException e) {
			throw new RoseException(entityClass + " deserialization error: " + doc.toJson(), e);
		}
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
}
