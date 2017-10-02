/**
 * 
 */
package com.tazhi.rose.event;

import java.util.Iterator;

import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.exception.ConcurrencyViolationException;

/**
 * 事件的保存机制。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public interface EventStore {

	/**
	 * 保存实体事件。
	 * @param entityClass 
	 * 
	 * @param event
	 */
	void append(Class<? extends AbstractEntity> entityClass, DomainEvent... event) throws ConcurrencyViolationException;

	/**
	 * 加载全部实体事件。
	 * 
	 * @param entityClass
	 * @param entityId 
	 * @param entityNotDeleted true如果实体被删除则返回空的Iterator，false则返回全部
	 * @return
	 */
	Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, boolean entityNotDeleted);

	/**
	 * 加载某个实体版本对应的事件。
	 * 
	 * @param entityClass
	 * @param entityId
	 * @param version
	 * @return
	 */
	DomainEvent loadEvent(Class<? extends AbstractEntity> entityClass, Object entityId, Integer version);

	/**
	 * 加载某个实体版本之后的事件。
	 * 
	 * @param entityClass
	 * @param entityId
	 * @param sinceVersion
	 * @return
	 */
	Iterator<DomainEvent> loadEvents(Class<? extends AbstractEntity> entityClass, Object entityId, Integer sinceVersion);

	/**
	 * 标记事件已经被发出。
	 * @param evts
	 */
	void markEventPublished(Class<? extends AbstractEntity> entityClass, DomainEvent... evts);
}
