/**
 * 
 */
package com.tazhi.rose.example.account;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.tazhi.rose.event.mongo.EventStoreMongodbProperties;
import com.tazhi.rose.event.mongo.SnapshotMongodbProperties;
import com.tazhi.rose.example.account.config.AccountTestConfiguration;
import com.tazhi.rose.example.account.entity.Account;
import com.tazhi.rose.example.command.CreditAccountCommand;
import com.tazhi.rose.example.command.DebitAccountCommand;
import com.tazhi.rose.example.command.DeleteAccountCommand;
import com.tazhi.rose.repository.EventSourcingRepository;

/**
 * 一个测试用例。创建两个账户a1, a2，处理命令。
 * 
 * @author Evan Wu
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=AccountTestConfiguration.class)
public class AccountTest {
	@Autowired
	private EventSourcingRepository repository;
	
	@Autowired
	private MongoClient mongo;
	@Autowired
	private EventStoreMongodbProperties eventStoreMongodbProps;
	@Autowired
	private SnapshotMongodbProperties snapshotMongodbProps;
	
	@Test
	public void testAccount() throws Exception {
		Account a1 = new Account("a1", 100);
		Account a2 = new Account("a2", 200);
		Account a3 = new Account("a3", 200);
		repository.save(a1);
		repository.save(a2);
		repository.save(a3);
		
		a1.processCommand(new CreditAccountCommand("a1", 1, "trans1", 20));
		repository.save(a1);
		Assert.assertEquals(120, a1.getBalance());
		Assert.assertEquals(2, a1.getVersion().intValue());
		Assert.assertEquals(0, a1.getUncommitedEvents().size());
		
		a3.processCommand(new DeleteAccountCommand("a3", 1));
		repository.save(a3);
		
		a3 = repository.get(Account.class, "a3");
		Assert.assertNull(a3);
		
		a2.processCommand(new DebitAccountCommand("a2", 1, "trans3", 20));
		repository.save(a2);
		Assert.assertEquals(180, a2.getBalance());
		
		// load
		a2 = repository.get(Account.class, "a2");
		Assert.assertNotNull(a2);
		Assert.assertEquals(180, a2.getBalance());
		
		// wait some time for event publish/processing...
		Thread.sleep(2000);
	}
	
	@After
	public void onTearDown() {
		MongoDatabase db = mongo.getDatabase(eventStoreMongodbProps.getDatabase());
		db.getCollection("event_trans_" + Account.class.getSimpleName()).deleteMany(Filters.or(Filters.eq("entityId", "a1"), Filters.eq("entityId", "a2"),Filters.eq("entityId", "a3")));
		db.getCollection("events_" + Account.class.getSimpleName()).deleteMany(Filters.or(Filters.eq("entityId", "a1"), Filters.eq("entityId", "a2"),Filters.eq("entityId", "a3")));
		db = mongo.getDatabase(snapshotMongodbProps.getDatabase());
		db.getCollection("snapshots").deleteOne(Filters.and(Filters.eq("entityType", Account.class.getName()), Filters.eq("id", "a1")));
		db.getCollection("snapshots").deleteOne(Filters.and(Filters.eq("entityType", Account.class.getName()), Filters.eq("id", "a2")));
		db.getCollection("snapshots").deleteOne(Filters.and(Filters.eq("entityType", Account.class.getName()), Filters.eq("id", "a3")));
	}
}
