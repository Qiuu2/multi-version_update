package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2017/10/9.
 */

public class EventBusString {
    private String message;

    public EventBusString(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
