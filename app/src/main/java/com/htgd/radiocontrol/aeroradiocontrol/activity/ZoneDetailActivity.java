package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GVMachineAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.TitleLayout;

import java.io.IOException;
import java.util.ArrayList;

import radiocontrol.htgd.com.aeroradiocontrol.widget.MyGridView;

public class ZoneDetailActivity extends BaseActivity {
    private ZoneDetailActivity mContext;
    private ButtonBox zonename, descraption, creattime;
    private ZoneModel model;
    private String mTag = "ZoneDetailActivity";
    private GVMachineAdapter machineAdapter;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<>();
    private MyGridView machineList;
    private Bundle bundle;
    private TitleLayout titleView;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_zone_detail;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        Intent intent = getIntent();
        LogUtils.setLog(mTag, "intent 是否为空" + intent);
        bundle = intent.getExtras();
        LogUtils.setLog(mTag, "bundle 是否为空" + bundle);
        model = (ZoneModel) bundle.getSerializable(CacheConstants.ZONE_MODEL);
        titleView = (TitleLayout) findViewById(R.id.title_layout);
        titleView.settitle(ChinaConstants.zonedetail);
        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                finish();
            }
        });
        //名称
        zonename = (ButtonBox) findViewById(R.id.zonename);
        zonename.setName(ChinaConstants.zonename);
        zonename.setButtonText(model.getName());

        zonename.setBtnShowOrHide(false);
        //描述
        descraption = (ButtonBox) findViewById(R.id.descraption);
        descraption.setName(ChinaConstants.zonedescribe);
        descraption.setButtonText(model.getDescription());

        descraption.setBtnShowOrHide(false);
        //创建时间
        creattime = (ButtonBox) findViewById(R.id.creattime);
        creattime.setName(ChinaConstants.creattime);
        creattime.setButtonText(model.getDatetime());

        creattime.setBtnShowOrHide(false);
        machineList = (MyGridView) findViewById(R.id.machine_list);
        machineAdapter = new GVMachineAdapter(mContext, chooseMachineList);
        machineList.setAdapter(machineAdapter);
        try {
            getMachineListFromServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void getMachineListFromServer() throws IOException {
        if (model != null) {
            String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getGroupTerminal + "/" + model.getId();
            RequestManger.getInstance().get(url, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                    if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                        if(model.getId()==0){
                            chooseMachineList = VariableConstant.allMachineList;
                        }else {
                            chooseMachineList = responseData.getData();
                        }
                        machineAdapter.setMachineInfos(chooseMachineList);
                        if (chooseMachineList.size() >= 1) {
                            updateMachineUI();
                        }
                    } else {
                        LogUtils.setLog(mTag, "that data is eorro");
                    }
                }

                @Override
                public void onFailed(int code, String message) {

                }
            });
        } else {
            LogUtils.setLog("the taskGuangboModel or taskId is null");
        }
    }

    private void updateMachineUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                machineAdapter.notifyDataSetChanged();
            }
        });
    }
}
