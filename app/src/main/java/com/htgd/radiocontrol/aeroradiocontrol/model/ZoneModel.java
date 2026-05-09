package com.htgd.radiocontrol.aeroradiocontrol.model;


import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzq on 2018/3/19.
 */

public class ZoneModel implements Serializable {

    /**
     * all : 4
     * count : 1
     * start : 3
     * state : 0
     * id : 4
     * datetime : 2017-9-5 9:28:43
     * name : 测试分区
     * description : 11
     */

    private String all;
    private int count;
    private int start;
    private int state;
    private int online;
    private int offline;
    private int busyline;

    public ArrayList<MachineInfo> getMachineInfoArrayList() {
        return machineInfoArrayList;
    }

    public void setMachineInfoArrayList(ArrayList<MachineInfo> machineInfoArrayList) {
        this.machineInfoArrayList = machineInfoArrayList;
    }

    private ArrayList<MachineInfo> machineInfoArrayList;



    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public int getOffline() {
        return offline;
    }

    public void setOffline(int offline) {
        this.offline = offline;
    }

    public int getBusyline() {
        return busyline;
    }

    public void setBusyline(int busyline) {
        this.busyline = busyline;
    }


    public ZoneModel(){

    }
    public ZoneModel(String all, int count, int start, int state, int id, String datetime, String name, String description) {
        this.all = all;
        this.count = count;
        this.start = start;
        this.state = state;
        this.id = id;
        this.datetime = datetime;
        this.name = name;
        this.description = description;
    }

    private int id;
    private String datetime;
    private String name;
    private String description;



    public String getAll() {
        return all;
    }

    public void setAll(String all) {
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
