package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

/**
 * Created by wzw on 2017/12/12.
 */

  public class TaskIdModel {
    public String getTaskid() {
        return taskid;
    }

    public void setTaskid(String taskid) {
        this.taskid = taskid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private String taskid;
    private int  id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private  String state;
}
