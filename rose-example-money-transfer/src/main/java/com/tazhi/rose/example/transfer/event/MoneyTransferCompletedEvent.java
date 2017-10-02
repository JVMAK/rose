/**
 * 
 */
package com.tazhi.rose.example.transfer.event;

import com.tazhi.rose.event.DomainEvent;

/**
 * 转账完成事件。
 * @author Evan Wu
 *
 */
public class MoneyTransferCompletedEvent extends DomainEvent {
	private String sourceVersion = "1.0";
	private String transactionId;
	private String fromAccountId;
	private String toAccountId;
	private int amount;
	
	protected MoneyTransferCompletedEvent() {}
	
	public MoneyTransferCompletedEvent(String transactionId, String fromAccountId, String toAccountId, int amount) {
		this.entityId = transactionId;
		this.transactionId = transactionId;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
	}

	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getFromAccountId() {
		return fromAccountId;
	}

	public String getToAccountId() {
		return toAccountId;
	}

	public int getAmount() {
		return amount;
	}
}
