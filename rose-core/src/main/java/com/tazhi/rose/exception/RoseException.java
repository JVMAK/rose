/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * R.O.S.E框架的异常基类。
 * 
 * @author Evan Wu
 *
 */
public class RoseException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6369097648290034524L;

	public RoseException() {}
	
	public RoseException(String msg) {
		super(msg);
	}
	
	public RoseException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public RoseException(Throwable cause) {
		super(cause);
	}
}
