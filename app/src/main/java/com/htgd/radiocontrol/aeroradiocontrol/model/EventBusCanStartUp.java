package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2018/1/8.
 */

public class EventBusCanStartUp {
    private String message;

    public EventBusCanStartUp(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
