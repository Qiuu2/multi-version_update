package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2018/1/9.
 */

public class EventBusShutService {
    private String message;

    public EventBusShutService(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
