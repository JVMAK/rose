/**
 * 
 */
package com.tazhi.rose.example.account.event;

import com.tazhi.rose.event.DomainEvent;

/**
 * 账户存款失败事件。
 * @author Evan Wu
 *
 */
public class AccountCreditFailedEvent extends DomainEvent {
	private String sourceVersion = "1.0";
	private int amount;
	private String transactionId;
	private String reason;

	protected AccountCreditFailedEvent() {}
	
	public AccountCreditFailedEvent(String accountId, String transactionId, int amount, String reason) {
		this.entityId = accountId;
		this.transactionId = transactionId;
		this.amount = amount;
		this.reason = reason;
	}

	public String getReason() {
		return reason;
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
