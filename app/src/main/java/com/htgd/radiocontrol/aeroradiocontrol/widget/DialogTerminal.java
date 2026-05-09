package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;


import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GridViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ArryListUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



/**
 * Created by wzw on 2017/12/13.
 */

public class DialogTerminal extends Dialog implements View.OnClickListener {


    private final Context mContext;
    private final onViewClickListener listener;
    private final TempTTSModel model;
    private final Handler handler;
    private   ArrayList<MachineInfo> chooseMachineList;
    private View dialogView;

    private ArrayList<MachineInfo> signMachineList;
    private CheckBox checkAll;
    private GridView machineList;
    private GridViewAdapter gridViewAdapter;
    private Dialog dialog;
    private Button confirm_bt;
    private ArrayList<MachineInfo> tempMachineList;
    private String[] choose;

    public DialogTerminal(Context context, ArrayList<MachineInfo> signMachineList , TempTTSModel model, Handler handler, onViewClickListener listener) {
        super(context);
        this.mContext = context;
        this.listener = listener;
        this.signMachineList = signMachineList;

        this.model=model;
        this.handler=handler;
        initView();
    }

    private void initView() {
        dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_terminal, null);
        confirm_bt = (Button) dialogView.findViewById(R.id.confirm_bt);
        confirm_bt.setOnClickListener(this);
        dialog = new Dialog(mContext);
        dialog.setContentView(dialogView);
        dialog.setTitle("终端设置");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);
        machineList = (GridView) dialogView.findViewById(R.id.gridview_oneselect);
        checkAll = (CheckBox) dialogView.findViewById(R.id.check_all);
        checkAll.setOnClickListener(this);

        try {
            getMachineList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gridViewAdapter = new GridViewAdapter(mContext, signMachineList);
        machineList.setAdapter(gridViewAdapter);
        //复现已选终端
        if(model.getTerminal()!="") { //model里的字符串分割出终端

            choose= model.getTerminal().split(",");
            LogUtils.setLog("数组长度"+choose.length+"");
            List<String> list = new ArrayList<String>();
            for (int i=0; i<choose.length; i++) {
                list.add(choose[i]);
            }
            list.remove(list.size()-1);
            list.remove(0);
            for (int i = 0; i < signMachineList.size(); i++) {
                for (int j=0;j<list.size();j++) {
                    if (signMachineList.get(i).getId() ==Integer.parseInt(list.get(j))){
                        signMachineList.get(i).setChoose(true);
                        LogUtils.setLog("第几个"+i+"jj"+j);
                    }
                }
            }
            gridViewAdapter.refrech(signMachineList);
        }
        machineList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (signMachineList.get(position).isChoose()) {
                    signMachineList.get(position).setChoose(false);
                } else {
                    signMachineList.get(position).setChoose(true);

                }
                gridViewAdapter.refrech(signMachineList);
            }
        });



        LogUtils.setLog( "mmpqqq"+ PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext));

    }


    // 获取设备信息
    private void getMachineList() throws IOException {
        RequestManger.getInstance().get(PreferencesUtil.getInstance().getField("serverAddress",mContext)+Constant.getMahcinelistAll, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog("get sucess code is" + code + "message is" + response);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    signMachineList = responseData.getData();
                    signMachineList = ArryListUtils.getInStance().cutTheSameMachine(signMachineList, mContext);
                    signMachineList=ArryListUtils.getInStance().getTheOnNetMachine(signMachineList,mContext);
                    for (int i=0;i<signMachineList.size();i++) {

                        LogUtils.setLog("终端名称"+signMachineList.get(i).getName());
                        if (signMachineList.get(i).getName() == PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext)) {
                            signMachineList.get(i).setChoose(true);

                        };
                    }
                    gridViewAdapter.setMachineArrayList(signMachineList);
                   Message msg=handler.obtainMessage(1,signMachineList);
                    handler.sendMessage(msg);

                    updateUI();
                } else {
                    LogUtils.setLog("");
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog("get failed code is" + code + "message is" + message);
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    reTry(1);
                }
            }
        });
    }


    public void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了

                gridViewAdapter.notifyDataSetChanged();

            }
        });
    }



    private void reTry(final int type) {
        try {
            getMachineList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm_bt://确定按钮
                if (listener != null) {
                    listener.onConfirmClick(v);
                }
                if (dialog != null) {
                    dialog.cancel();
                }
                break;
            case  R.id.check_all:
                if(checkAll.isChecked()){
                    for (int i=0;i<signMachineList.size();i++){
                        signMachineList.get(i).setChoose(true);
                    }
                    gridViewAdapter.refrech(signMachineList);
                }else{
                    for (int i=0;i<signMachineList.size();i++){
                        signMachineList.get(i).setChoose(false);
                    }
                    gridViewAdapter.refrech(signMachineList);
                }

                break;

        }
    }

    /***
     * 显示对话框
     */
    public void show() {

        if (dialog != null) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            if (!((Activity) mContext).isFinishing() && !dialog.isShowing()) {
                dialog.show();
            }
        }

    }
    public interface onViewClickListener {
        /***
         * 响应确定按钮
         *
         * @param v
         */
        public void onConfirmClick(View v);


        /***
         * 对话框消失回调
         */
        public void dialogDismiss();

    }
    /**
     * 设置框不能消失
     */
    public void setDialogUnableDismiss() {
        dialog.setCancelable(true);
    }



}
