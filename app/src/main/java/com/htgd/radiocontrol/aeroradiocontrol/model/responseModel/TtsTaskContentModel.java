package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;

/**
 * 作者：wzq
 * 时间：2021/4/12:17:25
 * 邮箱：535708929
 * 说明：文字语音任务文字内容
 */
/*"state": 0,
      "taskid": "71396",
      "speed": 1,
      "male": 80,
      "contents": "哦天记录记录塔园路无距离不开"*/
public class TtsTaskContentModel   implements Serializable {

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int state;

    public TtsTaskContentModel(int state, int taskid, int speed, int male, String contents) {
        this.state = state;
        this.taskid = taskid;
        this.speed = speed;
        this.male = male;
        this.contents = contents;
    }

    public int taskid;



    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
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

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public int speed;
    public int male;
    public String contents;
}
