/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 应用事件异常。
 * 
 * @author Evan Wu
 *
 */
public class ApplyEventException extends RoseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2082880366864396843L;

	public ApplyEventException(Throwable e) {
		super(e);
	}

	public ApplyEventException(String msg, ReflectiveOperationException e) {
		super(msg, e);
	}

	public ApplyEventException(String msg) {
		super(msg);
	}

}
