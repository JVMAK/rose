package com.tazhi.rose.event.kafka;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tazhi.rose.exception.BlockingDomainEventProcessingException;
import com.tazhi.rose.exception.DomainEventProcessingException;
import com.tazhi.rose.exception.NoRetryDomainEventProcessingException;

/**
 * 封装Kafka的Consumer，包含重试逻辑，方便注册为事件处理器。
 * 
 * @author Evan Wu
 *
 */
public class RoseKafkaConsumer {

	private final String group;
	private final Consumer<ConsumerRecord<String, String>> handler;
	private final String topic;
	private final static Logger logger = LoggerFactory.getLogger(RoseKafkaConsumer.class);
	private volatile boolean stopFlag = false;
	private Properties consumerProperties;
	private int retryCount;
	private Producer<String, String> producer;
	private KafkaConsumer<String, String> consumer;
	/**
	 * keep tracking the current offsets for partition when processing
	 */
	private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
	
	/**
	 * In case of rebalance, commit the currently processing offset to avoid duplication.
	 */
	private class RebalanceListener implements ConsumerRebalanceListener {
		@Override
		public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
			// commit the processed message offset for revoked partitions
			consumer.commitSync(currentOffsets);
		}

		@Override
		public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
			// nothing to do
		}
	}
	
	public RoseKafkaConsumer(String group,
			Consumer<ConsumerRecord<String, String>> handler, String topic, int retryCount,
			Properties consumerProperties, Producer<String, String> producer) {
		this.group = group;
		this.handler = handler;
		this.topic = topic;
		this.retryCount = retryCount;
		this.consumerProperties = consumerProperties;
		this.producer = producer;
	}

	private void verifyTopicExistsBeforeSubscribing(KafkaConsumer<String, String> consumer, String topic) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Verifying Topic {}", topic);
			List<PartitionInfo> partitions = consumer.partitionsFor(topic);
			if (logger.isDebugEnabled())
				logger.debug("Got these partitions {} for Topic {}", partitions, topic);
		} catch (Throwable e) {
			logger.error("Failed to verify topic existence: " + e.getMessage(), e);
			throw new DomainEventProcessingException("Failed to verify topic existence: " + e.getMessage(), e);
		}
	}

	public void start() {
		try {

			consumer = new KafkaConsumer<>(consumerProperties);

			verifyTopicExistsBeforeSubscribing(consumer, topic);

			if (logger.isDebugEnabled())
				logger.debug("Subscribing to {} on behalf of {}", topic, group);

			consumer.subscribe(Arrays.asList(topic), new RebalanceListener());
			// no communication until poll

			new Thread(() -> {
				try {
					while (!stopFlag) {
						ConsumerRecords<String, String> records = consumer.poll(100);
						if (!records.isEmpty() && logger.isDebugEnabled())
							logger.debug("Group {} got {} records", group, records.count());
						
						boolean blockingException = false;
						for (ConsumerRecord<String, String> record : records) {
							logger.info(String.format(
									"RoseKafkaConsumer processing record: group = %s, offset = %d, key = %s, value = %s",
									group, record.offset(), record.key(), record.value()));
							
							int tried = 0;
							while (true) {
								try {
									handler.accept(record);
									break;
								} catch (NoRetryDomainEventProcessingException e) {
									logger.error("DomainEvent processing exception: " + e.getMessage(), e);
									logger.error("DomainEvent from topic {} can not be retried due to exception: {}, moving event to dead letter topic: {}", record.topic(), e.getMessage(), record.value());
									moveToDeadLetterTopic(record);
									break;
								} catch (BlockingDomainEventProcessingException e) {
									logger.error("Blocking !!! DomainEvent processing exception: " + e.getMessage(), e);
									logger.error("!!!! Consumer for topic {} exits now! Application should be restarted after fixing, event: {}", record.topic(), record.value());
									blockingException = true;
									stopFlag = true;
									break;
								} catch (DomainEventProcessingException e) {
									logger.error("DomainEvent processing exception: " + e.getMessage(), e);
									logger.error("DomainEvent from topic {} will be re-processed due to exception: {}, event: {}", record.topic(), e.getMessage(), record.value());
								}
								
								if (++tried >= retryCount) {
									logger.error("Retry count reached {}, moving event from topic {} to dead letter topic: {}", retryCount, record.topic(), record.value());
									moveToDeadLetterTopic(record);
									break;
								}
								// sleep some time before retry
								Thread.sleep(1000 * tried);
							}
							// keep current offset for every partition
							currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset(),""));
							if (blockingException)
								break;
						}
						
						if (!records.isEmpty() && !blockingException) {
							consumer.commitAsync(); // async to improve throughput
							if (logger.isDebugEnabled())
								logger.debug("Processed {} records from topic {}, group {}", records.count(), topic, group);
						}
					}
				} catch (WakeupException e) {
					// ignored on stop
				} catch (Throwable e) {
					logger.error("RoseKafkaConsumer thread " + this + " got unexpected exception !!! Will die: " + e.getMessage(), e);
					throw new RuntimeException(e);
				} finally {
					consumer.commitSync(); // for safety
					consumer.close();
				}

			}, "Rose-KafkaConsumer-" + group + "-" + topic).start();

		} catch (Exception e) {
			logger.error("Error subscribing", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 把不能重试的事件移到dead letter队列。
	 * @param record
	 */
	private void moveToDeadLetterTopic(ConsumerRecord<String, String> record) {
		try {
			producer.send(new ProducerRecord<>(topic + ".dead", record.key(), record.value()));
		} catch (Exception e) {
			logger.error("Failed to move event to dead letter topic!!! Current group: {}, original topic: {}, event : {}", group, topic, record.value());
		}
	}

	public void stop() {
		stopFlag = true;
		if (consumer != null)
			consumer.wakeup();
	}
}
