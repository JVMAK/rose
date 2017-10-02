/**
 * 
 */
package com.tazhi.rose.example.transfer.command;

import com.tazhi.rose.command.Command;

/**
 * 账户收款失败命令。
 * @author Evan Wu
 *
 */
public class AccountCreditFailedCommand implements Command<String> {
	private String transactionId;
	private String fromAccountId;
	private String reason;

	public AccountCreditFailedCommand(String transactionId, String fromAccountId, String reason) {
		this.transactionId = transactionId;
		this.fromAccountId = fromAccountId;
		this.reason = reason;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getFromAccountId() {
		return fromAccountId;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String getEntityId() {
		return transactionId;
	}
}
