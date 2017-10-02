/**
 * 
 */
package com.tazhi.rose.event.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.event.SerializedDomainEvent;

/**
 * 用于本地测试的事件总线。
 * 
 * @author Evan Wu
 *
 */
public class LocalEventBus implements EventBus {
	private ConcurrentMap<String, List<DomainEvent>> allEvents = new ConcurrentHashMap<String, List<DomainEvent>>();
	private Map<String, Consumer<SerializedDomainEvent>> listeners = new HashMap<>();
	
	@Override
	public void publish(DomainEvent... events) {
		for (DomainEvent evt : events) {
			String topic = evt.getTopic() == null ? evt.getClass().getName() : evt.getTopic();
			allEvents.putIfAbsent(topic, new ArrayList<DomainEvent>());
			allEvents.get(topic).add(evt);
			Optional.ofNullable(listeners.get(topic)).ifPresent(c -> {c.accept(SerializedDomainEvent.fromDomainEvent(evt));});
		}
	}

	@Override
	public void subscribe(String topic, String group, Consumer<SerializedDomainEvent> handler) {
		listeners.put(topic, handler);
	}
	
	public Map<String, List<DomainEvent>> getAllEvents() {
		return allEvents;
	}
}
