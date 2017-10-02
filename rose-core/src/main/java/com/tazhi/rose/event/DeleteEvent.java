package com.tazhi.rose.event;

/**
 * 实体删除事件。
 * 
 * @author Evan Wu
 *
 */
public abstract class DeleteEvent extends DomainEvent {
    
    public boolean isDelete() {
        return true;
    }
}
