package com.tazhi.rose.example.transfer.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.tazhi.rose.event.EventBus;
import com.tazhi.rose.event.EventStore;
import com.tazhi.rose.event.kafka.EventBusKafkaProperties;
import com.tazhi.rose.event.kafka.KafkaEventBus;
import com.tazhi.rose.event.mongo.EventStoreMongodbProperties;
import com.tazhi.rose.event.mongo.MongoEventStore;
import com.tazhi.rose.event.mongo.MongoSnapshotService;
import com.tazhi.rose.event.mongo.SnapshotMongodbProperties;
import com.tazhi.rose.event.spring.EnableEventHandlers;
import com.tazhi.rose.example.transfer.eventhandler.MoneyTransferProcessManager;
import com.tazhi.rose.repository.EventSourcingRepository;
import com.tazhi.rose.repository.EventSourcingRepositoryImpl;
import com.tazhi.rose.repository.SnapshotService;

@Configuration
@EnableEventHandlers
@EnableConfigurationProperties({EventStoreMongodbProperties.class, SnapshotMongodbProperties.class, EventBusKafkaProperties.class})
public class MoneyTransferTestConfiguration {
	@Autowired
	private EventStoreMongodbProperties eventStoreMongodbProps;
	@Autowired
	private SnapshotMongodbProperties snapshotMongodbProps;
	@Autowired
	private EventBusKafkaProperties kafkaEventBusProps;
	/*
	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		return builder.setType(EmbeddedDatabaseType.HSQL).addScript("embeded-h2-event-store.sql").build();
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}
	
	@Bean
	public JdbcEventStore jdbcEventStore(JdbcTemplate jdbcTemplate) {
		return new JdbcEventStore(jdbcTemplate);
	}
	
	@Bean 
	public SnapshotService<MoneyTransfer> snapshotService(JdbcTemplate jdbcTemplate) {
		return new JdbcSnapshotService<MoneyTransfer>(jdbcTemplate);
	}
	*/
	
	@Bean
	public MongoClient mongo() {
		MongoClient mongo = null;
		if (eventStoreMongodbProps.getUsername() != null) {
			MongoCredential credential = MongoCredential.createCredential(eventStoreMongodbProps.getUsername(), eventStoreMongodbProps.getDatabase(), eventStoreMongodbProps.getPassword().toCharArray());
			mongo = new MongoClient(new ServerAddress(eventStoreMongodbProps.getHost(), eventStoreMongodbProps.getPort()), Arrays.asList(credential));
		} else {
			mongo = new MongoClient(new ServerAddress(eventStoreMongodbProps.getHost(), eventStoreMongodbProps.getPort()));
		}
		return mongo;
	}
	
	@Bean
	public EventStore mongoEventStore(MongoClient mongo, EventBus eventBus) {
		MongoEventStore eventStore = new MongoEventStore(mongo, eventStoreMongodbProps.getDatabase(), eventBus);
		return eventStore;
	}
	
	@Bean 
	public SnapshotService mongoSnapshotService(MongoClient mongo) {
		MongoSnapshotService service = new MongoSnapshotService(mongo, snapshotMongodbProps.getDatabase());
		return service;
	}
	
	@Bean
	public KafkaEventBus kafkaEventBus() {
		KafkaEventBus eventBus = new KafkaEventBus(kafkaEventBusProps.getBootstrapServers());
		return eventBus;
	}
	
	@Bean
	public EventSourcingRepository eventSourcedEntityRepository(EventStore eventStore, EventBus eventBus, SnapshotService snapshotService) {
		EventSourcingRepositoryImpl repo = new EventSourcingRepositoryImpl(eventStore, eventBus);
		repo.setSnapshotService(snapshotService);
		return repo;
	}
	
	@Bean
	public MoneyTransferProcessManager moneyTransferProcessManager() {
		return new MoneyTransferProcessManager();
	}
}
