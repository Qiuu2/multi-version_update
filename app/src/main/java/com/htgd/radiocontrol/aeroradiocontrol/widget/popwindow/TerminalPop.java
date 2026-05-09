package com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant.allMachineList;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.CheckBoxAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.CheckBoxMachineAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.ZoneAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2020/12/24:16:54
 * 邮箱：535708929
 * 说明：选终端的popwindow,从底部弹出
 */
public class TerminalPop implements View.OnClickListener {

    private Context mContext;

    private ArrayList<String> ulist = new ArrayList<>();
    private ArrayList<ZoneModel> zonelist = new ArrayList<>();
    private ArrayList<MachineInfo> zoneMachineList = new ArrayList<>();//
    private ZoneAdapter zoneAdapter;
    private String mTag = "TerminalPop";
    private ListView machineList;
    private PopupWindow popWindow;
    private CheckBoxMachineAdapter checkBoxAdapter;
    private HashMap<Integer, Boolean> pmaps = new HashMap<>();
    private ArrayList<MachineInfo> chooseMachineList;
    private ArrayList<MachineInfo> allMachineLists = new ArrayList<MachineInfo>();
    private Consumer<String> consumer;
    private ListView zoneListView;

    public TerminalPop(Context context, ArrayList<MachineInfo> chooseMachineList, Consumer<String> c) {
        this.mContext = context;
        this.chooseMachineList = chooseMachineList;
        showPopWindow();
        this.consumer = c;
        getData();
    }

    private void getData() {
        try {
            getZone();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.popwindow_terminal_select, null);
        popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        popWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        popWindow.setAnimationStyle(R.style.mypopwindow_anim_style);
        initView(view);
        initFourButton(view);
    }

    private void initView(View view) {
        zoneListView = (ListView) view.findViewById(R.id.zone_list);
        zoneAdapter = new ZoneAdapter(mContext, zonelist);
        zoneListView.setAdapter(zoneAdapter);
        machineList = (ListView) view.findViewById(R.id.terminal_list);
        checkBoxAdapter = new CheckBoxMachineAdapter(mContext, pmaps, allMachineLists, chooseMachineList);
        machineList.setAdapter(checkBoxAdapter);
        LogUtils.setLog(mTag, "设置监听");
        zoneAdapter.setListener(new ZoneAdapter.GetZoneTerminalListener() {
            @Override
            public void getZoneTerminals(int i) {
                LogUtils.setLog(mTag, "监听触发" + i);
                try {
                    getZoneTerminal(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //  checkBoxAdapter.notifyDataSetChanged();
            }
        });

        checkBoxAdapter.setListener(new  CheckBoxMachineAdapter.AddNumListener() {
            @Override
            public void addNum() {
                LogUtils.setLog(mTag, "选择了终端");
            }
        });
    }

    //初始化四个按钮
    private void initFourButton(View view) {
        TextView start = (TextView) view.findViewById(R.id.start);
        start.setOnClickListener(this);
        start.setText(mContext.getResources().getString(R.string.confirm));
        TextView all = (TextView) view.findViewById(R.id.all);
        all.setOnClickListener(this);
        TextView cancle = (TextView) view.findViewById(R.id.cancle);
        cancle.setOnClickListener(this);
        cancle.setText(mContext.getResources().getString(R.string.selectnull));
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
                    if(i==0){
                        LogUtils.setLog(mTag,"全部分区的终端"+allMachineList.size());
                        for (int j = 0; j < allMachineList.size(); j++) {
                            zoneMachineList.add(allMachineList.get(j));
                        }
                    }else{
                        for (int j = 0; j < list.size(); j++) {
                            if (list.get(j).getType() != 41 && list.get(j).getType() != 17) {
                                zoneMachineList.add(list.get(j));
                            }
                        }
                    }
                    setMap();
                }
                updateUI();
            }

            @Override
            public void onFailed(int i, String s) {
            }
        });
    }

    private void setMap() {//复原已选
       
        for (int i = 0; i < zoneMachineList.size(); i++) {
            pmaps.put(i, false);//获取
            for (int j = 0; j < chooseMachineList.size(); j++) {
                if (chooseMachineList.get(j).getId()==zoneMachineList.get(i).getId()) {//复原已选终端
                    pmaps.put(i, true);
                    LogUtils.setLog(mTag, zoneMachineList.get(i).getId()+"");
                }
            }
        }
    }

    private void selectNull() {
        for (Integer key : pmaps.keySet()) {
            LogUtils.setLog(mTag, zoneMachineList.get(key).getId()+"" + pmaps.get(key));
            if (pmaps.get(key) == true) {
                for (int i = 0; i < chooseMachineList.size(); i++) {
                    if (chooseMachineList.get(i).getId()==zoneMachineList.get(key).getId()) {
                        chooseMachineList.remove(i);
                    }
                }
            }
            pmaps.put(key, false);
        }
        updateUI();
    }


    private void clearAllList() {
        pmaps.clear();
        zoneMachineList.clear();
        chooseMachineList.clear();
    }

    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtils.setLog(mTag, "刷新了已选界面" + chooseMachineList.size());
                checkBoxAdapter.setList(zoneMachineList);
                checkBoxAdapter.setIsSelected(pmaps);
                checkBoxAdapter.notifyDataSetChanged();
            }
        });
    }

    //初始化时获取分区数组
    private void getZone() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.SearchZone, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                ZoneRsp responseData = JsonUtil.getInstance().deSerializeString(response, ZoneRsp.class);
                ulist.clear();
                zonelist.clear();
                zonelist.add(new ZoneModel("0", 0, 0, 0, 0, "sd", "全部终端", "sds"));
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    if(responseData.getData().get(0).getName()!=null) {
                        for (int i = 0; i < responseData.getData().size(); i++) {
                            ulist.add(responseData.getData().get(i).getId() + "");
                            zonelist.add(responseData.getData().get(i));
                        }
                    }
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.setLog(mTag, "刷新列表"+zonelist.size());
                            zoneAdapter.notifyDataSetChanged();
                            zoneListView.deferNotifyDataSetChanged();
                            try {
                                getZoneTerminal(zonelist.get(0).getId());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {


            }
        });
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
                Observable.just(chooseMachineList.size() + "").subscribe(consumer);
                break;
            case R.id.all:
                selectAll();
                break;
            case R.id.cancle:
                selectNull();
                break;


        }
    }
}
