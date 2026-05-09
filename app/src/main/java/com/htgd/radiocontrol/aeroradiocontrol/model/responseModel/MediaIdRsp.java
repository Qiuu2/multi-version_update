package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.FileResponseModel;

import java.util.ArrayList;

/**
 * Created by wzw on 2018/3/22.
 */

public class MediaIdRsp {
    private ArrayList<FileResponseModel> data;

    public ArrayList<FileResponseModel> getData() {
        return data;
    }

    public void setData(ArrayList<FileResponseModel> data) {
        this.data = data;
    }
}
