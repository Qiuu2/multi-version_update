package com.htgd.radiocontrol.aeroradiocontrol.httptask;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant.allMachineList;

import android.app.Activity;
import android.content.Context;

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 作者：wzq
 * 时间：2021/7/22:9:14
 * 邮箱：535708929
 * 说明：
 */
public class ZoneMethod {

    private Context mContext;
    private String mTag = "ZoneMethod";
    private ArrayList<MachineInfo> zoneMachineLists;
    private int currentposition;

    public ZoneMethod(Context context) {
        this.mContext = context;
    }

    public synchronized ArrayList<MachineInfo> getZoneTerminal(final int zoneid, final ArrayList<MachineInfo> zoneMachineList) throws IOException {
        this.zoneMachineLists = zoneMachineList;
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getGroupTerminal + "/" + zoneid, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                zoneMachineLists.clear();
                LogUtils.setLog(mTag, "fenqu拿终端" + zoneid);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    if(zoneid==0){
                        for (int i = 0; i < allMachineList.size() ; i++) {
                            zoneMachineLists.add(allMachineList.get(i));
                        }

                    }else {
                        if (responseData.getData().get(0).getName() != null) {
                            LogUtils.setLog(mTag, "fenqu拿终端名称" + responseData.getData().get(0).getName());
                            for (int j = 0; j < responseData.getData().size(); j++) {
                                zoneMachineLists.add(responseData.getData().get(j));

                            }
                        }
                    }
                    LogUtils.setLog(mTag, "fenqu终端数量" + zoneMachineLists.size());
                }
            }

            @Override
            public void onFailed(int i, String s) {
               /* if()
                try {
                    getZoneTerminal(    ii, zoneMachineList );
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
        });
        return zoneMachineLists;
    }


    public synchronized void getMachineListFromServer(final int i, final ArrayList<MachineInfo> chooseMachineList, final ButtonBox terminal) throws IOException {

        String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getGroupTerminal + "/" + i;
        RequestManger.getInstance().get(url, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                final MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (responseData.getData().get(0).getName() != null) {
                                if(i==0){
                                    for (int j = 0; j <  allMachineList.size(); j++) {
                                        chooseMachineList.add(allMachineList.get(j));
                                    }
                                }else {
                                    for (int j = 0; j < responseData.getData().size(); j++) {
                                        chooseMachineList.add(responseData.getData().get(j));
                                    }
                                }
                            }
                            terminal.setButtonText("已选" + chooseMachineList.size() + "台");
                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //提交分区终端
    public void postZoneTerminal(final String zoneid, String terminalid) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("id", zoneid);
        map.put("terminalid", terminalid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postGroupTerminal);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "绑定终端分区成功" + zoneMachineLists.get(currentposition).getId());
                currentposition++;
                if (currentposition < zoneMachineLists.size()) {
                    try {
                        postZoneTerminal(zoneid + "", zoneMachineLists.get(currentposition).getId() + "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ((Activity) mContext).finish();
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //提交 分区
    public void postZone(final ZoneModel model, final ArrayList<MachineInfo> zoneMachineList) throws IOException {
        this.zoneMachineLists = zoneMachineList;
        //HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        HashMap<String, String> map = new HashMap<>();
        map.put("id", model.getId() + "");
        map.put("zonename", model.getName());
        map.put("description", model.getDescription());
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postZone);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "提交分区成功");
                currentposition = 0;
                final TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    int zoneids = responseData.getData().get(0).getId();
                    LogUtils.setLog(mTag, "提交分区成功"+zoneids);
                    model.setId(zoneids);
                    try {
                        postZoneTerminal(model.getId() + "", zoneMachineList.get(currentposition).getId() + "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //删除分区
    public synchronized void deleteZone(int zoneid, final boolean p, final ZoneModel model, final ArrayList<MachineInfo> s) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("id", "" + zoneid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.deleteZone);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().deleteHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "删除终端分区成功"+p);
             if(p) {
                 LogUtils.setLog(mTag, "删除终端分区成功shangchuan");
                 try {
                     model.setId(0);
                     postZone(model ,s);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }
}
