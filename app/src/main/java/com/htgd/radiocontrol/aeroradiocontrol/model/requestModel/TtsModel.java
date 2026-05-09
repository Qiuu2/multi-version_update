package com.htgd.radiocontrol.aeroradiocontrol.model.requestModel;


import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * 作者：wzq
 * 时间：2021/2/26:10:45
 * 邮箱：535708929
 * 说明：提交tts任务模型
 */
/*{
  "taskid": "1",
  "prepower": 0,
  "level": 0,
  "volume": 0,
  "priority": 0,
  "datasendmodel": 0,
  "startdate": "2021-02-26",
  "enddate": "2021-02-26",
  "execmode": 128,
  "tasktype": 17,
  "taskname": "sdsd",
  "starttime": "11:20:20",
  "timelength": "20",
  "timelengthtype": "2",
  "israndomplay": 0,
  "cmd": 0,
  "male": 0,
  "speed": 0,
  "content": "string"
}*/
  public class TtsModel extends BaseModel {
    private String taskid;
    private int prepower;
    private int level;
    private int volume;
    private int priority;
    private int datasendmodel;
    private String startdate;
    private String enddate;
    private int execmode;
    private int tasktype;
    private String taskname;
    private String starttime;
    private String timelength;
    private String timelengthtype;
    private int israndomplay;
    private int cmd;
    private int speed;
    private int male;
    private String content;
    public TtsModel(String taskid, int prepower, int level, int volume, int priority, int datasendmodel, String startdate, String enddate, int execmode, int tasktype, String taskname, String starttime, String timelength, String timelengthtype, int israndomplay, int cmd, int speed, int male, String content) {
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


    public int getIsrandomplay() {
        return israndomplay;
    }

    public void setIsrandomplay(int israndomplay) {
        this.israndomplay = israndomplay;
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

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
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


}
