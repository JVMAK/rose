/**
 * 
 */
package com.tazhi.rose.example.transfer.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tazhi.rose.entity.AbstractEntity;
import com.tazhi.rose.example.transfer.command.AccountCreditFailedCommand;
import com.tazhi.rose.example.transfer.command.AccountCreditedCommand;
import com.tazhi.rose.example.transfer.command.AccountDebitFailedCommand;
import com.tazhi.rose.example.transfer.command.AccountDebitedCommand;
import com.tazhi.rose.example.transfer.event.MoneyTransferCompletedEvent;
import com.tazhi.rose.example.transfer.event.MoneyTransferCreatedEvent;
import com.tazhi.rose.example.transfer.event.MoneyTransferDebitRecordedEvent;
import com.tazhi.rose.example.transfer.event.MoneyTransferFailedEvent;
import com.tazhi.rose.exception.ApplyEventException;

/**
 * 一次转账。"saga"
 * 
 * @see <a href="http://stackoverflow.com/questions/15528015/what-is-the-difference-between-a-saga-a-process-manager-and-a-document-based-ap">Saga, Process Manager</a>
 * @author Evan Wu
 *
 */
public class MoneyTransfer extends AbstractEntity<String> {
	
	private static final Logger logger = LoggerFactory.getLogger(MoneyTransfer.class);
	/**
	 * 转账的几种状态
	 */
	public static enum State {
		INITIAL,
		DEBITED,
		COMPLETED,
		FAILED
	}
	/**
	 * 转账的状态
	 */
	private State state = State.INITIAL;
	/**
	 * 转出账号
	 */
	private String fromAccountId;
	/**
	 * 转入账号
	 */
	private String toAccountId;
	/**
	 * 转账金额
	 */
	private int amount;
	/**
	 * 失败原因
	 */
	@JsonInclude(Include.NON_NULL)
	private String failedReason;
	
	/**
	 * 提供默认的构造函数供框架反序列化时使用
	 */
	protected MoneyTransfer() {}
	
	/**
	 * 创建一次转账。
	 * @param transactionId
	 * @param fromAccountId
	 * @param toAccountId
	 * @param amount
	 */
	public MoneyTransfer(String transactionId, String fromAccountId, String toAccountId, int amount) {
		this.id = transactionId;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
		stateChanged(new MoneyTransferCreatedEvent(transactionId, fromAccountId, toAccountId, amount));
	}

	/**
	 * 处理账户已扣款命令。
	 * @param cmd
	 */
	protected void process(AccountDebitedCommand cmd) {
		if (this.state == State.INITIAL) {
			this.state = State.DEBITED;
			stateChanged(new MoneyTransferDebitRecordedEvent(this.id, this.fromAccountId, this.toAccountId, this.amount));
		} else {
			logger.warn("Can not process AccountDebitedCommand, MoneyTransfer state is " + this.state);
		}
	}
	
	/**
	 * 处理账户已入账命令。
	 * @param cmd
	 */
	protected void process(AccountCreditedCommand cmd) {
		if (this.state == State.DEBITED) {
			this.state = State.COMPLETED;
			stateChanged(new MoneyTransferCompletedEvent(this.id, this.fromAccountId, this.toAccountId, this.amount));
		} else {
			logger.warn("Can not process AccountDebitedCommand, MoneyTransfer state is " + this.state);
		}
	}
	
	/**
	 * 处理账户转出失败的命令。
	 * @param cmd
	 */
	protected void process(AccountDebitFailedCommand cmd) {
		if (this.state == State.INITIAL) {
			this.state = State.FAILED;
			this.failedReason = cmd.getReason();
			stateChanged(new MoneyTransferFailedEvent(this.id, this.fromAccountId, this.toAccountId, this.amount, cmd.getReason()));
		} else {
			logger.warn("Can not process AccountDebitFailedCommand, MoneyTransfer state is " + this.state);
		}
	}
	
	/**
	 * 处理账户转入失败的命令。
	 * @param cmd
	 */
	protected void process(AccountCreditFailedCommand cmd) {
		if (this.state == State.INITIAL) {
			this.state = State.FAILED;
			this.failedReason = cmd.getReason();
			stateChanged(new MoneyTransferFailedEvent(this.id, this.fromAccountId, this.toAccountId, this.amount, cmd.getReason()));
		} else {
			logger.warn("Can not process AccountDebitFailedCommand, MoneyTransfer state is " + this.state);
		}
	}
	
	/**
	 * 应用创建转账的事件。
	 * @param evt
	 */
	protected void apply(MoneyTransferCreatedEvent evt) {
		this.id = evt.getTransactionId(); // evt.getEntityId();
		this.fromAccountId = evt.getFromAccountId();
		this.toAccountId = evt.getToAccountId();
		this.amount = evt.getAmount();
		this.state = State.INITIAL;
	}
	
	protected void apply(MoneyTransferDebitRecordedEvent evt) {
		if (this.state == State.INITIAL) {
			this.id = evt.getTransactionId(); // evt.getEntityId();
			this.fromAccountId = evt.getFromAccountId();
			this.toAccountId = evt.getToAccountId();
			this.amount = evt.getAmount();
			this.state = State.DEBITED;
		} else {
			throw new ApplyEventException("MoneyTransfer state is " + this.state + ", can not apply MoneyTransferCreatedEvent");
		}
	}
	
	/**
	 * 应用转账成功的事件。
	 * @param evt
	 */
	protected void apply(MoneyTransferCompletedEvent evt) {
		this.id = evt.getTransactionId(); // evt.getEntityId();
		this.fromAccountId = evt.getFromAccountId();
		this.toAccountId = evt.getToAccountId();
		this.amount = evt.getAmount();
		this.state = State.COMPLETED;
	}
	
	public State getState() {
		return state;
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

	public String getFailedReason() {
		return failedReason;
	}
}
