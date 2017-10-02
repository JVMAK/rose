/**
 * 
 */
package com.tazhi.rose.event;

import java.util.function.Consumer;

/**
 * R.O.S.E框架的事件总线。用于发布事件和注册事件处理器。
 * 
 * @author Evan Wu
 *
 */
public interface EventBus {

	/**
	 * 发出领域事件。
	 * 
	 * @param events 领域事件
	 */
	void publish(DomainEvent... events);

	/**
	 * 注册监听某个topic的领域事件。
	 * 
	 * @param topic 事件的topic
	 * @param group 事件处理器的组名 (如Kafka的consumer group。同一个group的事件处理器将会做负载均衡)
	 * @param handler 事件处理器
	 */
	void subscribe(String topic, String group, Consumer<SerializedDomainEvent> handler);
}
