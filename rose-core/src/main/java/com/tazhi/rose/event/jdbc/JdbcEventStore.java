/**
 * 
 */
package com.tazhi.rose.event.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.event.DeleteEvent;
import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.event.EventStore;
import com.tazhi.rose.exception.ConcurrencyViolationException;
import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.util.JsonUtils;

/**
 * JDBC事件仓库。推荐使用Mongodb的事件仓库。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public class JdbcEventStore implements EventStore {
	
	private JdbcTemplate jdbcTemplate;
	
	public JdbcEventStore(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	public void append(Class<? extends AbstractEntity> entityClass, DomainEvent... event)
			throws ConcurrencyViolationException {
		for (DomainEvent e : event) {
			jdbcTemplate.update("INSERT INTO events(entity_type, entity_id, entity_version, event_type, event_json) VALUES(?,?,?,?,?)", 
					entityClass.getName(), e.getEntityId(), e.getVersion(), e.getClass().getName(), JsonUtils.toJson(e));
		}
	}

	@Override
	public Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, boolean entityNotDeleted) {
		List<DomainEvent> events = jdbcTemplate.query("SELECT entity_type, entity_id, entity_version, event_type, event_json FROM events WHERE "
				+ " entity_type = ? AND entity_id = ? ORDER BY entity_version", new Object[]{entityClass.getName(), entityId}, new ResultSetExtractor<List<DomainEvent>>(){

			@Override
			public List<DomainEvent> extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<DomainEvent> evts = new LinkedList<DomainEvent>();
				while (rs.next()) {
					try {
						@SuppressWarnings("unchecked")
						Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(rs.getString(4));
						if (DeleteEvent.class.isAssignableFrom(clazz))
						    return Collections.emptyList();
						
						DomainEvent evt = JsonUtils.fromJson(rs.getString(5), clazz);
						evts.add(evt);
					} catch (ClassNotFoundException e) {
						throw new RoseException(rs.getString(4) + " not found!", e);
					} catch (SecurityException e) {
						throw new RoseException(rs.getString(4) + " doesn't have a proper default constructor!", e);
					} catch (IOException e) {
						throw new RoseException(rs.getString(4) + " deserialization error!", e);
					}
					
				}
				return evts;
			}
		});
		return events.iterator();
	}

	@Override
	public DomainEvent loadEvent(Class<? extends AbstractEntity> entityClass, Object entityId, Integer version) {
		DomainEvent event = jdbcTemplate.query("SELECT entity_type, entity_id, entity_version, event_type, event_json FROM events WHERE "
				+ " entity_type = ? AND entity_id = ? AND entity_version = ? limit 1", new Object[]{entityClass.getName(), entityId, version}, new ResultSetExtractor<DomainEvent>(){

			@Override
			public DomainEvent extractData(ResultSet rs) throws SQLException, DataAccessException {
				if (rs.next()) {
					try {
						@SuppressWarnings("unchecked")
						Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(rs.getString(4));
						DomainEvent evt = JsonUtils.fromJson(rs.getString(5), clazz);
						return evt;
					} catch (ClassNotFoundException e) {
						throw new RoseException(rs.getString(4) + " not found!", e);
					} catch (SecurityException e) {
						throw new RoseException(rs.getString(4) + " doesn't have a proper default constructor!", e);
					} catch (IOException e) {
						throw new RoseException(rs.getString(4) + " deserialization error!", e);
					}
				}
				return null;
			}
		});
		return event;
	}

	@Override
	public Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, Integer sinceVersion) {
		List<DomainEvent> events = jdbcTemplate.query("SELECT entity_type, entity_id, entity_version, event_type, event_json FROM events WHERE "
				+ " entity_type = ? AND entity_id = ? AND version >= ? ORDER BY entity_version", new Object[]{entityClass.getName(), entityId, sinceVersion}, new ResultSetExtractor<List<DomainEvent>>(){

			@Override
			public List<DomainEvent> extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<DomainEvent> evts = new LinkedList<DomainEvent>();
				while (rs.next()) {
					try {
						@SuppressWarnings("unchecked")
						Class<? extends DomainEvent> clazz = (Class<? extends DomainEvent>) Class.forName(rs.getString(4));
						DomainEvent evt = JsonUtils.fromJson(rs.getString(5), clazz);
						evts.add(evt);
					} catch (ClassNotFoundException e) {
						throw new RoseException(rs.getString(4) + " not found!", e);
					} catch (SecurityException e) {
						throw new RoseException(rs.getString(4) + " doesn't have a proper default constructor!", e);
					} catch (IOException e) {
						throw new RoseException(rs.getString(4) + " deserialization error!", e);
					}
					
				}
				return evts;
			}
		});
		return events.iterator();
	}

	@Override
	public void markEventPublished(Class<? extends AbstractEntity> entityClass, DomainEvent... evts) {
		// TODO fix it
	}
}
