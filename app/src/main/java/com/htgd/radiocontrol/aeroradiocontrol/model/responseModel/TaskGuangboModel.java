package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;


import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

import java.io.Serializable;

/**
 * Created by wzq on 2017-08-03.
 */
public class TaskGuangboModel extends BaseModel  implements Serializable {
    public String taskid;
    public int prepower;
    public int level;
    public int volume;
    public int priority;
    public int datasendmodel;
    public String startdate;
    public String enddate;
    public int execmode;
    public int tasktype;
    public String taskname;
    public String starttime;
    public String timelength;
    public String timelengthtype;
    public int israndomplay;
    public String medianame;
    public String sechename;
    public int cmd;
    public String cmdargs;
    public int bandrate;
    public int liveterminalid;
    public String liveterminalname;
    public int samplerate;
    public int caiboprepower;
    public int taskstate;
    public int enablestate;
    public int isinstancy;
    public int projectstate;

    public int getPlaying() {
        return playing;
    }

    public void setPlaying(int playing) {
        this.playing = playing;
    }

    public int playing;


    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    //采播
    public int channel;
    //
    public String name;
    public int taskcount;
    //文字语音
    public int speed;
    public int male;
    public String content;

    public int getLengthtype() {
        return lengthtype;
    }

    public void setLengthtype(int lengthtype) {
        this.lengthtype = lengthtype;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
    public int getMediaid() {
        return mediaid;
    }

    public void setMediaid(int mediaid) {
        this.mediaid = mediaid;
    }

    //作息方案
    public int lengthtype;
    public int length;
    public int mediaid;
    public String info;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public TaskGuangboModel getModel() {
        return model;
    }

    public void setModel(TaskGuangboModel model) {
        this.model = model;
    }

    public TaskGuangboModel model;

    public TaskGuangboModel(String taskid, int prepower, int level, int volume, int priority, int datasendmodel, String startdate, String enddate, int execmode, int tasktype, String taskname, String starttime, String timelength, String timelengthtype, int israndomplay, int cmd, int speed, int male, String content) {
        this.taskid = taskid;
        this.prepower = prepower;
        this.level = level;
        this.volume = volume;
        this.priority = priority;
        this.datasendmodel = datasendmodel;
        this.startdate = startdate;
        this.enddate = enddate;
        this.execmode = execmode;
        this.tasktype = tasktype;
        this.taskname = taskname;
        this.starttime = starttime;
        this.timelength = timelength;
        this.timelengthtype = timelengthtype;
        this.israndomplay = israndomplay;
        this.cmd = cmd;
        this.speed = speed;
        this.male = male;
        this.content = content;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getMale() {
        return male;
    }

    public void setMale(int male) {
        this.male = male;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTaskcount() {
        return taskcount;
    }

    public void setTaskcount(int taskcount) {
        this.taskcount = taskcount;
    }


    public int getProjectstate() {
        return projectstate;
    }

    public void setProjectstate(int projectstate) {
        this.projectstate = projectstate;
    }

    public int getIsinstancy() {
        return isinstancy;
    }

    public void setIsinstancy(int isinstancy) {
        this.isinstancy = isinstancy;
    }

    public int getTaskstate() {
        return taskstate;
    }

    public void setTaskstate(int taskstate) {
        this.taskstate = taskstate;
    }

    public int getEnablestate() {
        return enablestate;
    }

    public void setEnablestate(int enablestate) {
        this.enablestate = enablestate;
    }


    public String getTaskid() {
        return taskid;
    }

    public void setTaskid(String taskid) {
        this.taskid = taskid;
    }

    public int getPrepower() {
        return prepower;
    }

    public void setPrepower(int prepower) {
        this.prepower = prepower;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getDatasendmodel() {
        return datasendmodel;
    }

    public void setDatasendmodel(int datasendmodel) {
        this.datasendmodel = datasendmodel;
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

    public int getExecmode() {
        return execmode;
    }

    public void setExecmode(int execmode) {
        this.execmode = execmode;
    }

    public int getTasktype() {
        return tasktype;
    }

    public void setTasktype(int tasktype) {
        this.tasktype = tasktype;
    }

    public String getTaskname() {
        return taskname;
    }

    public void setTaskname(String taskname) {
        this.taskname = taskname;
    }

    public String getStarttime() {
        return starttime;
    }

    public void setStarttime(String starttime) {
        this.starttime = starttime;
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

    public int getIsrandomplay() {
        return israndomplay;
    }

    public void setIsrandomplay(int israndomplay) {
        this.israndomplay = israndomplay;
    }

    public String getMedianame() {
        return medianame;
    }

    public void setMedianame(String medianame) {
        this.medianame = medianame;
    }

    public String getSechename() {
        return sechename;
    }

    public void setSechename(String sechename) {
        this.sechename = sechename;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getCmdargs() {
        return cmdargs;
    }

    public void setCmdargs(String cmdargs) {
        this.cmdargs = cmdargs;
    }

    public int getBandrate() {
        return bandrate;
    }

    public void setBandrate(int bandrate) {
        this.bandrate = bandrate;
    }

    public int getLiveterminalid() {
        return liveterminalid;
    }

    public void setLiveterminalid(int liveterminalid) {
        this.liveterminalid = liveterminalid;
    }

    public String getLiveterminalname() {
        return liveterminalname;
    }

    public void setLiveterminalname(String liveterminalname) {
        this.liveterminalname = liveterminalname;
    }

    public int getSamplerate() {
        return samplerate;
    }

    public void setSamplerate(int samplerate) {
        this.samplerate = samplerate;
    }

    public int getCaiboprepower() {
        return caiboprepower;
    }

    public void setCaiboprepower(int caiboprepower) {
        this.caiboprepower = caiboprepower;
    }


    public TaskGuangboModel(String taskid, int prepower, int level, int volume, int priority, int datasendmodel, String startdate, String enddate, int execmode, int tasktype, String taskname, String starttime, String timelength, String timelengthtype, int israndomplay, String medianame, String sechename, int cmd, String cmdargs, int bandrate, int liveterminalid, String liveterminalname, int samplerate, int caiboprepower) {
        this.taskid = taskid;
        this.prepower = prepower;
        this.level = level;
        this.volume = volume;
        this.priority = priority;
        this.datasendmodel = datasendmodel;
        this.startdate = startdate;
        this.enddate = enddate;
        this.execmode = execmode;
        this.tasktype = tasktype;
        this.taskname = taskname;
        this.starttime = starttime;
        this.timelength = timelength;
        this.timelengthtype = timelengthtype;
        this.israndomplay = israndomplay;
        this.medianame = medianame;
        this.sechename = sechename;
        this.cmd = cmd;
        this.cmdargs = cmdargs;
        this.bandrate = bandrate;
        this.liveterminalid = liveterminalid;
        this.liveterminalname = liveterminalname;
        this.samplerate = samplerate;
        this.caiboprepower = caiboprepower;
    }

}
