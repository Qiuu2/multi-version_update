package com.htgd.radiocontrol.aeroradiocontrol.constant;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;

import java.util.ArrayList;


/**
 * 作者：wzq
 * 时间：2019/1/22:11:34
 * 邮箱：535708929
 * 说明：
 */
public class Cons {

    public static String pw;
    public static boolean tokenUpdatable;
    public static int stateonline =0;
    public static ArrayList<MachineInfo> chooseMachine=new ArrayList<>();//开始寻呼是在服务里面调起所有定个全局变量
    public static ArrayList<MusicInfoModel> chooseProgList=new ArrayList<>();//开始寻呼是在服务里面调起所有定个全局变量
    public static TaskGuangboModel  shorttask;//开始寻呼是在服务里面调起所有定个全局变量
}
