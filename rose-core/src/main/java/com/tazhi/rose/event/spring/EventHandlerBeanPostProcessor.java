package com.tazhi.rose.event.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * 扫描spring application context，把加了{@link @EventHandlerMethod}注解的bean方法注册为事件处理器。
 * 
 * @author Evan Wu
 *
 */
public class EventHandlerBeanPostProcessor implements BeanPostProcessor {
	
	private EventSubscriberInitializer eventDispatcherInitializer;

	public EventHandlerBeanPostProcessor(EventSubscriberInitializer eventDispatcherInitializer) {
		this.eventDispatcherInitializer = eventDispatcherInitializer;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		EventSubscriber a = AnnotationUtils.findAnnotation(bean.getClass(), EventSubscriber.class);
		if (a != null)
			eventDispatcherInitializer.registerEventHandler(bean, beanName);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
