package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;


import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzw on 2017/12/29.
 */

public class TempTaskRsp implements Serializable {
    private ArrayList<TempRspModel> data;

    public ArrayList<TempRspModel> getData() {
        return data;
    }

    public void setData(ArrayList<TempRspModel> data) {
        this.data = data;
    }
}

