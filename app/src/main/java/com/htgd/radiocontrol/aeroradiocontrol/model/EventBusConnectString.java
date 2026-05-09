package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzq on 2018/4/7.
 */

public class EventBusConnectString {
    private String message;

    public EventBusConnectString(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
