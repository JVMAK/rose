/**
 * 
 */
package com.tazhi.rose.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tazhi.rose.command.Command;
import com.tazhi.rose.command.ConcurrentCommand;
import com.tazhi.rose.event.DeleteEvent;
import com.tazhi.rose.event.DomainEvent;
import com.tazhi.rose.exception.ApplyEventException;
import com.tazhi.rose.exception.CommandProcessingException;
import com.tazhi.rose.exception.ConcurrencyViolationException;
import com.tazhi.rose.repository.EventSourcingRepository;
import com.tazhi.rose.util.ReflectionUtils;

/**
 * 抽象的实体类，实体类可以处理(process)命令，应用(apply)事件。
 * </p>
 * 命令(Command)用于驱动改变实体的状态，事件(DomainEvent)用于记录实体的状态变化过程。
 * </p>
 * 注意实体需要一个默认(无参数)的构造函数，用于反序列化时创建实例。
 * </p>
 * 
 * @param ID id type
 * 
 * @author Evan Wu
 *
 */
public abstract class AbstractEntity<ID> implements EventSourcedEntity<ID> {
	/**
	 * ID
	 */
	protected ID id;
	/**
	 * 并发控制的乐观锁版本号
	 */
	protected Integer version = 1;
	/**
	 * 实体是否已删除
	 */
	@JsonInclude(value=Include.NON_DEFAULT)
	protected boolean deleted;
	/**
	 * events to publish when saved
	 */
	@JsonIgnore
	protected List<DomainEvent> uncommitedEvents = Collections.synchronizedList(new ArrayList<DomainEvent>());
	/**
	 * "process", "setVersion" method cache.
	 */
	private static ConcurrentMap<Class<?>, Method> cachedMethods = new ConcurrentHashMap<>();
	/**
	 * 
	 */
	private static ConcurrentMap<Class<?>, Method> cachedApplyMethods = new ConcurrentHashMap<>();
	/**
	 * 并发控制的版本号。
	 */
	public Integer getVersion() {
		return version;
	}

	/**
	 * 实体ID。
	 * @return
	 */
	public ID getId() {
		return id;
	}

	/**
	 * 实体是否已删除。
	 * @return
	 */
	public boolean isDeleted() {
		return deleted;
	}
	
	/**
	 * 标记实体对象已删除，最终会在{@link EventSourcingRepository#save}时被删除。删除事件是实体对象最后的一个事件，删除后不能再对实体对象进行操作/改变状态。
	 * 
	 */
	protected void delete(DeleteEvent deleteEvent) {
		this.deleted = true;
		stateChanged(deleteEvent);
	}
	
	/**
	 * 状态改变，添加状态改变的事件。
	 * @param event
	 */
	protected void stateChanged(DomainEvent event) {
		uncommitedEvents.add(event);
	}
	
	/**
	 * 当前未提交的状态改变事件。
	 * @return
	 */
	public List<DomainEvent> getUncommitedEvents() {
		return uncommitedEvents;
	}
	
	/**
	 * 处理命令。通过反射来调用实体类对应Command子类参数的process()方法。每处理一个命令，实体的版本号会递增。
	 * 
	 * @param cmd 要处理的事件
	 * 
	 */
	public Object processCommand(Command<ID> cmd) {
	    if (this.deleted) {
	        throw new CommandProcessingException("Illegal state, I'm already deleted");
	    }
	    
		if (cmd instanceof ConcurrentCommand) {
			@SuppressWarnings("rawtypes")
			Integer ver = ((ConcurrentCommand)cmd).getVersion();
			if (ver != null)
				failWhenConcurrencyViolation(ver);
		}
		int stateChangedEvents = uncommitedEvents.size();
		
		Method method = null;
		if (cachedMethods.containsKey(cmd.getClass()))
			method = cachedMethods.get(cmd.getClass());
		else {
			method = ReflectionUtils.findMethod(getClass(), "process", cmd.getClass());
			if (method == null)
				throw new CommandProcessingException("Command process method is not defined for command: " + cmd.getClass());
			ReflectionUtils.makeAccessible(method);
			cachedMethods.putIfAbsent(cmd.getClass(), method);
		}
		
		Object ret = null;
		try {
			ret = method.invoke(this, cmd);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new CommandProcessingException("Command process method is not properly defined for command: " + cmd.getClass());
		}
		
		if (stateChangedEvents < uncommitedEvents.size()) {
			if (uncommitedEvents.size() != stateChangedEvents + 1) //one event generated for one command (one event for one version)
				throw new CommandProcessingException("There should be no more than one event generated for one Command, but there was " + (uncommitedEvents.size() - stateChangedEvents));
			this.version ++;
			// set event version
			Class<?> evtClass = uncommitedEvents.get(stateChangedEvents).getClass();
			if (cachedMethods.containsKey(evtClass))
				method = cachedMethods.get(evtClass);
			else {
				method = ReflectionUtils.findMethod(uncommitedEvents.get(stateChangedEvents).getClass(), "setVersion", Integer.class);
				ReflectionUtils.makeAccessible(method);
				cachedMethods.putIfAbsent(evtClass, method);
			}
			try {
				method.invoke(uncommitedEvents.get(stateChangedEvents), this.version);
			} catch (IllegalAccessException | InvocationTargetException e) { // should not happen
				throw new CommandProcessingException("Failed to set version for event: " + uncommitedEvents.get(stateChangedEvents).getClass());
			}
		}
		return ret;
	}

	/**
	 * 用事件来更新实体。通过反射来调用实体类对应DomainEvent子类参数的apply()方法。
	 * 
	 * @param event 表示实体状态改变的事件
	 * @return 更新后的实体，可能是 <b>this</b>
	 */
	public AbstractEntity<ID> applyEvent(DomainEvent event) {
		try {
			Method method = null;
			if (cachedApplyMethods.containsKey(event.getClass()))
				method = cachedApplyMethods.get(event.getClass());
			else {
				method = ReflectionUtils.findMethod(getClass(), "apply", event.getClass());
				if (method == null)
					throw new ApplyEventException("Apply event method is not defined for event: " + event.getClass());
				
				ReflectionUtils.makeAccessible(method);
				cachedApplyMethods.putIfAbsent(event.getClass(), method);
			}
			method.invoke(this, event);
			this.version = event.getVersion();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ApplyEventException("Apply event method is not properly defined for event: " + event.getClass(), e);
		}
		return this;
	}
	
	/**
	 * 当前实体是否已经被更改，如果是则抛出并发异常。
	 * @param version
	 */
	protected void failWhenConcurrencyViolation(Integer version) {
		if (!this.version.equals(version))
			throw new ConcurrencyViolationException(this.getClass().getName() + " with id " + id + " has a newer version " + this.version + " thant " + version);
	}
	
	/**
	 * 简化实体处理单个命令的方法，从仓库加载实体实例，处理命令，并保存实体。
	 * </p>
	 * 等同于执行以下模板代码：<br>
	 * <pre>
	 * SomeEntity entity = repo.get(SomeEntity.class, entityId);
	 * entity.processCommand(SomeCommand);
	 * repo.save(entity);
	 * </pre>
	 * @param entityClass
	 * @param repo
	 * @param cmd
	 * @return 更新后的实体，null如果实体不存在
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends AbstractEntity> T processCommand(Class<? extends AbstractEntity> entityClass, EventSourcingRepository repo, Command cmd) {
		//MethodHandles.lookup().lookupClass() only available in java 7+ but only get 
		//new Throwable().getStackTrace()[0].getClassName()) but slow 
		//Class<? extends AbstractEntity> entityClass = (Class<? extends AbstractEntity>)MethodHandles.lookup().lookupClass();
		if (entityClass == AbstractEntity.class)
			throw new IllegalArgumentException("The caller can not be the AbstractEntity! Use its subclass instead. ");
		AbstractEntity entity = repo.get(entityClass, cmd.getEntityId());
		if (entity == null)
			return null;
		entity.processCommand(cmd);
		repo.save(entity);
		return (T) entity;
	}
}
