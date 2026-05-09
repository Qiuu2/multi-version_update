package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * 作者：wzq
 * 时间：2019/3/29:13:36
 * 邮箱：535708929
 * 说明：
 */
public class EventBusGetVolume {
    private int count;

    public EventBusGetVolume(int count) {
        this.count=count;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
