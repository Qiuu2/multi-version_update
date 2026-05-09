package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;

/**
 * Created by wzw on 2017/12/11.
 */

public class TempTTSModel implements Serializable {

    private String content;
    private String volume;
    private String speed;
    private String taskid;
    private String male;
    private String sort;
    private String taskname;
    private String terminal;
    private String createtime;
    private String timelength;
    private String priority;
    private String state;
    private String number;
    private String zonename;




    public String getip() {
        return ip;
    }

    public void setip(String ip) {
        this.ip = ip;
    }

    private String ip;



    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private String tag;

    public String getMediaurl() {
        return mediaurl;
    }

    public void setMediaurl(String mediaurl) {
        this.mediaurl = mediaurl;
    }

    private String mediaurl;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;

    public String getZonename() {
        return zonename;
    }

    public void setZonename(String zonename) {
        this.zonename = zonename;
    }



    public TempTTSModel(String content, String volume, String speed, String taskid, String male, String sort, String taskname, String createtime, String timelength, String priority, String timelengthtype, String terminal, String state, String number, String username, String zonename, String mediaurl, String tag, String ip) {
        this.content = content;
        this.volume = volume;
        this.speed = speed;
        this.taskid = taskid;
        this.male = male;
        this.sort = sort;
        this.taskname = taskname;
        this.createtime = createtime;
        this.timelength = timelength;
        this.priority = priority;
        this.timelengthtype = timelengthtype;
        this.terminal = terminal;
        this.state = state;
        this.number=number;
        this.username=username;
        this.zonename=zonename;
        this.mediaurl=mediaurl;
        this.tag=tag;
        this.ip=ip;

    }
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public TempTTSModel(String taskname, String createtime, String volume) {
        this.taskname = taskname;
        this.createtime = createtime;
        this.volume = volume;
    }
    public TempTTSModel(String taskid, String username, String content, String state, String createtime, String mediaurl) {
        this.username = username;
        this.content = content;
        this.state = state;
        this.createtime=createtime;
        this.mediaurl=mediaurl;
        this.taskid=taskid;
    }

    public TempTTSModel() {

    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String gettaskid() {
        return taskid;
    }

    public void settaskid(String taskid) {
        this.taskid = taskid;
    }


    public String getTimelength() {
        return timelength;
    }

    public void setTimelength(String timelength) {
        this.timelength = timelength;
    }

    public String getTimelengthtype() {
        return timelengthtype;
    }

    public void setTimelengthtype(String timelengthtype) {
        this.timelengthtype = timelengthtype;
    }

    private String timelengthtype;


    public String getTaskname() {
        return taskname;
    }

    public void setTaskname(String taskname) {
        this.taskname = taskname;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMale() {
        return male;
    }

    public void setMale(String male) {
        this.male = male;
    }
}
