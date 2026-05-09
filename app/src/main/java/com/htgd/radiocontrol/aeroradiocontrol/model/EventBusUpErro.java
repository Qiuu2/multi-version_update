package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzq on 2018/1/26.
 */

public class EventBusUpErro {
    private String message;

    public EventBusUpErro(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
