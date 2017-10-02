/**
 * 
 */
package com.tazhi.rose.event.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <a href="http://kafka.apache.org/documentation.html#producerconfigs">Kafka Producer/Consumer配置</a>
 * @author Evan Wu
 *
 */
@ConfigurationProperties("kafka.eventbus")
public class EventBusKafkaProperties {
	private String bootstrapServers;
	/*
	private String acts = "all";
	private Long bufferMemory = 33554432L;
	private String compressionType = "none";
	private Integer retries = 0;
	private Integer batchSize = 16384;
	private Long lingerMs = 1L;
	private Integer sendBufferBytes = 131072;
	*/
	
	public String getBootstrapServers() {
		return bootstrapServers;
	}

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}
}
