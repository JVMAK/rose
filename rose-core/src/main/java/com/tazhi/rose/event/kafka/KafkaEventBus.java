/**
 * 
 */
package com.tazhi.rose.event.kafka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;

import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.event.SerializedDomainEvent;
import com.tazhi.rose.exception.NoRetryDomainEventProcessingException;
import com.tazhi.rose.util.JsonUtils;

/**
 * Kafka实现的事件总线。
 * 
 * @author Evan Wu
 *
 */
public class KafkaEventBus implements EventBus, DisposableBean {
	private static final Logger logger = LoggerFactory.getLogger(KafkaEventBus.class);
	
	private Properties producerProps;
	private Properties consumerProps;
	private Producer<String, String> producer;
	private List<RoseKafkaConsumer> consumers = new ArrayList<>();
	@Value("${spring.application.name}")
	private String applicationName;
	
	public KafkaEventBus(String bootstrapServers) {
		producerProps = new Properties();
		producerProps.put("bootstrap.servers", bootstrapServers);
		producerProps.put("acks", "all");
		producerProps.put("retries", 0);
		producerProps.put("batch.size", 16384);
		producerProps.put("linger.ms", 1);
		producerProps.put("buffer.memory", 33554432);
		//producerProps.put("partitioner.class", "kafka.producer.ByteArrayPartitioner")
		producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		producer = new KafkaProducer<>(producerProps);
		
		consumerProps = new Properties();
	    consumerProps.put("bootstrap.servers", bootstrapServers);
	    consumerProps.put("enable.auto.commit", "false");
	    //consumerProps.put("auto.commit.interval.ms", "1000");
	    consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	    consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	    consumerProps.put("auto.offset.reset", "earliest");
	}
	
	public KafkaEventBus(Properties producerProps, Properties consumerProps) {
		this.producerProps = producerProps;
		this.consumerProps = consumerProps;
	}
	
	private void send(String topic, String key, String body) {
		producer.send(new ProducerRecord<>(topic, key, body), (metadata, exception) -> {
			if (exception != null) {
				logger.error("Failed to send domain event: " + exception.getMessage() + ", please fix and manually re-send event: " + body + ", topic: " + topic, exception);
			}
		});
	}
	
	@Override
	public void publish(DomainEvent... events) {
		for (DomainEvent evt : events) {
			if (applicationName != null)
				evt.setOriginator(applicationName);
			
			String topic = evt.getTopic() == null ? evt.getClass().getName() : evt.getTopic();
			logger.info("Publishing " + evt + " to topic: " + topic);
			SerializedDomainEvent sevt = SerializedDomainEvent.fromDomainEvent(evt);
			// 相同id的实体的事件会被发送到Kafka topic的同一个partition，以保证该实体的事件顺序
			send(topic, String.valueOf(evt.getEntityId()), JsonUtils.toJson(sevt));
		}
	}

	@Override
	public void subscribe(String topic, String group, Consumer<SerializedDomainEvent> handler) {
		Properties props = (Properties)consumerProps.clone();
		// set group
		props.put("group.id", group);
	    // 一个Consumer一个线程？
	    RoseKafkaConsumer consumer = new RoseKafkaConsumer(group, record -> {
	    	try {
				SerializedDomainEvent sevt = (SerializedDomainEvent) JsonUtils.fromJson(record.value(), SerializedDomainEvent.class);
				handler.accept(sevt);
			} catch (IOException e) {
				// can not be retried
				throw new NoRetryDomainEventProcessingException("DomainEvent deserialization error: " + e.getMessage(), e);
			}
	    }, topic, 3, props, producer);
	    consumer.start();
	    
	    synchronized (consumers) {
	        consumers.add(consumer);
	    }
	}

	@Override
	public void destroy() throws Exception {
		logger.info("Stopping kafka producer and consumers...");
		producer.close(1, TimeUnit.SECONDS);
		
		synchronized (consumers) {
			consumers.stream().forEach(RoseKafkaConsumer::stop);
		}
		
		logger.debug("Waiting for consumers to commit");
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			logger.error("Error waiting", e);
		}
	}
}
