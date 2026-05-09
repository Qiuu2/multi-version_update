package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicFolderInfoModel;

import java.util.ArrayList;

/**
 * Created by wzw on 2018/4/7.
 */

public class MusicFolderInfosRsp {
    private ArrayList<MusicFolderInfoModel> data;

    public ArrayList<MusicFolderInfoModel> getData() {
        return data;
    }

    public void setData(ArrayList<MusicFolderInfoModel> data) {
        this.data = data;
    }
}
