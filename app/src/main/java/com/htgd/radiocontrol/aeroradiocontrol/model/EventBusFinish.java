package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2018/1/6.
 */

public class EventBusFinish {
    private String message;

    public EventBusFinish(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}