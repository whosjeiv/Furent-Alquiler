package com.alquiler.furent.event;

import com.alquiler.furent.model.User;

public class UserRegisteredEvent extends FurentEvent {

    private final User user;

    public UserRegisteredEvent(Object source, User user, String tenantId) {
        super(source, tenantId, user.getId());
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
