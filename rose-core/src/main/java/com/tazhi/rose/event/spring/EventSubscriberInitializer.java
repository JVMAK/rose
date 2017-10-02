package com.tazhi.rose.event.spring;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.exception.DomainEventProcessingException;
import com.tazhi.rose.exception.NoRetryDomainEventProcessingException;
import com.tazhi.rose.exception.RoseException;
import com.tazhi.rose.util.JsonUtils;
import com.tazhi.rose.util.ReflectionUtils;

/**
 * 具体实现把事件处理器注册到事件总线{@link EventBus}的类。
 * 
 * @author Evan Wu
 *
 */
public class EventSubscriberInitializer {
	
	private static final Logger logger = LoggerFactory.getLogger(EventSubscriberInitializer.class);
	
	private Set<String> subscriberIds = new HashSet<>();
	private EventBus eventBus;
	@Value("${spring.application.name}")
	private String applicationName;
	
	public EventSubscriberInitializer(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public void registerEventHandler(Object eventHandlerBean, String beanName) {

		List<AccessibleObject> fieldsAndMethods = Stream.<AccessibleObject>concat(
				Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(eventHandlerBean.getClass())),
				Arrays.stream(eventHandlerBean.getClass().getDeclaredFields())).collect(Collectors.toList());

		List<AccessibleObject> annotatedCandidateEventHandlers = fieldsAndMethods.stream().filter(
				fieldOrMethod -> AnnotationUtils.findAnnotation(fieldOrMethod, EventHandlerMethod.class) != null)
				.collect(Collectors.toList());

		//Needed?
		EventSubscriber a = AnnotationUtils.findAnnotation(eventHandlerBean.getClass(), EventSubscriber.class);
		if (a == null)
			throw new RoseException("Needs @EventSubscriber annotation: " + eventHandlerBean);

		String subscriberId = StringUtils.isEmpty(a.id()) ? beanName : a.id();

		if (subscriberIds.contains(subscriberId))
			throw new RoseException("Duplicate subscriptionId " + subscriberId);
		subscriberIds.add(subscriberId);
		
		annotatedCandidateEventHandlers.forEach(m -> {
			logger.info("Registering event handler method: " + m);
			Method method = (Method)m;
			EventHandlerMethod anno = m.getAnnotation(EventHandlerMethod.class);
			String topic = anno.topic();
			String group = StringUtils.isEmpty(anno.group())? applicationName : anno.group();
			
			eventBus.subscribe(topic, group, event -> {
				try {
					logger.info(method + " processing domain event, topic: " + topic + ", group: " + group + ", eventType: " + event.getTypeName());
					//topic is specialized to Event type
					method.invoke(eventHandlerBean, event);
				} catch (IllegalAccessException | IllegalArgumentException e) {
					//can not retry
					throw new NoRetryDomainEventProcessingException("Event handler method failed to process event, method: " + method + ", event: " + JsonUtils.toJson(event) + ". " + e.getMessage(), e);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof DomainEventProcessingException) {
						throw (DomainEventProcessingException)e.getCause();
					} else {
						throw new NoRetryDomainEventProcessingException("Event handler method failed to process event, method: " + method + ", event: " + JsonUtils.toJson(event) + ". " + e.getMessage(), e);
					}
				} 
			});
		});
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	
	
}