package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;

/**
 * 作者：wzq
 * 时间：2018/10/8:15:57
 * 邮箱：535708929
 * 说明：显示是否在线
 */
public class EventBusNetState {
    private String message;
    public EventBusNetState(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
