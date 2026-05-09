package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by zongwei on 2017-07-19.
 */
public class SignTaskModel extends BaseModel{
    private int taskVoice;
    private String taskName;
    private String taskMssage;
    private boolean isSelect;

    public int getTaskVoice() {
        return taskVoice;
    }

    public void setTaskVoice(int taskVoice) {
        this.taskVoice = taskVoice;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskMssage() {
        return taskMssage;
    }

    public void setTaskMssage(String taskMssage) {
        this.taskMssage = taskMssage;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }
}
