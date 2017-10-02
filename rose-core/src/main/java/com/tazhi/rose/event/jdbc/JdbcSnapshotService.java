/**
 * 
 */
package com.tazhi.rose.event.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.repository.SnapshotService;
import com.tazhi.rose.util.JsonUtils;

/**
 * JDBC快照服务。推荐使用更高性能的快照服务。
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("rawtypes")
public class JdbcSnapshotService implements SnapshotService {

	private JdbcTemplate jdbcTemplate;

	public JdbcSnapshotService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
    public void deleteSnapshot(AbstractEntity entity) {
	    jdbcTemplate.update("DELETE FROM snapshots WHERE entity_type = ? AND entity_id = ?", 
                new Object[]{entity.getClass().getName(), entity.getId()});
	}
	
	@Override
	public void updateSnapshot(AbstractEntity entity) {
		Integer mode = jdbcTemplate.query("SELECT entity_version FROM snapshots WHERE entity_type = ? AND entity_id = ?", 
				new Object[]{entity.getClass().getName(), entity.getId()}, 
				new ResultSetExtractor<Integer>(){
					@Override
					public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
						if (rs.next()) {
							Integer ver = rs.getInt(1);
							if (ver.compareTo(entity.getVersion())<0)
								return 2;
							return 3;
						}
						return 1;
					}
		});
		if (mode.equals(1))
			jdbcTemplate.update("INSERT INTO snapshots(entity_type, entity_id, entity_version, snapshot_json) VALUES(?,?,?,?)", 
					entity.getClass().getName(), entity.getId(), entity.getVersion(), JsonUtils.toJson(entity));
		if (mode.equals(2))
			jdbcTemplate.update("UPDATE snapshots SET entity_version = ?, snapshot_json = ? WHERE entity_type =? AND entity_id = ?", 
					entity.getVersion(), JsonUtils.toJson(entity), entity.getClass().getName(), entity.getId());
		// 3 ignore
	}

	@Override
	public <T extends AbstractEntity> T get(Class<T> entityClass, Object entityId) {
		return jdbcTemplate.query("SELECT entity_type, entity_id, entity_version, snapshot_json FROM snapshots WHERE entity_type = ? AND entity_id = ? ORDER BY entity_version DESC LIMIT 1", 
				new Object[]{entityClass.getName(), entityId}, 
				new ResultSetExtractor<T>(){
					@Override
					public T extractData(ResultSet rs) throws SQLException, DataAccessException {
						if (rs.next()) {
							try {
								String json = rs.getString(4);
								return (T) JsonUtils.fromJson(json, entityClass);
							} catch (IOException e) {
								throw new RoseException(e.getMessage(), e);
							}
						}
						return null;
					}
		});
	}
}
