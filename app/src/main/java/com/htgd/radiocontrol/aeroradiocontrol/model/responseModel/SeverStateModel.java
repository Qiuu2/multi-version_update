package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;

/**
 * Created by wzw on 2017/10/27.
 */

public class SeverStateModel implements Serializable{
    private int state;
    private int connection;
    private int taskcount;
    private int bandwidth;
    private long maxconnection;
    private int ctrlport;
    private int dataport;
    private String  name;
    private String ip;
    private String gate;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getConnection() {
        return connection;
    }

    public void setConnection(int connection) {
        this.connection = connection;
    }

    public int getTaskcount() {
        return taskcount;
    }

    public void setTaskcount(int taskcount) {
        this.taskcount = taskcount;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public long getMaxconnection() {
        return maxconnection;
    }

    public void setMaxconnection(long maxconnection) {
        this.maxconnection = maxconnection;
    }

    public int getCtrlport() {
        return ctrlport;
    }

    public void setCtrlport(int ctrlport) {
        this.ctrlport = ctrlport;
    }

    public int getDataport() {
        return dataport;
    }

    public void setDataport(int dataport) {
        this.dataport = dataport;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getGate() {
        return gate;
    }

    public void setGate(String gate) {
        this.gate = gate;
    }
}
