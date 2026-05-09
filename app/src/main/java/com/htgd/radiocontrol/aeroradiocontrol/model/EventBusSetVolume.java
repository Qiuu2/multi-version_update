package com.htgd.radiocontrol.aeroradiocontrol.model;

public class EventBusSetVolume {
    private String message;

    public EventBusSetVolume(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}