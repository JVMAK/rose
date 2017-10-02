/**
 * 
 */
package com.tazhi.rose.example.transfer.command;

import com.tazhi.rose.command.Command;

/**
 * 账户已入款命令。
 * @author Evan Wu
 *
 */
public class AccountCreditedCommand implements Command<String> {
	
	private String toAccountId;
	private Integer amount;
	private String transactionId;

	public AccountCreditedCommand(String transactionId, String toAccountId, Integer amount) {
		this.transactionId = transactionId;
		this.toAccountId = toAccountId;
		this.amount = amount;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getToAccountId() {
		return toAccountId;
	}

	public Integer getAmount() {
		return amount;
	}

	@Override
	public String getEntityId() {
		return transactionId;
	}
}
