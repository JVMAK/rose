/**
 * 
 */
package com.tazhi.rose.event;

import java.io.IOException;

import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.util.JsonUtils;

/**
 * 序列化之后的DomainEvent。原事件被序列化成JSON字符串，作为eventBody字段。
 * 
 * @author Evan Wu
 *
 */
public class SerializedDomainEvent {
	/**
	 * 事件的类名
	 */
	private String eventName;
	/**
	 * 事件的迭代版本
	 */
	private String sourceVersion;
	/**
	 * 序列化成JSON的事件内容
	 */
	private String eventBody;
	
	public SerializedDomainEvent() {}
	
	/**
	 * 
	 * @param eventName 事件的类名
	 * @param sourceVersion 事件的迭代版本
	 * @param eventBody 序列化成JSON的事件内容
	 */
	public SerializedDomainEvent(String eventName, String sourceVersion, String eventBody) {
	    this.eventName = eventName;
	    this.sourceVersion = sourceVersion;
	    this.eventBody = eventBody;
	}
	
	/**
	 * 从{@link DomainEvent}生成序列化的事件。
	 * 
	 * @param domainEvent
	 * @return
	 */
	public static SerializedDomainEvent fromDomainEvent(DomainEvent domainEvent) {
		SerializedDomainEvent sevt = new SerializedDomainEvent(domainEvent.getClass().getName(), 
		        domainEvent.getSourceVersion(), JsonUtils.toJson(domainEvent));
		//TODO test source version, apply event conversion?
		return sevt;
	}
	
	/**
	 * 转换成{@link DomainEvent}。
	 * @return
	 */
	public DomainEvent toDomainEvent() {
		try {
			Class<?> clazz = Class.forName(eventName);
			return (DomainEvent)JsonUtils.fromJson(eventBody, clazz);
		} catch (ClassNotFoundException e) {
			throw new RoseException(eventName + " class not found", e);
		} catch (IOException e) {
			throw new RoseException("Can not deserialize " + eventName + " from " + eventBody, e);
		}
	}

	/**
	 * 事件的类名。
	 * @return
	 */
	public String getTypeName() {
		return eventName;
	}

	/**
	 * 序列化成JSON的事件内容。
	 * @return
	 */
	public String getEventBody() {
		return eventBody;
	}

	/**
	 * 事件的迭代版本。
	 * @return
	 */
	public String getSourceVersion() {
		return sourceVersion;
	}
}
