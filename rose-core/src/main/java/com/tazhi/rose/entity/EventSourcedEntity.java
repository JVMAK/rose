/**
 * 
 */
package com.tazhi.rose.entity;

import com.tazhi.rose.command.Command;
import com.tazhi.rose.event.DomainEvent;

/**
 * Event Sourcing实体类的接口。能处理命令和应用事件。
 * 
 * @author Evan Wu
 *
 */
public interface EventSourcedEntity<ID> {
	/**
	 * 处理命令。
	 * 
	 * @param cmd 要处理的事件
	 * @return 改变实体状态的事件(会被保存起来)
	 * 
	 */
	Object processCommand(Command<ID> cmd);
	
	/**
	 * 用事件来更新实体。
	 * 
	 * @param event 表示实体状态改变的事件
	 * @return 更新后的实体，可能是 <b>this</b>
	 */
	AbstractEntity<ID> applyEvent(DomainEvent event);
}
