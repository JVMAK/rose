/**
 * 
 */
package com.tazhi.rose.example.command;

import com.tazhi.rose.command.ConcurrentCommand;

/**
 * 存款(转入)命令。命令一般以一个动词开头。
 * 
 * @author Evan Wu
 *
 */
public class CreditAccountCommand implements ConcurrentCommand<String> {
	private String id;
	private Integer version;
	private int amount;
	private String transactionId;
	
	public CreditAccountCommand(String accountId, Integer version, String transactionId, int amount) {
		this.id = accountId;
		this.version = version;
		this.amount = amount;
		this.transactionId = transactionId;
	}
	
	@Override
	public String getEntityId() {
		return id;
	}

	@Override
	public Integer getVersion() {
		return version;
	}

	public int getAmount() {
		return amount;
	}

	public String getTransactionId() {
		return transactionId;
	}
}
