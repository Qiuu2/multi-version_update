package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzq on 2017-08-16.
 */
public class TaskStateRsp implements Serializable{
    ArrayList< TaskStateModel> data;

    public ArrayList< TaskStateModel> getData() {
        return data;
    }

    public void setData(ArrayList< TaskStateModel> data) {
        this.data = data;
    }
}
