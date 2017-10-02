/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 命令处理异常。
 * 
 * @author Evan Wu
 *
 */
public class CommandProcessingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3069754377786741971L;

	public CommandProcessingException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public CommandProcessingException(String msg) {
		super(msg);
	}
}
