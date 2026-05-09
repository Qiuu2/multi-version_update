package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;

/**
 * 作者：wzq
 * 时间：2021/8/2:16:26
 * 邮箱：535708929
 * 说明： 停止快捷任务
 */
public class EventBusStopShortTask {
    private String message;
    public EventBusStopShortTask(String str){
        this.message = str;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
