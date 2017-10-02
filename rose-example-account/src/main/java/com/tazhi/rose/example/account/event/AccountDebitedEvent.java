/**
 * 
 */
package com.tazhi.rose.example.account.event;

import com.tazhi.rose.event.DomainEvent;

/**
 * 账户取款(转出)事件。
 * @author Evan Wu
 *
 */
public class AccountDebitedEvent extends DomainEvent {
	private String sourceVersion = "1.0";
	private int amount;
	private String transactionId;

	protected AccountDebitedEvent() {}
	
	public AccountDebitedEvent(String accountId, String transactionId, int amount) {
		this.entityId = accountId;
		this.transactionId = transactionId;
		this.amount = amount;
	}

	public int getAmount() {
		return amount;
	}

	public String getTransactionId() {
		return transactionId;
	}

	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}
}
