package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.ZoneMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TimeUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.TerminalPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.Editbox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.TitleLayout;


import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.functions.Consumer;

public class AddZoneActivity extends BaseActivity {
    private AddZoneActivity mContext;
    private ButtonBox terminal;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<MachineInfo>();
    private TitleLayout titleView;
    private Editbox etZoneName,descraption;
    private Bundle bundle;
    private String mTag = "AddZoneActivity";
    private ZoneModel model;
    private ZoneMethod zoneMethod;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_zone;
    }

    @Override
    protected void initSubViews() {
        mContext = this;

        initModel();
        zoneMethod = new ZoneMethod(mContext);
        titleView = (TitleLayout) findViewById(R.id.title_layout);
        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                finish();
            }
        });
        //设置分区名称
        etZoneName = (Editbox) findViewById(R.id.zonename);
        etZoneName.setTextName(ChinaConstants.zonename);

        //设置分区描述
        descraption = (Editbox) findViewById(R.id.descraption);
        descraption.setTextName(ChinaConstants.zonedescribe);
        //设置头
        if (bundle == null) {
            titleView.settitle(ChinaConstants.add_zone);
        } else {
            titleView.settitle(ChinaConstants.modify_zone);
        }
        //设置终端列表
        terminal = (ButtonBox) findViewById(R.id.terminal);
        terminal.setName(ChinaConstants.terminallist);
        terminal.setButtonTextHint(mContext.getResources().getString(R.string.pleasegetmachine));
        terminal.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TerminalPop terminalPop = new TerminalPop(mContext, chooseMachineList, new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        terminal.setButtonText("已选" + s + "台");
                    }
                });
            }
        });
        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                model.setName(etZoneName.getTaskname());
                model.setDescription(descraption.getTaskname());
                 if(bundle==null){

                     postZone();
                 }else{
                     putZone();
                 }
            }
        });
        initViewWithmodel();

    }



    private void initViewWithmodel() {
        etZoneName.setEdit(model.getName());
        descraption.setEdit(model.getDescription());
         LogUtils.setLog(mTag,"分区ID"+model.getId());
         if(bundle!=null) {
             try {
                 zoneMethod.getMachineListFromServer(model.getId(), chooseMachineList, terminal);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
    }

    private void putZone() {
        try {
            zoneMethod.deleteZone(model.getId(),true,model,chooseMachineList);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void postZone() {
        if (chooseMachineList.size() > 0) {
            try {
                zoneMethod.postZone(model, chooseMachineList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            showToast("请先选择终端");
        }
    }

    private void initModel() {
        Intent intent = getIntent();
        LogUtils.setLog(mTag,"intent 是否为空"+intent);
        bundle = intent.getExtras();
        if (bundle == null) {
            LogUtils.setLog(mTag, "新添加的model" + TimeUtils.getTime());
            model = new ZoneModel("sd",12,2,2,0,TimeUtils.getTime()," "," ");
        } else {
            LogUtils.setLog(mTag, "旧model填充");
            model = (ZoneModel) bundle.getSerializable(CacheConstants.ZONE_MODEL);
            LogUtils.setLog(mTag, "旧model填充  "+model.getName());

        }
    }




}
