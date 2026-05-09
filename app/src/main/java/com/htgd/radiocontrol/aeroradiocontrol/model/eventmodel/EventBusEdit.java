package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;

/**
 * 作者：wzq
 * 时间：2019/3/28:13:20
 * 邮箱：535708929
 * 说明：点击临时语音的item进行编辑
 */
public class EventBusEdit {
    private String message;

    public EventBusEdit(String str) {
        this.message = str;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
