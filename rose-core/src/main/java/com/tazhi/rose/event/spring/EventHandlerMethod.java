package com.tazhi.rose.event.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为事件处理方法。
 * 
 * @see {@link EnableEventHandlers}
 * @see {@link EventSubscriber}
 * 
 * @author Evan Wu
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandlerMethod {
	/**
	 * DomainEvent对应的topic。
	 * @return
	 */
	String topic();
	/**
	 * 事件处理器的分组。不同分组可同时收到同一个事件，但同一分组的只有一个事件处理器能收到。如果不指定，则会被设为当前的 {@code applicationName}。
	 * @return
	 */
	String group() default "";
}
