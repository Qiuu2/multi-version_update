package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

/**
 * Created by wzq on 2018/4/7.
 */

public class MusicFolderInfoModel {
    int all;
    int count;
    int start;
    int state;
    int folderid;
    int parentid;
    String name;

    public int getAll() {
        return all;
    }

    public void setAll(int all) {
        this.all = all;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getFolderid() {
        return folderid;
    }

    public void setFolderid(int folderid) {
        this.folderid = folderid;
    }

    public int getParentid() {
        return parentid;
    }

    public void setParentid(int parentid) {
        this.parentid = parentid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
