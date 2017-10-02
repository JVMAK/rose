/**
 * 
 */
package com.tazhi.rose.repository;

import com.tazhi.rose.entity.AbstractEntity;

/**
 * 按EventSourcing方式保存与获取实体的仓库类。只提供根据id获取实体的方法，其他查询通过CQRS的Q端进行。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public interface EventSourcingRepository {
	/**
	 * 保存并实体并在事件总线上发出实体变更的事件。
	 * 
	 * @param entity
	 */
	<T extends AbstractEntity> void save(T entity);
	
	/**
	 * 按id获取实体。
	 * 
	 * @param type 
	 * @param entityId
	 * @return
	 */
	<T extends AbstractEntity> T get(Class<T> type, Object entityId);
	
	//All other queries go to the query-side database
}
