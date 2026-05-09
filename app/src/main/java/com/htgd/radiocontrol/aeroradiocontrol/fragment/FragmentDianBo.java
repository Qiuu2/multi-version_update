package com.htgd.radiocontrol.aeroradiocontrol.fragment;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.ActivityMusicOrder;
import com.htgd.radiocontrol.aeroradiocontrol.activity.SelectMusicActivity;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GridViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseFragment;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ArryListUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.MusicPop;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.functions.Consumer;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans.RefreshPeriodTimeInCall;

/**
 * Created by wzq on 2017-07-12.
 */
public class FragmentDianBo extends BaseFragment implements View.OnClickListener{

    private Context mContext;
    private ArrayList<MachineInfo> signMachineList = new ArrayList<MachineInfo>();
    private ArrayList<MachineInfo> chooseMachine = new ArrayList<MachineInfo>();
    private String mTag = "FragmentPlayss";
    private TextView start, all, has, cancle;
    private HashMap<String, Boolean> map = new HashMap<>();
    public Timer timer = new Timer();
    private TimerTask timerTask;
    private CommonAdapter mAdapter;
    private LRecyclerViewAdapter LAdapter;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private RelativeLayout IvUser;
    private int s;


    //构造函数
    public static FragmentDianBo newInstance(String s,Context context) {
        FragmentDianBo fgDianBo = new FragmentDianBo();
        Bundle bundle = new Bundle();
        fgDianBo.setArguments(bundle);
        return fgDianBo;
    }

    public FragmentDianBo() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = this.getActivity();
        LogUtils.setLog(mTag, "onCreateView");
        View v = inflater.inflate(R.layout.new_fragment_call, null);


          s = 5;
        while (s > 2){
            LogUtils.setLog(mTag,"is的值" +s);
           test();
           s--;
        }
        initListview(v);
        showMachine();
        return v;
    }
    private synchronized void test(){
        int is=2;
        for (int i = 0; i < 3; i++) {
            LogUtils.setLog(mTag,"is的值"+i+s);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.setLog(mTag,"关闭fragment");
        stopRefreshMachine();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtils.setLog(mTag,"onDestroyView");
        for (int i = 0; i < signMachineList.size(); i++) {
            map.put(signMachineList.get(i).getName(), false);
            signMachineList.get(i).setChoose(false);
        }
        updateUI();
        stopRefreshMachine();
    }

    private void showMachine() {
        for (int i = 0; i < signMachineList.size(); i++) {
            map.put(signMachineList.get(i).getName(), false);
            signMachineList.get(i).setChoose(false);
        }
        stopRefreshMachine();
        LogUtils.setLog(mTag, "oncreat KAISHISHUAXIN");
        refreshMachineStateInTime();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        LogUtils.setLog(mTag, "setUserVisibleHint"+isVisibleToUser);
        if (!isVisibleToUser) {//隐藏时
            LogUtils.setLog(mTag, "setUserVisibleHint");
           /* for (int i = 0; i < signMachineList.size(); i++) {
                map.put(signMachineList.get(i).getName(), false);
                signMachineList.get(i).setChoose(false);
            }
            updateUI();*/
            stopRefreshMachine();
        } else {
            stopRefreshMachine();
            refreshMachineStateInTime();

        }
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
            timer.schedule(timerTask, 2000, RefreshPeriodTimeInCall);//
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
                                updateUI();
                            }
                        });
                    } else {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju"+signMachineList.size());


                                list.refreshComplete(signMachineList.size());//停止刷新
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
    @Override
    public void onPause() {
        super.onPause();
        stopRefreshMachine();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogUtils.setLog(mTag, "onViewCreated");
    }



    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        LogUtils.setLog(mTag, "onresume");
        for (int i = 0; i < signMachineList.size(); i++) {
            map.put(signMachineList.get(i).getName(), false);
            signMachineList.get(i).setChoose(false);
        }
        stopRefreshMachine();


    }

    private void initListview(View v) {
        //has = (TextView) v.findViewById(R.id.has);
        //has.setOnClickListener(this);
        View threeRv =(View) v.findViewById(R.id.threerv);
        initFourButton(threeRv);
        list = new MyLRecycView(getActivity(), R.color.transparent);
        content = (LinearLayout) v.findViewById(R.id.content);
        empty = (LinearLayout) v.findViewById(R.id.empty);
        list.setLayoutManager(new GridLayoutManager(mContext, 3, GridLayoutManager.VERTICAL, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 20, 0, 0);
        list.setLayoutParams(lp);
        renderView();
        LAdapter = new LRecyclerViewAdapter(mAdapter);
        LAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                map.put(signMachineList.get(position).getName(), !signMachineList.get(position).isChoose());

                signMachineList.get(position).setChoose(!signMachineList.get(position).isChoose());
                chooseMachine.add(signMachineList.get(position));
                updateUI();
                LAdapter.notifyDataSetChanged();
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


    //初始化四个按钮
    private void initFourButton(View v) {
        start = (TextView) v.findViewById(R.id.start);
        start.setOnClickListener(this);
        all =  (TextView) v.findViewById(R.id.all);
        all.setOnClickListener(this);
        cancle = (TextView) v.findViewById(R.id.cancle);
        cancle.setOnClickListener(this);
    }

    // 获取设备信息
    private synchronized void getMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelist + IntConstans.FLAG_DIANBO, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag + "get success code is" + code + "message is" + response);
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    lists = ArryListUtils.getInStance().cutTheSameMachine(lists, mContext);
                    signMachineList.clear();
                    if (lists.size() > 0) {
                        for (int i = 0; i < lists.size(); i++) {
                            if(lists.get(i).getType()!=41&&lists.get(i).getType()!=17) {//去除手机和应急终端
                                signMachineList.add(lists.get(i));
                            }
                        }
                        resetData();
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                content.removeViewAt(0);
                                content.addView(list, 0);
                                list.setAdapter(LAdapter);
                                updateUI();
                            }
                        });

                    } else {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju" + signMachineList.size());
                                content.removeViewAt(0);
                                content.addView(list, 0);

                                list.refreshComplete(signMachineList.size());//停止刷新
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
                if (signMachineList.size() > 0 && map.size() > 0) {
                    for (int i = 0; i < signMachineList.size(); i++) {
                        for (String key : map.keySet()) {
                            if (signMachineList.get(i).getName().equals(key)) {
                                signMachineList.get(i).setChoose(map.get(signMachineList.get(i).getName()));
                            }
                        }
                    }
                }
            } else {
                signMachineList.clear();
            }
        }
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new BaseFragment.updatelister() {
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


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                if (chooseMachine.size() > 0) {//在选音乐里面添加到静态变量里面
                    /*Intent intent = new Intent(mContext, ActivityMusicOrder.class);
                    intent.putExtra("choosemachine", chooseMachine);
                    ((Activity) mContext).startActivity(intent);*/
                    new MusicPop(mContext,"", new ArrayList<MusicInfoModel>(),chooseMachine, new Consumer() {
                        @Override
                        public void accept(Object o) throws Exception {
                          //  ArrayList<MusicInfoModel> a=(ArrayList<MusicInfoModel>)o;
                            LogUtils.setLog(mTag,"startplay");
                           // MainMethod.startplay(chooseMachine, a);
                        }
                    });
                } else {
                    showMsg("请先选择终端");
                }
                break;
            case R.id.all:
                selectAll();
                break;
            case R.id.cancle:
                selectNull();
                break;
            /*case R.id.has:
                showDialog();
                break;*/
        }
    }

    public void showDialog() {
       /* final ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < chooseMachine.size(); i++) {
            strings.add(chooseMachine.get(i).getName());
        }
        final ChoosedAdapter recyclerAdapter = new ChoosedAdapter(mContext, strings);
        DialogSelectedMachine dialogSelectedMachine = new DialogSelectedMachine(mContext, recyclerAdapter);
        dialogSelectedMachine.show();
        recyclerAdapter.setListener(new ChoosedAdapter.DeleteListener() {
            @Override
            public void delete(int i) {
                strings.remove(i);
                recyclerAdapter.notifyDataSetChanged();
                for (int j = 0; j < signMachineList.size(); j++) {
                    if (chooseMachine.get(i).getName() == signMachineList.get(j).getName()) {
                        setClickActions(j);
                        break;
                    }
                }

            }
        });*/
    }

    //设置设备的选中状态
    private void setClickActions(int position) {
        if (signMachineList.get(position).isChoose()) {
            signMachineList.get(position).setChoose(false);
            for (int i = 0; i < chooseMachine.size(); i++) {
                if (chooseMachine.get(i).getName() == signMachineList.get(position).getName())
                    chooseMachine.remove(i);
            }
        } else {
            signMachineList.get(position).setChoose(true);
            chooseMachine.add(signMachineList.get(position));
        }
        map.put(signMachineList.get(position).getName(), signMachineList.get(position).isChoose());
    }

    private void selectNull() {
        for (int i = 0; i < signMachineList.size(); i++) {
            signMachineList.get(i).setChoose(false);
            map.put(signMachineList.get(i).getName(), signMachineList.get(i).isChoose());
        }
        updateUI();
    }

    private void selectAll() {
        for (int i = 0; i < signMachineList.size(); i++) {
            signMachineList.get(i).setChoose(true);
            map.put(signMachineList.get(i).getName(), signMachineList.get(i).isChoose());
        }
        updateUI();
    }

    public void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
                LogUtils.setLog(mTag, "updateui" + signMachineList.size());
                chooseMachine.clear();
                for (int i = 0; i < signMachineList.size(); i++) {
                    if (signMachineList.get(i).isChoose()) {
                        chooseMachine.add(signMachineList.get(i));
                    }
                }
               // has.setText("已选  " + chooseMachine.size() + "");
                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(signMachineList.size());//停止刷新
            }
        });
    }

    public void renderView() {
        mAdapter = new CommonAdapter<MachineInfo>(mContext, R.layout.item_machine, signMachineList) {
            @Override
            protected void convert(ViewHolder viewHolder, final MachineInfo s, int i) {
                if (s != null) {
                    viewHolder.setText(R.id.name, s.getName());//昵称
                    TextView name = (TextView) viewHolder.getView(R.id.name);
                    IvUser = (RelativeLayout) viewHolder.getView(R.id.iv_user);
                    getState(signMachineList.get(i - 1).getTaskstate() + "");
                    if (signMachineList.get(i - 1).getTaskstate() == VariableConstant.stateonline && signMachineList.get(i - 1).isChoose()) {
                        name.setTextColor(mContext.getResources().getColor(R.color.white));
                        IvUser.setBackgroundResource(R.mipmap.selected);
                    } else if (signMachineList.get(i - 1).getTaskstate() == VariableConstant.stateonline && !signMachineList.get(i - 1).isChoose()) {
                        name.setTextColor(mContext.getResources().getColor(R.color.black));
                        IvUser.setBackgroundResource(R.mipmap.anull);//未选中
                    }
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
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);

                    break;
                case "2":
                    tmp = "正在对讲";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);

                    break;
                case "3":
                    tmp = "点播";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "4":
                    tmp = "选播";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "5":
                    tmp = "快捷寻呼";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "6":
                    tmp = "寻呼";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "7":
                    tmp = "本地扩音";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "8":
                    tmp = "USB播放";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "9":
                    tmp = "请求对讲";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "10":
                    tmp = "被请求对讲";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "11":
                    tmp = "播放寻呼";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "12":
                    tmp = "报警";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    break;
                case "13":
                    tmp = "采播";
                    IvUser.setBackgroundResource(R.mipmap.machine_busy);
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



}
