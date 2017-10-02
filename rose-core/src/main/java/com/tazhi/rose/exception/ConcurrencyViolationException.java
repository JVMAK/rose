/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 并发修改异常。
 * 
 * @author Evan Wu
 *
 */
public class ConcurrencyViolationException extends RoseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1422959794836026653L;

	public ConcurrencyViolationException(String msg) {
		super(msg);
	}

	public ConcurrencyViolationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
