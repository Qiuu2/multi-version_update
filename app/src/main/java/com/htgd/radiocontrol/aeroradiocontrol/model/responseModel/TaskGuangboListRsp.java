package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by wzq on 2017-08-03.
 */
public class TaskGuangboListRsp implements Serializable {

    private ArrayList< TaskGuangboModel> data;

    public ArrayList< TaskGuangboModel> getData() {
        return data;
    }

    public void setData(ArrayList< TaskGuangboModel> data) {
        this.data = data;
    }
}
