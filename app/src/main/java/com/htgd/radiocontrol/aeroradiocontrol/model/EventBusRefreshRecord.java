package com.htgd.radiocontrol.aeroradiocontrol.model;

public class EventBusRefreshRecord {
    private String message;

    public EventBusRefreshRecord(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
