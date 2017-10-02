/**
 * 
 */
package com.tazhi.rose.example.command;

import com.tazhi.rose.command.ConcurrentCommand;

/**
 * 取款(转出)命令。命令一般以一个动词开头。
 * @author Evan Wu
 *
 */
public class DebitAccountCommand implements ConcurrentCommand<String> {
	private String id;
	private Integer version;
	private int amount;
	private String transactionId;
	
	public DebitAccountCommand(String accountId, Integer version, String transactionId, int amount) {
		this.id = accountId;
		this.version = version;
		this.transactionId = transactionId;
		this.amount = amount;
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
