/**
 * 
 */
package com.tazhi.rose.entity;

import org.bson.types.ObjectId;

/**
 * 默认的参照BSON ObjectId实现。
 * 
 * @author Evan Wu
 *
 */
public class DefaultIdGenerator extends IdGenerator {

	@Override
	public String generateId() {
		return new ObjectId().toHexString();
	}

}
