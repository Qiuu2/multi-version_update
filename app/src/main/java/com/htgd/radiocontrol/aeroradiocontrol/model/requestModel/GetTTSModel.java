package com.htgd.radiocontrol.aeroradiocontrol.model.requestModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;


/**
 * Created by wzw on 2017/11/23.
 */

public class GetTTSModel extends BaseModel {

    private Boolean ttsState;
    private String ttsName;
    private Boolean hasXunFei;

    public GetTTSModel(Boolean ttsState, String ttsName, Boolean hasXunFei) {
        this.ttsState = ttsState;
        this.ttsName = ttsName;
        this.hasXunFei = hasXunFei;
    }

    public Boolean getttsState() {
        return ttsState;
    }

    public void setttsState(Boolean ttsState) {
        this.ttsState = ttsState;
    }

    public Boolean gethasXunFei() {
        return hasXunFei;
    }

    public void sethasXunFei(Boolean hasXunFei) {
        this.hasXunFei = hasXunFei;
    }

    public String getttsName() {
        return ttsName;
    }

    public void setttsName(String ttsName) {
        this.ttsName = ttsName;
    }
}
