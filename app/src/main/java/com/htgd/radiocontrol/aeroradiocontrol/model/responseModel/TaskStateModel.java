package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;


/**
 * Created by zongwei on 2017-08-16.
 */
public class TaskStateModel implements Serializable{
    private int state;
    private String id;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
