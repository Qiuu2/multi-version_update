package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

/**
 * Created by wzq on 2018/3/22.
 */

public class FileResponseModel {
    public String getmediaid() {
        return mediaid;
    }

    public void setmediaid(String mediaid) {
        this.mediaid = mediaid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private String  mediaid; 
            
    private String  state;

    public FileResponseModel(String mediaid, String state) {
        this.mediaid = mediaid;
        this.state = state;
    }
}
