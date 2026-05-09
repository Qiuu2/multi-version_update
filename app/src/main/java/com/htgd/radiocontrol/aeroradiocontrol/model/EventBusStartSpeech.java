package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * 作者：wzq
 * 时间：2019/2/22:9:32
 * 邮箱：535708929
 * 说明：开始对讲
 */
public class EventBusStartSpeech {
    private String message;

    public EventBusStartSpeech(String str) {
        this.message = str;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
