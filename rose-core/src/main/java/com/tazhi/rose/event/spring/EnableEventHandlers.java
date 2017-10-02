package com.tazhi.rose.event.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Application应用配置，启用事件处理的spring bean，自动注册DomainEvent事件处理器。
 * 
 * @see {@link EventSubscriber}
 * @see {@link EventHandlerMethod}
 * 
 * @author Evan Wu
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EventSubscriberAutoConfiguration.class)
public @interface EnableEventHandlers {
}
