/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 导致事件处理挂起的异常。消息处理会阻塞，消息一直放在原队列里，等待程序修复后再次处理。
 * 
 * @author Evan Wu
 *
 */
public class BlockingDomainEventProcessingException extends DomainEventProcessingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2703543140305597205L;

	public BlockingDomainEventProcessingException(String msg, Throwable e) {
		super(msg, e);
	}

	public BlockingDomainEventProcessingException(String msg) {
		super(msg);
	}

}
