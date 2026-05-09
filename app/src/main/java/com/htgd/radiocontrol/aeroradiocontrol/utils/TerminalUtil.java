package com.htgd.radiocontrol.aeroradiocontrol.utils;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans.RefreshPeriodTimeInCall;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.view.menu.MenuBuilder;

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalUtil {

    private Context mContext;
    private String mTag="TerminalUtil";
    private Timer timer;
    private TimerTask timerTask;
    private ArrayList<MachineInfo> signMachineList;

    public TerminalUtil(Context  context,ArrayList<MachineInfo> s){
        this.mContext = context ;
        this.signMachineList=s;
    }
    //定时刷新终端列表
    public void refreshMachineStateInTime() {
        LogUtils.setLog(mTag, "KAISHISHUAXIN");
        if (timer == null) {
            timer = new Timer();
        }
        if(timerTask==null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        refreshMachineList();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.schedule(timerTask, 2000, RefreshPeriodTimeInCall);//
        }else{

        }


    }
    // 刷新设备信息
    private synchronized void refreshMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelist + IntConstans.FLAG_XUNHU, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag + "refreshMachineList get sucess code is" + code + "message is" + response);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    lists = ArryListUtils.getInStance().cutTheSameMachine(lists, mContext);
                    signMachineList.clear();
                    if(lists.size()>0) {
                        for (int i = 0; i < lists.size(); i++) {
                            if(lists.get(i).getType()!=41&&lists.get(i).getType()!=17) {//去除手机和应急终端
                                signMachineList.add(lists.get(i));
                            }
                        }
                        resetData();
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                              //  updateUI();
                            }
                        });
                    } else {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju"+signMachineList.size());


                              //  list.refreshComplete(signMachineList.size());//停止刷新
                               // list.setEmptyView(empty);
                            }
                        });

                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag + "get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                   // reTry();
                }
            }
        });

    }


    public void resetData() {
        if(signMachineList.size()>0) {
            if (signMachineList.get(0).getName() != null) {
                if (signMachineList.size() > 0 && signMachineList.get(0).getName() != null) {
                    for (int i = 0; i < signMachineList.size(); i++) { //去除自己
                        if (signMachineList.get(i).getName().equals(PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext))) {
                            signMachineList.remove(i);
                        }
                    }
                }

                //userAdapter.setMachineArrayList(signMachineList);
                //服务器获取的都是未选中的，
               /* if (signMachineList.size() > 0 && map.size() > 0) {
                    for (int i = 0; i < signMachineList.size(); i++) {
                        for (String key : map.keySet()) {
                            if (signMachineList.get(i).getName().equals(key)) {
                                signMachineList.get(i).setChoose(map.get(signMachineList.get(i).getName()));
                            }
                        }
                    }
                }*/
            } else {
                signMachineList.clear();
            }
        }
    }



}
