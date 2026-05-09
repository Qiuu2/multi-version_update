package com.htgd.radiocontrol.aeroradiocontrol.model;

public class EventBusHide {
    private String message;

    public EventBusHide(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
