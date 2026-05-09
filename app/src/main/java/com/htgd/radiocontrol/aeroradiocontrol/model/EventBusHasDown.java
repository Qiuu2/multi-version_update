package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2018/1/24.
 */

public class EventBusHasDown {
    private String message;

    public EventBusHasDown(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
