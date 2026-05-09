package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.SeverStateModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzw on 2017/10/27.
 */

public class SeverStateRsp implements Serializable {

    private ArrayList<SeverStateModel> data;

    public ArrayList<SeverStateModel> getData() {
        return data;
    }

    public void setData(ArrayList<SeverStateModel> data) {
        this.data = data;
    }
}
