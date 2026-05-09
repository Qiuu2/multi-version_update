package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2017/10/30.
 */

public class EventBusConnect {
    boolean mflag;
    public EventBusConnect(boolean flag){
        this.mflag =flag;
    }

    public boolean isMflag() {
        return mflag;
    }

    public void setMflag(boolean mflag) {
        this.mflag = mflag;
    }
}
