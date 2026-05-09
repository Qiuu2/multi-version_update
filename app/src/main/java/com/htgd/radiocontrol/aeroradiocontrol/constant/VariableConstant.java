package com.htgd.radiocontrol.aeroradiocontrol.constant;


import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;

import java.util.ArrayList;


/**
 * 作者：wzq
 * 时间：2019/1/22:11:34
 * 邮箱：535708929
 * 说明：全局恒定的变量
 */
public class VariableConstant {


    public static boolean tokenUpdatable=false;
    public static int stateonline =0;
    public static ArrayList<MachineInfo> chooseMachine=new ArrayList<>();//开始寻呼是在服务里面调起所有定个全局变量
    public static ArrayList<MusicInfoModel> chooseProgList=new ArrayList<>();//开始寻呼是在服务里面调起所有定个全局变量
    public static ArrayList<MachineInfo> ttsHostList=new ArrayList<>();
    public static ArrayList<MachineInfo> caiboHostList=new ArrayList<>();
    public static ArrayList<MachineInfo> allMachineList=new ArrayList<>();
    public static ArrayList<MachineInfo> callmachineList=new ArrayList<>();
    public static ArrayList<ZoneModel> zoneListAll=new ArrayList<>();
    public static StringBuffer zonename ;
}
