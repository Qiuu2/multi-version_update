package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

import java.io.Serializable;
/**
 * Created by zongwei on 2017-08-03.
 */
public class TaskZuoxiModel extends BaseModel implements Serializable {
    private int taskcount;
    private String mediaid;
    private String medianame;
    private String starttime;
    private  String  startdate;
    private  String  enddate;
    private int execmode;
    private int volume;
    private int taskstate;
    private int projectstate;
    private int priority;
    private int taskid;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectstatetate() {
        return projectstatetate;
    }

    public void setProjectstatetate(String projectstatetate) {
        this.projectstatetate = projectstatetate;
    }

    private String projectstatetate;
    public int getTaskcount() {
        return taskcount;
    }

    public void setTaskcount(int taskcount) {
        this.taskcount = taskcount;
    }

    public String getMediaid() {
        return mediaid;
    }

    public void setMediaid(String mediaid) {
        this.mediaid = mediaid;
    }

    public String getMedianame() {
        return medianame;
    }

    public void setMedianame(String medianame) {
        this.medianame = medianame;
    }

    public String getStarttime() {
        return starttime;
    }

    public void setStarttime(String starttime) {
        this.starttime = starttime;
    }

    public String getStartDate() {
        return startdate;
    }

    public void setStartDate(String startDate) {
        this.startdate = startDate;
    }

    public String getEndDate() {
        return enddate;
    }

    public void setEndDate(String endDate) {
        this.enddate = endDate;
    }

    public int getExecmode() {
        return execmode;
    }

    public void setExecmode(int execmode) {
        this.execmode = execmode;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getTaskstate() {
        return taskstate;
    }

    public void setTaskstate(int taskstate) {
        this.taskstate = taskstate;
    }

    public int getProjectstate() {
        return projectstate;
    }

    public void setProjectstate(int projectstate) {
        this.projectstate = projectstate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }





}
