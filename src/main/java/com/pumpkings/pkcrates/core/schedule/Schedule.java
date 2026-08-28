package com.pumpkings.pkcrates.core.schedule;

import java.time.DayOfWeek;

public class Schedule {

    private final String id;
    private final String keyId;
    private final int amount;
    private final java.util.List<String> messages;
    
    // Time configuration
    private final Integer month;
    private final Integer dayOfMonth;
    private final DayOfWeek dayOfWeek;
    private final Integer hour;
    private final Integer minute;

    public Schedule(String id, String keyId, int amount, java.util.List<String> messages, 
                    Integer month, Integer dayOfMonth, DayOfWeek dayOfWeek, 
                    Integer hour, Integer minute) {
        this.id = id;
        this.keyId = keyId;
        this.amount = amount;
        this.messages = messages;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.minute = minute;
    }

    public String getId() {
        return id;
    }

    public String getKeyId() {
        return keyId;
    }

    public int getAmount() {
        return amount;
    }

    public java.util.List<String> getMessages() {
        return messages;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public Integer getHour() {
        return hour;
    }

    public Integer getMinute() {
        return minute;
    }
}
