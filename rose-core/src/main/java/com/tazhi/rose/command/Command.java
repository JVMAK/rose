/**
 * 
 */
package com.tazhi.rose.command;

/**
 * 命令(Command)接口。命令用于改变实体的状态。
 * 
 * @author Evan Wu
 *
 */
public interface Command<ID> {
	ID getEntityId();
}
