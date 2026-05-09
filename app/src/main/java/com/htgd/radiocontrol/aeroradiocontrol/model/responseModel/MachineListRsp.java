package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by wzq on 2017-08-01.
 */
public class MachineListRsp implements Serializable {

    private ArrayList<MachineInfo> data;

    public ArrayList<MachineInfo> getData() {
        return data;
    }

    public void setData(ArrayList<MachineInfo> data) {
        this.data = data;
    }
}
