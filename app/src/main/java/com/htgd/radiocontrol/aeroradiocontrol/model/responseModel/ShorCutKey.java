package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by wzq on 2017/10/26.
 */
/*
 * shortcutkey 快捷键
 **/
public class ShorCutKey extends BaseModel {
    private int terminalid;
    private int shortcutkey;//任务id
    private String name;
    private int id;

    public int getTerminalid() {
        return terminalid;
    }

    public void setTerminalid(int terminalid) {
        this.terminalid = terminalid;
    }

    public int getShortcutkey() {
        return shortcutkey;
    }

    public void setShortcutkey(int shortcutkey) {
        this.shortcutkey = shortcutkey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
