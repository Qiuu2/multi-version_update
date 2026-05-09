package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * 作者：wzq
 * 时间：2019/2/22:10:44
 * 邮箱：535708929
 * 说明：被发起对讲弹框
 */
public class EventBusBeRequestSpeech {
    private String message;

    public EventBusBeRequestSpeech(String str) {
        this.message = str;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
