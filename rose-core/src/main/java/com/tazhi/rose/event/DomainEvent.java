/**
 * 
 */
package com.tazhi.rose.event;

import java.util.Date;

/**
 * 领域事件的父类。
 * </p>
 * <b>注意：</b></br>
 * 全部事件需要一个默认(无参数)的构造函数，用于反序列时创建实例，建议是protected的。
 * 子类需要指定：</br>
 * entityId: 关联实体的id </br>
 * sourceVersion: 事件的代码版本号，用于迭代新版本时兼容以往版本的历史事件。
 * </p>
 * 其中，version字段是实体的版本号，会被框架自动设置为当前实体的版本。
 * </p>
 * 
 * @author Evan Wu
 *
 */
public abstract class DomainEvent {
	/**
	 * 关联实体的id，相同id的实体的事件能保证其顺序（如发送到Kafka topic的同一个partition）
	 */
	protected Object entityId;
	/**
	 * 版本号，与实体的version保持一致
	 */
	protected Integer version = 1;
	/**
	 * 事件发生时间
	 */
	protected Date timestamp = new Date();
	/**
	 * 发出事件的系统名称，由框架自动设置为spring.application.name
	 */
	private String originator;
	
	/**
	 * 事件的代码版本号。如：1.0, 1.1, 2.0 ...
	 * 注意：当事件的结构发生变化时，返回新的版本号。用于区别处理不同结构的历史事件。
	 */
	public abstract String getSourceVersion();
	
	/**
	 * 产生事件的实体ID。
	 * @return
	 */
	public Object getEntityId() {
		return entityId;
	}

	/**
	 * 实体的版本。
	 * @return
	 */
	public Integer getVersion() {
		return version;
	}
	
	/**
	 * 事件发生时间。
	 * @return
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * 设置实体版本。
	 * @param version
	 */
	protected void setVersion(Integer version) {
		this.version = version;
	}
	
	/**
	 * 设置事件发生时间。
	 * @param timestamp
	 */
	protected void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * 设置发出事件的应用名称，一般为{@code spring.application.name}。
	 * @param originator
	 */
	public void setOriginator(String originator) {
		this.originator = originator;
	}
	
	/**
	 * 发出事件的应用名称，一般为{@code spring.application.name}。
	 * @return
	 */
	public String getOriginator() {
		return originator;
	}
	
	/**
	 * 事件指定的topic，如果不指定(null)则使用事件的类名作为topic。
	 * @return
	 */
	public String getTopic() {
		return null;
	}
}
