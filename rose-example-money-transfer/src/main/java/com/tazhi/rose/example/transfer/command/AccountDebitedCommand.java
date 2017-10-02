/**
 * 
 */
package com.tazhi.rose.example.transfer.command;

import com.tazhi.rose.command.Command;

/**
 * 账户已扣款命令。
 * @author Evan Wu
 *
 */
public class AccountDebitedCommand implements Command<String> {
	private String transactionId;
	private String fromAccountId;
	private Integer amount;

	public AccountDebitedCommand(String transactionId, String fromAccountId, Integer amount) {
		this.transactionId = transactionId;
		this.fromAccountId = fromAccountId;
		this.amount = amount;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getFromAccountId() {
		return fromAccountId;
	}

	public Integer getAmount() {
		return amount;
	}

	@Override
	public String getEntityId() {
		return transactionId;
	}
}
