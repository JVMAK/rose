/**
 * 
 */
package com.tazhi.rose.repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.util.ReflectionUtils;

import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.event.EventStore;
import com.tazhi.rose.exception.RepositoryException;

/**
 * EventSourcing实体仓库实现类。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public class EventSourcingRepositoryImpl implements EventSourcingRepository{
	private EventStore eventStore;
	private EventBus eventBus;
	private SnapshotService snapshotService;
	
	public EventSourcingRepositoryImpl(EventStore eventStore, EventBus eventBus) {
		this.eventStore = eventStore;
		this.eventBus = eventBus;
	}
	
	public void setSnapshotService(SnapshotService snapshotService) {
		this.snapshotService = snapshotService;
	}

	@Override
	public <T extends AbstractEntity> void save(T entity) {
		@SuppressWarnings("unchecked")
		List<DomainEvent> events = entity.getUncommitedEvents();
		if (events.size() > 0) {
			DomainEvent[] evts = events.toArray(new DomainEvent[]{});
			eventStore.append(entity.getClass(), evts);
			for (DomainEvent evt : events) {
				eventBus.publish(evt);
				eventStore.markEventPublished(entity.getClass(), evt);
			}
			events.clear();
			Optional.ofNullable(snapshotService).ifPresent(s -> s.updateSnapshot(entity));
		}
	}

	@Override
	public <T extends AbstractEntity> T get(Class<T> type, Object entityId) {
		T snapshotEntity = (snapshotService == null? null : snapshotService.get(type, entityId));
		if (snapshotEntity != null) {
			if (snapshotEntity.isDeleted())
				return null;
			// check if outdated
			Iterator<DomainEvent> events = eventStore.loadEvents(type, entityId, snapshotEntity.getVersion()+1);
			boolean needUpdate = false;
			while (events.hasNext()) {
				needUpdate = true;
				snapshotEntity.applyEvent(events.next());
			}
			if (snapshotEntity.isDeleted()) {
			    snapshotService.deleteSnapshot(snapshotEntity);
				return null;
			}
			
			if (needUpdate)
                snapshotService.updateSnapshot(snapshotEntity);
            
			return snapshotEntity;
		}
		
		Iterator<DomainEvent> events = eventStore.loadEvents(type, entityId, false);
		if (events == null || !events.hasNext())
			return null;
		
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			T entity = (T)constructor.newInstance();
			
			while (events.hasNext()) {
				entity.applyEvent(events.next());
			}
			if (snapshotService != null)
				snapshotService.updateSnapshot(entity);
			
			if (entity.isDeleted())
				return null;
			return entity;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
			throw new RepositoryException(type + " doesn't have a proper default constructor!", e);
		}
	}

}
