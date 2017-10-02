/**
 * 
 */
package com.tazhi.rose.example.transfer.eventhandler;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tazhi.rose.event.EventStore;
import com.tazhi.rose.event.SerializedDomainEvent;
import com.tazhi.rose.event.spring.EventHandlerMethod;
import com.tazhi.rose.event.spring.EventSubscriber;
import com.tazhi.rose.example.transfer.command.AccountCreditedCommand;
import com.tazhi.rose.example.transfer.command.AccountDebitedCommand;
import com.tazhi.rose.example.transfer.entity.MoneyTransfer;
import com.tazhi.rose.example.transfer.event.MoneyTransferCreatedEvent;
import com.tazhi.rose.exception.DomainEventProcessingException;
import com.tazhi.rose.exception.NoRetryDomainEventProcessingException;
import com.tazhi.rose.repository.EventSourcingRepository;
import com.tazhi.rose.util.JsonUtils;

/**
 * 转账相关事件处理。也作为一个"Process Manager"。
 * 
 * @see @see <a href="http://stackoverflow.com/questions/15528015/what-is-the-difference-between-a-saga-a-process-manager-and-a-document-based-ap">Saga, Process Manager</a>
 * @author Evan Wu
 *
 */
@Component
@EventSubscriber
public class MoneyTransferProcessManager {
	
	private static final Logger logger = LoggerFactory.getLogger(MoneyTransferProcessManager.class);
	
	@Autowired
	EventSourcingRepository repository;
	@Autowired
	EventStore eventStore;
	
	@EventHandlerMethod(topic = "com.tazhi.rose.example.account.event.AccountDebitedEvent", group = "test-group-money-transfer")
	public void processAccountDebitedEvent(SerializedDomainEvent accountDebitedEvent) {
		try {
			//区分非转账的account debit
			logger.info("****** AccountDebitedEvent: " + accountDebitedEvent.getEventBody());
			Map<String, String> evtData = JsonUtils.toMap(accountDebitedEvent.getEventBody(), String.class, String.class);
			String fromAccountId = evtData.get("entityId");
			String transactionId = evtData.get("transactionId");
			int amount = Integer.valueOf(evtData.get("amount"));
			
			MoneyTransfer moneyTransfer = repository.get(MoneyTransfer.class, transactionId);
			if (moneyTransfer == null) {
				logger.info("Not a money transfer transaction, ignoring the AccountDebitedEvent");
				return;
			}
			
			if (moneyTransfer.getState() == MoneyTransfer.State.INITIAL) {
				moneyTransfer.processCommand(new AccountDebitedCommand(transactionId, fromAccountId, amount));
				repository.save(moneyTransfer);
			}
		} catch (IOException e) {
			// event structure error, can not retry! move to dead letter topic
			throw new NoRetryDomainEventProcessingException("Failed to process event, will not be retried : " + accountDebitedEvent.getEventBody(), e);
		}
	}
	
	@EventHandlerMethod(topic = "com.tazhi.rose.example.account.event.AccountCreditedEvent", group = "test-group-money-transfer")
	public void processAccountCreditedEvent(SerializedDomainEvent accountCreditedEvent) {
		try {
			//区分非转账的account credit
			logger.info("****** AccountCreditedEvent: " + accountCreditedEvent.getEventBody());
			Map<String, String> evtData = JsonUtils.toMap(accountCreditedEvent.getEventBody(), String.class, String.class);
			String toAccountId = evtData.get("entityId");
			String transactionId = evtData.get("transactionId");
			int amount = Integer.valueOf(evtData.get("amount"));
			
			MoneyTransfer moneyTransfer = repository.get(MoneyTransfer.class, transactionId);
			if (moneyTransfer == null) {
				logger.info("Not a money transfer transaction, ignoring the AccountCreditedEvent");
				return;
			}
			
			moneyTransfer.processCommand(new AccountCreditedCommand(transactionId, toAccountId, amount));
			repository.save(moneyTransfer);
		} catch (IOException e) {
			throw new DomainEventProcessingException("Failed to process event, will be retried : " + accountCreditedEvent.getEventBody(), e);
		}
	}
	
	@EventHandlerMethod(topic = "com.tazhi.rose.example.transfer.event.MoneyTransferCompletedEvent", group = "test-group-money-transfer")
	public void processMoneyTransferCompletedEvent(SerializedDomainEvent completedEvent) {
		try {
			logger.info("****** MoneyTransferCompletedEvent: " + completedEvent.getEventBody());
			MoneyTransferCreatedEvent completeEvent = JsonUtils.fromJson(completedEvent.getEventBody(), MoneyTransferCreatedEvent.class);
			String transactionId = completeEvent.getTransactionId();
			MoneyTransferCreatedEvent createEvent = (MoneyTransferCreatedEvent)eventStore.loadEvent(MoneyTransfer.class, transactionId, 1);
			if (createEvent != null) {
				long diff = completeEvent.getTimestamp().getTime() - createEvent.getTimestamp().getTime();
				logger.info("******* MoneyTransfer completed in " + diff + "ms");
			} else
				logger.warn("******* MoneyTransfer has been deleted");
		} catch (IOException e) {
			throw new NoRetryDomainEventProcessingException("Failed to process event, will be retried : " + completedEvent.getEventBody(), e);
		}
	}
}
