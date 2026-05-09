package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;

/**
 * 作者：wzq
 * 时间：2019/3/11:15:48
 * 邮箱：535708929
 * 说明：音量调节
 */
public class EventBusRefreshVoice {
    private String message;

    public EventBusRefreshVoice(String str) {
        this.message = str;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
