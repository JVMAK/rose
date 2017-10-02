/**
 * 
 */
package com.tazhi.rose.example.account.eventhandler;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.tazhi.rose.event.SerializedDomainEvent;
import com.tazhi.rose.event.spring.EventHandlerMethod;
import com.tazhi.rose.event.spring.EventSubscriber;
import com.tazhi.rose.example.account.entity.Account;
import com.tazhi.rose.example.command.CreditAccountCommand;
import com.tazhi.rose.example.command.DebitAccountCommand;
import com.tazhi.rose.exception.NoRetryDomainEventProcessingException;
import com.tazhi.rose.repository.EventSourcingRepository;
import com.tazhi.rose.util.JsonUtils;

/**
 * 监听事件，用于处理 rose-example-money-transfer 发出的领域事件。实现转账过程。
 * 
 * @author Evan Wu
 *
 */
@Component
@EventSubscriber
public class MoneyTransferEventHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(MoneyTransferEventHandler.class);
	
	@Autowired
	EventSourcingRepository repository;
	
	@EventHandlerMethod(topic = "com.tazhi.rose.example.transfer.event.MoneyTransferCreatedEvent", group = "test-group-account")
	public void processMoneyTransferCreatedEvent(SerializedDomainEvent evt) {
		try {
			logger.info("****** MoneyTransferCreatedEvent: " + evt.getEventBody());
			//把event body转成map先
			Map<String, String> evtData = JsonUtils.toMap(evt.getEventBody(), String.class, String.class);
			String fromAccountId = (String)evtData.get("fromAccountId");
			String toAccountId = (String)evtData.get("toAccountId");
			String transactionId = (String)evtData.get("transactionId");
			int amount = Integer.valueOf((String)evtData.get("amount"));
			Account fromAccount = repository.get(Account.class, fromAccountId);
			Account toAccount = repository.get(Account.class, toAccountId);
			
			Assert.notNull(fromAccount);
			Assert.notNull(toAccount);
			
			fromAccount.processCommand(new DebitAccountCommand(fromAccountId, null, transactionId, amount));
			
			repository.save(fromAccount);
		} catch (IOException e) {
			throw new NoRetryDomainEventProcessingException("Failed to process MoneyTrasferCreatedEvent: " + e.getMessage(), e);
		}
	}
	
	@EventHandlerMethod(topic = "com.tazhi.rose.example.transfer.event.MoneyTransferDebitRecordedEvent", group = "test-group-account")
	public void processMoneyTransferDebitRecordedEvent(SerializedDomainEvent evt) {
		try {
			logger.info("****** MoneyTransferDebitRecordedEvent: " + evt.getEventBody());
			//把event body转成map先
			Map<String, String> evtData = JsonUtils.toMap(evt.getEventBody(), String.class, String.class);
			String fromAccountId = (String)evtData.get("fromAccountId");
			String toAccountId = (String)evtData.get("toAccountId");
			String transactionId = (String)evtData.get("transactionId");
			int amount = Integer.valueOf((String)evtData.get("amount"));
			Account fromAccount = repository.get(Account.class, fromAccountId);
			Account toAccount = repository.get(Account.class, toAccountId);
			Assert.notNull(fromAccount);
			Assert.notNull(toAccount);
			
			toAccount.processCommand(new CreditAccountCommand(toAccountId, null, transactionId, amount));
			
			repository.save(toAccount);
		} catch (IOException e) {
			throw new NoRetryDomainEventProcessingException("Failed to process MoneyTransferDebitRecordedEvent: " + e.getMessage(), e);
		}
		
	}
}
