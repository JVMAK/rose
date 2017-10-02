/**
 * 
 */
package com.tazhi.rose.example.account.entity;

import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.example.account.event.AccountCreatedEvent;
import com.tazhi.rose.example.account.event.AccountCreditedEvent;
import com.tazhi.rose.example.account.event.AccountDebitedEvent;
import com.tazhi.rose.example.account.event.AccountDeletedEvent;
import com.tazhi.rose.example.command.CreditAccountCommand;
import com.tazhi.rose.example.command.DebitAccountCommand;
import com.tazhi.rose.example.command.DeleteAccountCommand;

/**
 * 一个简单的账户实体。继承自{@link AbstractEntity}，按照Event Sourcing的方式处理命令与应用事件。
 * 
 * @author Evan Wu
 *
 */
public class Account extends AbstractEntity<String> {
	/**
	 * 余额
	 */
	private int balance;
	
	/**
	 * 框架需要默认的构造函数来反序列化。
	 */
	protected Account() {
	}
	
	/**
	 * 创建一个账户。
	 * @param id
	 * @param balance
	 */
	public Account(String id, int balance) {
		this.id = id;
		this.balance = balance;
		stateChanged(new AccountCreatedEvent((String)this.id, this.balance));
	}
	
	/**
	 * 返回余额。
	 * @return
	 */
	public int getBalance() {
		return balance;
	}
	
	/**
	 * 取款。
	 * @param amount
	 */
	public void debit(int amount) {
		this.balance -= amount;
	}
	
	/**
	 * 存款。
	 * @param amount
	 */
	public void credit(int amount) {
		this.balance += amount;
	}
	
	/**
	 * 处理取款命令。
	 * @param command
	 */
	protected void process(DebitAccountCommand command) {
		debit(command.getAmount());
		stateChanged(new AccountDebitedEvent(command.getEntityId(), command.getTransactionId(), command.getAmount()));
	}
	
	/**
	 * 处理存款命令。
	 * @param command
	 */
	protected void process(CreditAccountCommand command) {
		credit(command.getAmount());
		stateChanged(new AccountCreditedEvent(command.getEntityId(), command.getTransactionId(), command.getAmount()));
	}
	
	/**
	 * 处理删除账户命令。
	 * @param command
	 */
	protected void process(DeleteAccountCommand command) {
		super.delete(new AccountDeletedEvent(command.getEntityId()));
	}
	
	/**
	 * 应用创建账号事件。
	 * @param event
	 */
	protected void apply(AccountCreatedEvent event) {
		this.id = (String)event.getEntityId();
		this.balance = event.getBalance();
	}
	
	/**
	 * 应用取款事件。
	 * @param event
	 */
	protected void apply(AccountDebitedEvent event) {
		debit(event.getAmount());
	}
	
	/**
	 * 应用存款事件。
	 * @param event
	 */
	protected void apply(AccountCreditedEvent event) {
		credit(event.getAmount());
	}
	
	/**
	 * 应用删除事件。
	 * @param event
	 */
	protected void apply(AccountDeletedEvent event) {
		this.deleted = true;
	}
}
