/**
 * 
 */
package com.tazhi.rose.exception;

/**
 * 保存实体异常。
 * 
 * @author Evan Wu
 *
 */
public class RepositoryException extends RoseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2867320194724038029L;

	public RepositoryException(String msg, Throwable e) {
		super(msg, e);
	}
	
	public RepositoryException(String msg) {
		super(msg);
	}
	
	public RepositoryException(Throwable cause) {
		super(cause);
	}
}
