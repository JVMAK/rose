package com.tazhi.rose.event.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记某个bean用于事件处理，标记了{@link EventHandlerMethod}的方法会自动注册为DomainEvent处理器。
 * </p>
 * 结合{@link EnableEventHandlers}和{@link EventHandlerMethod}一起使用。
 * 
 * @see {@link EnableEventHandlers}
 * @see {@link EventHandlerMethod}
 * @author Evan Wu
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSubscriber {

  String id() default "";
}

