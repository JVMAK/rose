/**
 * 
 */
package com.tazhi.rose.command;

/**
 * 对某实体做并发操作控制的命令(Command)。需要传递乐观锁控制的{@code version、id}字段。
 * 
 * @param ID 实体Id类型
 * 
 * @author Evan Wu
 *
 */
public interface ConcurrentCommand<ID> extends Command<ID> {
	Integer getVersion();
}
