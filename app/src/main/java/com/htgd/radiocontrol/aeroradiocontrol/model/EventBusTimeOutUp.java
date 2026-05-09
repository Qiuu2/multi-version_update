package com.htgd.radiocontrol.aeroradiocontrol.model;

public class EventBusTimeOutUp {
    private String message;

    public EventBusTimeOutUp(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
