package com.htgd.radiocontrol.aeroradiocontrol.model;

public class EventBusRecord {
    private String message;

    public EventBusRecord(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}