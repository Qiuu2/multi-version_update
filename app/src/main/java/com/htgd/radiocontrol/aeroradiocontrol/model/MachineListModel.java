package com.htgd.radiocontrol.aeroradiocontrol.model;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by wzq on 2017/10/17.
 */

public class MachineListModel implements Serializable{
     private ArrayList<MachineInfo> machineInfos;

    public ArrayList<MachineInfo> getMachineInfos() {
        return machineInfos;
    }

    public void setMachineInfos(ArrayList<MachineInfo> machineInfos) {
        this.machineInfos = machineInfos;
    }
}
