package com.htgd.radiocontrol.aeroradiocontrol.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.htapplib.HTIntf;
import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.CallOtherActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseFragment;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusWaitingForAnswer;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ArryListUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by wzq on 2017-07-12.
 */
public class FragmentDuiJiang extends BaseFragment {
    private Context mContext;
    private ArrayList<MachineInfo> machineList = new ArrayList<>();
    private AdapterView.OnItemClickListener listener;
    private Timer timer;
    private String mTag = "FragmentDuiJiang";
    private TimerTask timerTask;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private CommonAdapter<MachineInfo> mAdapter;
    private LRecyclerViewAdapter LAdapter;
    private ArrayList<MachineInfo> chooseMachine = new ArrayList<MachineInfo>();
    private HashMap<String, Boolean> map = new HashMap<>();
    public ImageView IvUser;

    public static FragmentDuiJiang newInstance(String text, Context mContext) {
        FragmentDuiJiang fragmentDuiJiang = new FragmentDuiJiang();
        Bundle bundle = new Bundle();
        fragmentDuiJiang.setArguments(bundle);
        return fragmentDuiJiang;
    }

    public FragmentDuiJiang() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         mContext=this.getActivity();
        PreferencesUtil.getInstance().getField(Constring.serverAddress, this.getContext());
        View v = inflater.inflate(R.layout.common_list, null);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        initListview(v);
        showMachine();
        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        LogUtils.setLog(mTag, "setUserVisibleHint"+isVisibleToUser);
        if (!isVisibleToUser) {//隐藏时
            LogUtils.setLog(mTag, "setUserVisibleHint");
            /*for (int i = 0; i < machineList.size(); i++) {
                map.put(machineList.get(i).getName(), false);
                machineList.get(i).setChoose(false);
            }
            updateUI();*/
            stopRefreshMachine();
        } else {
            stopRefreshMachine();
            refreshMachineStateInTime();

        }
    }
    private void showMachine() {
        for (int i = 0; i < machineList.size(); i++) {
            map.put(machineList.get(i).getName(), false);
            machineList.get(i).setChoose(false);
        }
        stopRefreshMachine();
        LogUtils.setLog(mTag, "oncreat KAISHISHUAXIN");
        refreshMachineStateInTime();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        stopRefreshMachine();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtils.setLog(mTag,"onDestroyView");
        for (int i = 0; i < machineList.size(); i++) {
            map.put(machineList.get(i).getName(), false);
            machineList.get(i).setChoose(false);
        }
        updateUI();
        stopRefreshMachine();
    }
    @Override
    public void onPause() {
        super.onPause();
        stopRefreshMachine();
    }
    //定时刷新终端列表
    public void refreshMachineStateInTime() {
        LogUtils.setLog(mTag, "KAISHISHUAXIN");
        if (timer == null) {
            timer = new Timer();
        }
        if (timerTask == null) {
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
            timer.schedule(timerTask, 2000, IntConstans.RefreshPeriodTimeInCall);//
        }


    }
    // 刷新设备信息
    private synchronized void refreshMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelist + IntConstans.FLAG_DUIJIANG, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag + "refreshMachineList get sucess code is" + code + "message is" + response);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    lists = ArryListUtils.getInStance().cutTheSameMachine(lists, mContext);
                    machineList.clear();
                    if(lists.size()>0) {
                        for (int i = 0; i < lists.size(); i++) {
                            if(lists.get(i).getType()!=41&&lists.get(i).getType()!=17) {//去除手机和应急终端
                                machineList.add(lists.get(i));
                            }
                        }
                        resetData();
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });
                    } else {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju"+machineList.size());
                                list.refreshComplete(machineList.size());//停止刷新
                                list.setEmptyView(empty);
                            }
                        });

                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag + "get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });

    }
    public void stopRefreshMachine() {
        LogUtils.setLog(mTag, "TINGZHISHUAXIN");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void renderView() {
        mAdapter = new CommonAdapter<MachineInfo>(mContext, R.layout.item_tolk_machine, machineList) {
            @Override
            protected void convert(ViewHolder viewHolder, final MachineInfo s, final int i) {
                if(s!=null) {
                    viewHolder.setText(R.id.item_name_text, s.getName());//昵称
                    TextView name = (TextView) viewHolder.getView(R.id.item_name_text);
                    IvUser = (ImageView) viewHolder.getView(R.id.item_big_image);
                    getState(machineList.get(i - 1).getTaskstate() + "");
                    viewHolder.setOnClickListener(R.id.right_bt_duijiang, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (machineList.get(i - 1).getTaskstate() == 0) {
                                chooseMachine.add(machineList.get(i - 1));
                                MainMethod.startSpeech(chooseMachine);
                                updateUI();
                                chooseMachine.clear();
                            } else {
                                showMsg(ChinaConstants.terminalbusy);
                            }
                        }
                    });
                }else{
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };

    }

    /**
     * 获取状态值对应的文字
     *
     * @param status 状态值
     * @return 对应的文字
     */
    private String getState(String status) {
        if (!TextUtils.isEmpty(status)) {
            String tmp = "";
            switch (status) {
                case "-1":
                    tmp = "没有初始化";
                    break;
                case "0":
                    tmp = "准备就绪";
                    break;
                case "1":
                    tmp = "定时播放";
                    IvUser.setBackgroundResource(R.mipmap.busyline);

                    break;
                case "2":
                    tmp = "正在对讲";
                    IvUser.setBackgroundResource(R.mipmap.busyline);

                    break;
                case "3":
                    tmp = "点播";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "4":
                    tmp = "选播";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "5":
                    tmp = "快捷寻呼";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "6":
                    tmp = "寻呼";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "7":
                    tmp = "本地扩音";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "8":
                    tmp = "USB播放";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "9":
                    tmp = "请求对讲";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "10":
                    tmp = "被请求对讲";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "11":
                    tmp = "播放寻呼";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "12":
                    tmp = "报警";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "13":
                    tmp = "采播";
                    IvUser.setBackgroundResource(R.mipmap.busyline);
                    break;
                case "14":
                    tmp = "设备挂起";

                    break;
                default:
                    break;
            }
            return tmp;
        }
        return "";
    }


    private void initListview(View v) {
        list = new MyLRecycView(getActivity(), R.color.transparent);
        content = (LinearLayout) v.findViewById(R.id.content);
        empty = (LinearLayout) v.findViewById(R.id.empty);
        list.setLayoutManager(new LinearLayoutManager(mContext,   LinearLayoutManager.VERTICAL, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 20, 0, 0);
        list.setLayoutParams(lp);
        renderView();
        LAdapter = new LRecyclerViewAdapter(mAdapter);
        LAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (machineList.get(position).getTaskstate() == 0) {
                    chooseMachine.add(machineList.get(position));
                    //MainMethod.startSpeech(chooseMachine);
                    updateUI();
                    chooseMachine.clear();
                } else {
                    showMsg(ChinaConstants.terminalbusy);
                }
            }
        });
        list.setAdapter(LAdapter);
        list.setEmptyView(empty);
        list.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    getMachineList();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            getMachineList();//首页
        } catch (IOException e) {
            e.printStackTrace();
        }
        //网络错误的时候调用
        list.setOnNetWorkErrorListener(new OnNetWorkErrorListener() {
            @Override
            public void reload() {
                try {
                    getMachineList();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        //滑动时停止刷新
        list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        LogUtils.setLog(mTag,"按下了");
                        stopRefreshMachine();
                        break;
                    case MotionEvent.ACTION_UP:
                        LogUtils.setLog(mTag,"拿起了");
                        refreshMachineStateInTime();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public void onResume() {
        super.onResume();
        mContext = this.getActivity();
        /*try {
            getMachineList();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        stopRefreshMachine();

    }

    private void getMachineList() throws IOException {
         RequestManger.getInstance().get(Constant.serveraddress  + Constant.getMahcinelist + Constant.FLAG_DUIJIANG, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag + "get sucess code is" + code + "message is" + response);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    lists = ArryListUtils.getInStance().cutTheSameMachine(lists, mContext);
                    machineList.clear();
                    if (lists.size() > 0) {
                        for (int i = 0; i < lists.size(); i++) {
                            machineList.add(lists.get(i));
                        }
                        resetData();


                    }
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            content.removeViewAt(0);
                            content.addView(list, 0);
                            list.setAdapter(LAdapter);
                            updateUI();
                            if(machineList.size()==0) {
                                LogUtils.setLog(mTag,"没有终端");
                                list.setEmptyView(empty);
                            }
                        }
                    });

                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog("get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }

    public void resetData() {
        if (machineList.size() > 0) {
            if (machineList.get(0).getName() != null) {
                if (machineList.size() > 0 && machineList.get(0).getName() != null) {
                    for (int i = 0; i < machineList.size(); i++) { //去除自己
                        if (machineList.get(i).getName().equals(PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext))) {
                            machineList.remove(i);
                        }
                    }
                }

                //userAdapter.setMachineArrayList(machineList);
                //服务器获取的都是未选中的，
                if (machineList.size() > 0 && map.size() > 0) {
                    for (int i = 0; i < machineList.size(); i++) {
                        for (String key : map.keySet()) {
                            if (machineList.get(i).getName().equals(key)) {
                                machineList.get(i).setChoose(map.get(machineList.get(i).getName()));
                            }
                        }
                    }
                }
            } else {
                machineList.clear();
            }
        }
    }

    public void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了

                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(machineList.size());
            }
        });
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getMachineList();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed() {

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


   @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent2(EventBusWaitingForAnswer event) {
        LogUtils.setLog(mTag, "the event is WaitingForAnswer");
        Intent intent=new Intent(mContext, CallOtherActivity.class);
        startActivity(intent);
    }




}
