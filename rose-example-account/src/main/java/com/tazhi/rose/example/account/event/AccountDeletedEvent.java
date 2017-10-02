package com.tazhi.rose.example.account.event;

import com.tazhi.rose.event.DeleteEvent;

public class AccountDeletedEvent extends DeleteEvent {

    public AccountDeletedEvent(String accountId) {
        entityId = accountId;
    }

    protected AccountDeletedEvent() {
        
    }
    
    @Override
    public String getSourceVersion() {
        return "1.0";
    }

}
