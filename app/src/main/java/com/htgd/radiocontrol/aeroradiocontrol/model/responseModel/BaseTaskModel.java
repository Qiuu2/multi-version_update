package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;


import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by zongwei on 2017-08-03.
 */
public class BaseTaskModel extends BaseModel {
    private int taskid;
    private int taskstate;
    private String startdate;
    private String enddate;
    private String name;

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }

    public int getTaskstate() {
        return taskstate;
    }

    public void setTaskstate(int taskstate) {
        this.taskstate = taskstate;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
