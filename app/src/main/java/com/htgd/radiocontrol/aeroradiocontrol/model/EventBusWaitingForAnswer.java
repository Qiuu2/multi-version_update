package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * 作者：wzq
 * 时间：2019/2/22:9:21
 * 邮箱：535708929
 * 说明：等待对讲接听
 */
public class EventBusWaitingForAnswer {
    private String message;

    public EventBusWaitingForAnswer(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
