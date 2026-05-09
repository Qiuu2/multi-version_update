package com.htgd.radiocontrol.aeroradiocontrol.model;

import java.io.Serializable;

/**
 * Created by 万志强 on 2017-07-18.
 */
public class BaseModel implements Serializable{
    private int all;
    private int count;
    private int start;
    private int state;

    public int getAll() {
        return all;
    }

    public void setAll(int all) {
        this.all = all;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
