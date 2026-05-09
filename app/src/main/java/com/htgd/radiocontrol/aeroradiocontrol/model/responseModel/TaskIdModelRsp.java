package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzw on 2017/12/12.
 */

public class TaskIdModelRsp implements Serializable {



    private ArrayList< TaskIdModel> data;

    public ArrayList< TaskIdModel> getData() {
        return data;
    }

    public void setData(ArrayList< TaskIdModel> data) {
        this.data = data;
    }
}
