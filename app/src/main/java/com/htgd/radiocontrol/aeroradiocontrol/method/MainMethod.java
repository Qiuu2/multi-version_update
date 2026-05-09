package com.htgd.radiocontrol.aeroradiocontrol.method;

import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

/**
 * 作者：wzq
 * 时间：2019/1/23:17:20
 * 邮箱：535708929
 * 说明:寻呼对讲点播的指令集合
 */
public class MainMethod {
    private static String mTAG="MainMethod";
    public static HTIntf htIntf=new HTIntf() ;

    public static void  startCall(ArrayList<MachineInfo> chooseMachineList){
        if(htIntf==null) {
            LogUtils.setLog(mTAG,"新的htintf");
              htIntf=new HTIntf();
        }
            htIntf.newpagingslist();
            for (MachineInfo model : chooseMachineList) {
                htIntf.setpaginglistitem(model.getId());

                LogUtils.setLog(mTAG, "has selected " + model.getId());
            }
            int state = htIntf.startpaging();

    }
    public static void startplay(ArrayList<MachineInfo> machineList, ArrayList<MusicInfoModel> chooseProgList) {
        htIntf.newondemandlist();
        LogUtils.setLog(mTAG, "startplay");
        if ( machineList!=null&&machineList.size() > 0) {
            for (MachineInfo info : machineList) {
                htIntf.setondemandterminal(info.getId());
            }
        } else {
            LogUtils.setLog(mTAG, "the list is  null");
        }
        if (chooseProgList!=null&&chooseProgList.size() > 0) {
            for (MusicInfoModel music : chooseProgList) {
                    htIntf.setondemandmedia(music.getMediaid());
            }
        }
        int state = htIntf.startondemand();
    }

    public static void startSpeech(ArrayList<MachineInfo> chooseMachine) {
        LogUtils.setLog("the selet spack id is" + chooseMachine.get(0).getId());
        htIntf.newspeechitem();
        htIntf.setspeechitem(chooseMachine.get(0).getId());
        int state = htIntf.startspeech();
    }



}
