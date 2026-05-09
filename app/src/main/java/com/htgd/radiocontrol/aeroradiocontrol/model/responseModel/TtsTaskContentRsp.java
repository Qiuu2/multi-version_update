package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 作者：wzq
 * 时间：2021/4/12:17:30
 * 邮箱：535708929
 * 说明：
 */
public class TtsTaskContentRsp implements Serializable {
    private ArrayList<TtsTaskContentModel> data;

    public ArrayList<TtsTaskContentModel> getData() {
        return data;
    }

    public void setData(ArrayList<TtsTaskContentModel> data) {
        this.data = data;
    }
}
