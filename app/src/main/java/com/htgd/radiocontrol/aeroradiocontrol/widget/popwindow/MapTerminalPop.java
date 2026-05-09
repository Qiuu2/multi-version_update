package com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.functions.Consumer;

public class MapTerminalPop implements View.OnClickListener {


    private final Context mContext;
    private final MachineInfo chooseMachine;
    private final MachineInfo tempmachine;

    private PopupWindow popWindow;
    private TextView name;
    private TextView latitude;
    private TextView longtitude;
    private TextView call;
    private TextView ip, devicestate, taskstate;
    private TextView edit_latlng;
    private String mTag="MapTerminalPop";

    public MapTerminalPop(Context context, MachineInfo chooseMachine, MachineInfo tempmachine ) {
        this.mContext = context;
        this.chooseMachine = chooseMachine;
        this.tempmachine = tempmachine;
        showPopWindow();

        getData();
    }

    private void showPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.popwindow_terminal_onmap, null);
        popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        popWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        popWindow.setAnimationStyle(R.style.mypopwindow_anim_style);
        initView(view);
        // initFourButton(view);
    }

    private void initView(View view) {
        call = (TextView) view.findViewById(R.id.call);
        call.setOnClickListener(this);
        edit_latlng = (TextView) view.findViewById(R.id.edit_lat);
        edit_latlng.setOnClickListener(this);
        if (tempmachine.getName().length() != 0) {
            edit_latlng.setBackgroundResource(R.drawable.login_bt_style);
        } else {
            edit_latlng.setBackgroundResource(R.drawable.login_bt_style);//不可编辑
            edit_latlng.setClickable(false);
        }
        name = (TextView) view.findViewById(R.id.name);
        latitude = (TextView) view.findViewById(R.id.latitude);
        longtitude = (TextView) view.findViewById(R.id.longtitude);
        ip = (TextView) view.findViewById(R.id.ip);

        taskstate = (TextView) view.findViewById(R.id.taskstate);
        name.setText(chooseMachine.getName());
        latitude.setText(chooseMachine.getLatitude());
        longtitude.setText(chooseMachine.getLongitude());
        ip.setText(chooseMachine.getIp());
        if (chooseMachine.getDevicestate() == 0 && chooseMachine.getNetstate() == 1) {
            if (chooseMachine.getTaskstate() == 0) {
                taskstate.setText("空闲");
            } else {
                taskstate.setText("忙碌");
            }
        } else {
            taskstate.setText("离线");
        }

    }

    private void getData() {

    }

    //提交任务
    public void savegitude(final MachineInfo chooseMachine) throws IOException {
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(chooseMachine);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.saveMahcineLatitude);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {

            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.call:
                ArrayList chooseMachineList = new ArrayList<MachineInfo>();
                chooseMachineList.add(chooseMachine);
                MainMethod.startCall(chooseMachineList);
                break;
            case R.id.edit_lat:

                if (tempmachine.getLatitude().length() != 0) {
                    chooseMachine.setLongitude(tempmachine.getLongitude().substring(0,tempmachine.getLongitude().indexOf(".")+7));
                    chooseMachine.setLatitude(tempmachine.getLatitude().substring(0,tempmachine.getLatitude().indexOf(".")+7));
                    chooseMachine.setName(tempmachine.getName());
                    LogUtils.setLog(mTag,"设置的经纬度"+chooseMachine.getLatitude()+chooseMachine.getLongitude());
                    try {
                        savegitude(chooseMachine);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
