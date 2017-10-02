/**
 * 
 */
package com.tazhi.rose.example.account;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.tazhi.rose.entity.IdGenerator;
import com.tazhi.rose.event.mongo.EventStoreMongodbProperties;
import com.tazhi.rose.event.mongo.SnapshotMongodbProperties;
import com.tazhi.rose.example.account.config.AccountTestConfiguration;
import com.tazhi.rose.example.account.entity.Account;
import com.tazhi.rose.example.command.DebitAccountCommand;
import com.tazhi.rose.exception.ConcurrencyViolationException;
import com.tazhi.rose.repository.EventSourcingRepository;

/**
 * 测试并发情况。
 * 
 * @author Evan Wu
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=AccountTestConfiguration.class)
public class AccountConcurrencyTest {
	@Autowired
	private EventSourcingRepository repository;
	
	@Autowired
	private MongoClient mongo;
	@Autowired
	private EventStoreMongodbProperties eventStoreMongodbProps;
	@Autowired
	private SnapshotMongodbProperties snapshotMongodbProps;
	
	private static String accountId;
	private static int threads = 5;
	private long runTime = 5000;
	private static volatile boolean done = false;
	static CountDownLatch startSignal = new CountDownLatch(1);
	static CountDownLatch exitSignal = new CountDownLatch(threads);
	static AtomicInteger failedCount = new AtomicInteger();
	static AtomicInteger successCount = new AtomicInteger();
	static AtomicInteger runsCount = new AtomicInteger();
	static AtomicInteger debitsCount = new AtomicInteger();
	
	static class TestThread extends Thread {
		private EventSourcingRepository repository;

		public TestThread(EventSourcingRepository repository) {
			this.repository = repository;
		}
		
		public void run() {
			try {
				startSignal.await();
			} catch (InterruptedException e1) {
				//
			}
			Account acct = repository.get(Account.class, accountId);
			Random rnd = new Random();
			while (!done) {
				int debits = rnd.nextInt(5);
				for (int i=0; i< debits; i++)
					acct.processCommand(new DebitAccountCommand(accountId, acct.getVersion(), IdGenerator.getInstance().generateId(), 10));
				try {
					repository.save(acct);
					successCount.incrementAndGet();
					debitsCount.addAndGet(debits);
				} catch (ConcurrencyViolationException e) {
					failedCount.incrementAndGet();
					acct = repository.get(Account.class, accountId);
				}
				runsCount.incrementAndGet();
//				try {
//					Thread.sleep(20);
//				} catch (InterruptedException e) {
//					//
//				}
			}
			exitSignal.countDown();
			System.out.println(this.getName() + " existing.");
		}
	}
	@Test
	public void testAccountTransferConcurrently() throws Exception {
		accountId = IdGenerator.getInstance().generateId();
		Account acct = new Account(accountId, 1000000000);
		repository.save(acct);
		
		System.out.println("Test account save, starting test threads...");
		for (int i=0; i<threads; i++) {
			new TestThread(repository).start();
		}
		
		startSignal.countDown();
		System.out.println("Let them run for " + runTime + " millseconds...");
		Thread.sleep(runTime);
		System.out.println("Time's up!");
		done = true;
		exitSignal.await();
		
		System.out.println();
		System.out.println("Total runs: " + runsCount.get());
		System.out.println("Success count: " + successCount.get());
		System.out.println("Failed count: " + failedCount.get());
		System.out.println("TPS: " + runsCount.get()/(runTime/1000));
		System.out.println("Success TPS: " + successCount.get()/(runTime/1000));
		System.out.println("Success debits count: " + debitsCount.get());
		System.out.println("Debits TPS: " + debitsCount.get()/(runTime/1000));
		acct = repository.get(Account.class, accountId);
		System.out.println("Inital balance: 1000000000, completed balance: " + acct.getBalance());
		Assert.assertEquals(1000000000-debitsCount.get()*10, acct.getBalance());
	}
	
	@After
	public void onTearDown() {
		MongoDatabase db = mongo.getDatabase(eventStoreMongodbProps.getDatabase());
		db.getCollection("event_trans_" + Account.class.getSimpleName()).deleteMany(Filters.eq("entityId", accountId));
		db.getCollection("events_" + Account.class.getSimpleName()).deleteMany(Filters.eq("entityId", accountId));
		db = mongo.getDatabase(snapshotMongodbProps.getDatabase());
		db.getCollection("snapshots").deleteOne(Filters.and(Filters.eq("entityType", Account.class.getName()), Filters.eq("id", accountId)));
	}
}
