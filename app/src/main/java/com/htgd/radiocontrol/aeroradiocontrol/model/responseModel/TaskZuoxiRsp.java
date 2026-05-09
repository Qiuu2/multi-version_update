package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by wzq on 2017-08-03.
 */
public class TaskZuoxiRsp implements Serializable {
    private ArrayList< TaskZuoxiModel> data;

    public ArrayList< TaskZuoxiModel> getData() {
        return data;
    }

    public void setData(ArrayList< TaskZuoxiModel> data) {
        this.data = data;
    }
}
