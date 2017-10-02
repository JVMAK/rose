/**
 * 
 */
package com.tazhi.rose.repository;

import com.tazhi.rose.entity.AbstractEntity;

/**
 * 实体快照服务的接口类。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public interface SnapshotService {
	
	/**
	 * 更新快照。
	 * 
	 * @param entity
	 */
	<T extends AbstractEntity> void updateSnapshot(T entity);

	/**
	 * 获取有快照的实体。
	 * 
	 * @param entityClass
	 * @param entityId
	 * @return
	 */
	<T extends AbstractEntity> T get(Class<T> entityClass, Object entityId);

	/**
	 * 删除快照。
	 * @param snapshotEntity
	 */
	<T extends AbstractEntity> void deleteSnapshot(T snapshotEntity);

}
