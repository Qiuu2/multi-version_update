package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzq on 2017-08-07.
 */
public class MusicInfosRsp implements Serializable {
    private ArrayList<MusicInfoModel> data;

    public ArrayList<MusicInfoModel> getData() {
        return data;
    }

    public void setData(ArrayList<MusicInfoModel> data) {
        this.data = data;
    }
}
