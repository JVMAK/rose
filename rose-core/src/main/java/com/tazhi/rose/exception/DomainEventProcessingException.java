/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 事件处理异常，<br>注意：如果事件结构有问题等不能通过重试去恢复的，要抛出{@link NoRetryDomainEventProcessingException}，否则事件会被重试。
 * 
 * @author Evan Wu
 *
 */
public class DomainEventProcessingException extends RoseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1392156463879972166L;

	public DomainEventProcessingException(String msg, Throwable e) {
		super(msg, e);
	}

	public DomainEventProcessingException(String msg) {
		super(msg);
	}

}
