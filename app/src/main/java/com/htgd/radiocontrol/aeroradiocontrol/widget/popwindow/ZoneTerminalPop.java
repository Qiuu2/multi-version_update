package com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant.allMachineList;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.CheckBoxMachineAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.MusicPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.TipDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ZoneTerminalPop implements View.OnClickListener {
    private Context mContext;
    private PopupWindow popWindow;
    private String mTag = "ZonePop";
    private ArrayList<MachineInfo> zoneMachineList = new ArrayList<>();
    private CheckBoxMachineAdapter checkBoxAdapter;
    private HashMap<Integer, Boolean> pmaps = new HashMap<>();
    private ArrayList<MachineInfo> chooseMachineList=new ArrayList<>();
    private ListView machineList;
    private ArrayList<MachineInfo> allMachineLists = new ArrayList<MachineInfo>();

    public ZoneTerminalPop(Context context, ZoneModel zonemodel ) {
        this.mContext = context;
        showPopWindow();
        try {
            getZoneTerminal(zonemodel.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void showPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.popwindow_terminal_select, null);
        popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new BitmapDrawable());//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        popWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        popWindow.setAnimationStyle(R.style.mypopwindow_anim_style);
        LinearLayout zonenamelist = (LinearLayout) view.findViewById(R.id.zonenamelist);
        machineList = (ListView) view.findViewById(R.id.terminal_list);
        checkBoxAdapter = new CheckBoxMachineAdapter(mContext, pmaps, allMachineLists, chooseMachineList);
        machineList.setAdapter(checkBoxAdapter);
        checkBoxAdapter.setListener(new CheckBoxMachineAdapter.AddNumListener() {
            @Override
            public void addNum() {
                LogUtils.setLog(mTag, "选择了终端");
            }
        });
        zonenamelist.setVisibility(View.GONE);
        initFourButton(view);
    }
    //初始化四个按钮
    private void initFourButton(View view) {

        TextView start = (TextView) view.findViewById(R.id.start);
        start.setOnClickListener(this);
        start.setText(mContext.getResources().getString(R.string.call));
        TextView all = (TextView) view.findViewById(R.id.all);
        all.setOnClickListener(this);
        all.setText(mContext.getResources().getString(R.string.play));
        TextView cancle = (TextView) view.findViewById(R.id.cancle);
        cancle.setOnClickListener(this);
        cancle.setText(mContext.getResources().getString(R.string.selectall));
        TextView has = (TextView) view.findViewById(R.id.has);
        has.setOnClickListener(this);
        has.setText(mContext.getResources().getString(R.string.cancel));
        TextView selected_num = (TextView) view.findViewById(R.id.select_num);
        selected_num.setVisibility(View.INVISIBLE);


    }
    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < zoneMachineList.size(); i++) {
                    LogUtils.setLog(mTag,"刷新"+zoneMachineList.get(i).getName());
                }
                LogUtils.setLog(mTag, zoneMachineList.size()+"刷新了已选界面" + chooseMachineList.size());
                checkBoxAdapter.setList(zoneMachineList);
                checkBoxAdapter.setIsSelected(pmaps);
                checkBoxAdapter.notifyDataSetChanged();
            }
        });
    }
    private void setMap() {//复原已选

        for (int i = 0; i < zoneMachineList.size(); i++) {
            pmaps.put(i, false);//获取
            for (int j = 0; j < chooseMachineList.size(); j++) {
                if (chooseMachineList.get(j).getId() == zoneMachineList.get(i).getId()) {//复原已选终端
                    pmaps.put(i, true);
                    LogUtils.setLog(mTag, zoneMachineList.get(i).getId() + "");
                }
            }
        }
    }
    public void getZoneTerminal(final int i) throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getGroupTerminal + "/" + i, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                zoneMachineList.clear();
                pmaps.clear();
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> list = responseData.getData();
                    if (i == 0) {
                        for (int j = 0; j < allMachineList.size(); j++) {
                            zoneMachineList.add(allMachineList.get(j));
                        }
                    } else {
                        for (int j = 0; j < list.size(); j++) {
                            list.get(j).setGroupid(i);//给设备添加groupid
                            if (list.get(j).getType() != 41 && list.get(j).getType() != 17) {
                                zoneMachineList.add(list.get(j));
                            }
                        }
                    }
                    setMap();
                    if (zoneMachineList.size() > 0 && zoneMachineList.get(0).getId() != -1) {//筛选数据以免程序崩溃
                        LogUtils.setLog(mTag, zoneMachineList.size() + "终端个数及设备列表地址" + zoneMachineList.get(0).getId());

                    } else {
                        zoneMachineList.clear();
                    }

                }
                updateUI();
            }

            @Override
            public void onFailed(int i, String s) {
            }
        });
    }



    private void selectNull() {
        for (Integer key : pmaps.keySet()) {
            LogUtils.setLog(mTag, zoneMachineList.get(key).getId() + "" + pmaps.get(key));
            if (pmaps.get(key) == true) {
                for (int i = 0; i < chooseMachineList.size(); i++) {
                    if (chooseMachineList.get(i).getId() == zoneMachineList.get(key).getId()) {
                        chooseMachineList.remove(i);
                    }
                }
            }
            pmaps.put(key, false);
        }
        updateUI();
    }
    private void selectAll() {
        for (Integer key : pmaps.keySet()) {
            if (pmaps.get(key) == false) {
                chooseMachineList.add(zoneMachineList.get(key));
            }
            pmaps.put(key, true);
        }
        updateUI();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                popWindow.dismiss();

                for (int i = 0; i < chooseMachineList.size(); i++) {
                    VariableConstant.chooseMachine.add(chooseMachineList.get(i));
                }
                if (VariableConstant.chooseMachine.size() > 0) {
                    TipDialog tipCallDialog = new TipDialog(mContext, "确定寻呼吗", new TipDialog.OnViewClickListener() {
                        @Override
                        public void onConfirmClick(View v) {
                            MainMethod.startCall(chooseMachineList);

                        }

                        @Override
                        public void onCancelClick(View v) {

                        }
                    });
                    tipCallDialog.show();


                } else {
                    Toast.makeText(mContext,"请先选择终端",Toast.LENGTH_LONG);
                }
                break;
            case R.id.all:
                for (int i = 0; i < chooseMachineList.size(); i++) {
                    VariableConstant.chooseMachine.add(chooseMachineList.get(i));
                }
                if (chooseMachineList.size() > 0) {//在选音乐里面添加到静态变量里面
                    new MusicPop(mContext, "", new ArrayList<MusicInfoModel>(), chooseMachineList );
                } else {
                    Toast.makeText(mContext,"请先选择终端",Toast.LENGTH_LONG);
                }
                break;
            case R.id.cancle:
                 selectAll();
                break;
            case R.id.has:
                selectNull();
                break;

        }
    }
}
