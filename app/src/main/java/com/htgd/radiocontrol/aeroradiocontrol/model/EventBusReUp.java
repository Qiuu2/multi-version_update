package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2018/1/23.
 */

public class EventBusReUp {
    private String message;

    public EventBusReUp(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
