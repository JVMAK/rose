/**
 * 
 */
package com.tazhi.rose.entity;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IdGenerator用于生成唯一的Id供Domain对象使用，如Aggregate等。抽象的工厂类来提供一个IdGenerator的实现
 * </p>
 * 使用{@link ServiceLoader}机制来查找实现类，如果没有指定的话将使用默认的BSON的ObjectId机制来生成Id。
 * </p>
 * 如需要指定实现类，请在{@code META/INF/services}目录下创建名为{@code com.tazhi.rose.aggregate.IdFactory}的文件。内容为实现类的全类名。
 * 实现类必须要有一个public无参数的构造函数。
 * 
 * @author Evan Wu
 *
 */
public abstract class IdGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(IdGenerator.class);
	private static IdGenerator INSTANCE;
	
	static {
        logger.debug("Looking for IdFactory implementation using the context class loader");
        IdGenerator factory = locateFactories(Thread.currentThread().getContextClassLoader(), "Context");
        if (factory == null) {
            logger.debug("Looking for IdFactory implementation using the IdFactory class loader.");
            factory = locateFactories(IdGenerator.class.getClassLoader(), "IdFactory");
        }
        if (factory == null) {
            factory = new DefaultIdGenerator();
            logger.debug("Using default BSON ObjectId IdFactory");
        } else {
            logger.info("Found custom IdFactory implementation: {}", factory.getClass().getName());
        }
        INSTANCE = factory;
    }

    private static IdGenerator locateFactories(ClassLoader classLoader, String classLoaderName) {
    	IdGenerator found = null;
        Iterator<IdGenerator> services = ServiceLoader.load(IdGenerator.class, classLoader).iterator();
        if (services.hasNext()) {
            logger.debug("Found IdFactory implementation using the {} Class Loader", classLoaderName);
            found = services.next();
            if (services.hasNext()) {
                logger.warn("More than one IdFactory implementation was found using the {} "
                                    + "Class Loader. This may result in different selections being made after "
                                    + "restart of the application.", classLoaderName);
            }
        }
        return found;
    }

    /**
     * 返回当前classpath中IdFactory的实例。如果没有指定, 默认是BSON ObjectId实现。
     *
     * @return IdFactory
     *
     * @see ServiceLoader
     */
    public static IdGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * 生成唯一的Id。
     *
     * @return String: 唯一的Id
     */
    public abstract String generateId();
}
