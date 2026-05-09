package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2017/10/26.
 */

public class EventBusRefuse {
    private String message;

    public EventBusRefuse(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
