package com.tazhi.rose.example.account.event;

import com.tazhi.rose.event.DomainEvent;

/**
 * 创建账户的事件。
 * @author Evan Wu
 *
 */
public class AccountCreatedEvent extends DomainEvent {
	private String sourceVersion = "1.0";
	private int balance;

	protected AccountCreatedEvent() {}
	
	public AccountCreatedEvent(String accountId, int balance) {
		this.entityId = accountId;
		this.balance = balance;
	}
	
	public int getBalance() {
		return balance;
	}

	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}
}
