package com.tazhi.rose.example.command;

import com.tazhi.rose.command.ConcurrentCommand;

/**
 * 删除账户命令。
 * @author Evan Wu
 *
 */
public class DeleteAccountCommand implements ConcurrentCommand<String> {
	private String id;
	private Integer version;
	
	public DeleteAccountCommand(String id, Integer version) {
		this.id = id;
		this.version = version;
	}
	
	@Override
	public String getEntityId() {
		return id;
	}
	@Override
	public Integer getVersion() {
		return version;
	}
}
