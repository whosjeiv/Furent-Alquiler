package com.alquiler.furent.exception;

import org.springframework.security.authentication.DisabledException;

public class AccountSuspendedException extends DisabledException {
    private final String reason;
    private final String duration;
    private final boolean permanent;

    public AccountSuspendedException(String msg, String reason, String duration, boolean permanent) {
        super(msg);
        this.reason = reason;
        this.duration = duration;
        this.permanent = permanent;
    }

    public String getReason() {
        return reason;
    }

    public String getDuration() {
        return duration;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
