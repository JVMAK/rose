/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 不可重试的事件处理异常。事件会被移到另外的队列，被另外机制处理(如人工处理)。
 * 
 * @author Evan Wu
 *
 */
public class NoRetryDomainEventProcessingException extends DomainEventProcessingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2703543140305597205L;

	public NoRetryDomainEventProcessingException(String msg, Throwable e) {
		super(msg, e);
	}

	public NoRetryDomainEventProcessingException(String msg) {
		super(msg);
	}

}
