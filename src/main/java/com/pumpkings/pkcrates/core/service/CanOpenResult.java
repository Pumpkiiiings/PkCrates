package com.pumpkings.pkcrates.core.service;

public class CanOpenResult {

    public enum Status {
        SUCCESS,
        DENIED
    }

    public enum Reason {
        NONE,
        MASS_OPENING_DISABLED,
        NOT_ENOUGH_KEYS,
        NO_PERMISSION,
        CRATE_BUSY,
        INVENTORY_FULL
    }

    private final Status status;
    private final Reason reason;
    private final String messageKey;

    private CanOpenResult(Status status, Reason reason, String messageKey) {
        this.status = status;
        this.reason = reason;
        this.messageKey = messageKey;
    }

    public static CanOpenResult success() {
        return new CanOpenResult(Status.SUCCESS, Reason.NONE, null);
    }

    public static CanOpenResult denied(Reason reason, String messageKey) {
        return new CanOpenResult(Status.DENIED, reason, messageKey);
    }

    public boolean isAllowed() {
        return status == Status.SUCCESS;
    }

    public Status getStatus() {
        return status;
    }

    public Reason getReason() {
        return reason;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
