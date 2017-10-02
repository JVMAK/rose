package com.tazhi.rose.util;

import java.lang.reflect.Constructor;

import sun.reflect.ReflectionFactory;

/**
 * 
 * Instantiate objects without invoking any constructors, but depends on Sun/Oracle JVM.
 * 
 * @see http://www.javaspecialists.eu/archive/Issue175.html
 * 
 * @author Evan Wu
 *
 */
@SuppressWarnings("restriction")
public class SilentObjectCreator {
    
    public static <T> T create(Class<T> clazz) throws InstantiationException {
        return create(clazz, Object.class);
    }

    public static <T> T create(Class<T> clazz, Class<? super T> parent) throws InstantiationException {
        try {
            ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
            Constructor<?> objDef = parent.getDeclaredConstructor();
            Constructor<?> intConstr = rf.newConstructorForSerialization(clazz, objDef);
            return clazz.cast(intConstr.newInstance());
        } catch (Exception e) {
            throw new InstantiationException(e.getMessage());
        }
    }
}
