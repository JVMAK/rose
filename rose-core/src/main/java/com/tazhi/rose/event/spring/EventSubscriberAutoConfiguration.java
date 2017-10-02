package com.tazhi.rose.event.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tazhi.rose.event.EventBus;


/**
 * Application应用配置，自动发现与配置事件处理器。
 * 
 * @author Evan Wu
 *
 */
@Configuration
public class EventSubscriberAutoConfiguration {

  @Bean
  public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventSubscriberInitializer eventSubscriberInitializer) {
    return new EventHandlerBeanPostProcessor(eventSubscriberInitializer);
  }

  @Bean
  public EventSubscriberInitializer eventSubscriberInitializer(@Autowired EventBus eventBus) {
    return new EventSubscriberInitializer(eventBus);
  }

}
