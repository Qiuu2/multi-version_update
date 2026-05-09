package com.htgd.radiocontrol.aeroradiocontrol.model;

import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;

import java.util.ArrayList;

/**
 * Created by wzw on 2018/3/19.
 */

public class ZoneRsp {
    private ArrayList<ZoneModel> data;

    public ArrayList<ZoneModel> getData() {
        return data;
    }

    public void setData(ArrayList<ZoneModel> data) {
        this.data = data;
    }
}
