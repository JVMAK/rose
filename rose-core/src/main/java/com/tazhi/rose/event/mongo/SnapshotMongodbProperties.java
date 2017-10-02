/**
 * 
 */
package com.tazhi.rose.event.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mongodb快照配置。
 * 
 * @author Evan Wu
 *
 */
@ConfigurationProperties("mongodb.snapshot")
public class SnapshotMongodbProperties {
	private String database;
	private String host;
	private int port = 27017;
	private String username;
	private String password;
	
	public String getDatabase() {
		return database;
	}
	public void setDatabase(String database) {
		this.database = database;
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
