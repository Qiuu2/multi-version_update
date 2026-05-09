package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

/**
 * Created by wzw on 2017/12/29.
 */

public class TempRspModel {
    public String getTask_id() {
        return task_id;
    }

    public void setTask_id(String task_id) {
        this.task_id = task_id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private String  task_id;
    private String  state;

    public TempRspModel(String task_id, String state) {
        this.task_id = task_id;
        this.state = state;
    }
}
