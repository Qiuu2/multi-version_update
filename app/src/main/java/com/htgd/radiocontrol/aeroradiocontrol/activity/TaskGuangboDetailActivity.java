package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GVMachineAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MusicListAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TtsTaskContentModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TtsTaskContentRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;

import java.io.IOException;
import java.util.ArrayList;

import radiocontrol.htgd.com.aeroradiocontrol.widget.MyGridView;
import radiocontrol.htgd.com.aeroradiocontrol.widget.MyListView;

/**
 * Created by wzq on 2017-08-04.
 */
public class TaskGuangboDetailActivity extends BaseActivity implements View.OnClickListener {
    private Context mContext;
    private final String tag = "TaskDetailActivity";
    private ArrayList<MachineInfo> machineInfoList = new ArrayList<>();
    private ArrayList<MusicInfoModel> musicInfoList = new ArrayList<>();
    private ViewHolder viewHolder = new ViewHolder();
    private TaskGuangboModel taskGuangboModel;
    private GVMachineAdapter machineAdapter;
    private MusicListAdapter musicListAdapter;
    private TaskManageUtils taskManageUtils;
    private int flag;
    private String mTag = "TaskGuangboDetailActivity";
    private ArrayList<TtsTaskContentModel> ttsContentModelList;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_task_detail;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        taskManageUtils = new TaskManageUtils(mContext);
        initFlag();
        initView();
        getDateAndUpdateUI();
    }

    public void initFlag() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        flag = bundle.getInt(Constant.BUNDLE_KEY_TYPE);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_go_back:
                finish();
                break;
            case R.id.button_turn_dowm:
                try {
                    setTaskVoice(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.button_turn_up:
                try {
                    setTaskVoice(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.use_or_stop:
                LogUtils.setLog(mTag, "qitingstate" + taskGuangboModel.getEnablestate());
                LogUtils.setLog(mTag, "zhixingstate" + taskGuangboModel.getTaskstate());
                if (taskGuangboModel.getEnablestate() == 0) {
                    try {
                        startOrStopTask(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        startOrStopTask(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.run_or_stop:
                LogUtils.setLog(mTag, "qitingstate" + taskGuangboModel.getEnablestate());
                LogUtils.setLog(mTag, "zhixingstate" + taskGuangboModel.getTaskstate());
                if (taskGuangboModel.getTaskstate() == 0) {
                    try {
                        runOrStopTask(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        runOrStopTask(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //获取bundle中的数据更新ui
    private synchronized void getDateAndUpdateUI() {

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        taskGuangboModel = (TaskGuangboModel) bundle.getSerializable(Constant.BUNDLE_KEY_MODEL);
        updateUIByModel();
        if (flag == IntConstans.keyWenZiYuYin) {
            viewHolder.contentLv.setVisibility(View.VISIBLE);
            try {
                getTtsTaskContents();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                getMusicListFromSever();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            getMachineListFromServer();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void getTtsTaskContents() throws IOException {
        RequestManger.getInstance().get( Constant.serveraddress + Constant.getTtsTaskContent + "/" + taskGuangboModel.getTaskid(), new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TtsTaskContentRsp responseData = JsonUtil.getInstance().deSerializeString(response, TtsTaskContentRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ttsContentModelList = responseData.getData();
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.setLog(mTag,"获取的tts任务内容"+ttsContentModelList.get(0).getContents()+"速率"+ttsContentModelList.get(0).getSpeed()+"男女"+ttsContentModelList.get(0).getMale());
                            viewHolder.content.setText(ttsContentModelList.get(0).getContents());
                            taskGuangboModel.setContent(ttsContentModelList.get(0).getContents());
                            taskGuangboModel.setSpeed(ttsContentModelList.get(0).getSpeed());
                            taskGuangboModel.setMale(ttsContentModelList.get(0).getMale());

                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }
    private synchronized void getMachineListFromServer() throws IOException {
        if (taskGuangboModel != null) {
            String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskMachines + "/" + taskGuangboModel.getTaskid();
            RequestManger.getInstance().get(url, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                    if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                        machineInfoList = responseData.getData();
                        machineAdapter.setMachineInfos(machineInfoList);
                        if (machineInfoList.size() >= 1) {
                            updateMachineUI();
                        }
                    } else {
                        LogUtils.setLog(tag, "that data is eorro");
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

    private synchronized void getMusicListFromSever() throws IOException {
        if (taskGuangboModel != null) {
            String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskMusics + "/" + taskGuangboModel.getTaskid();
            RequestManger.getInstance().get(url, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                    if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                        musicInfoList = responseData.getData();

                        musicListAdapter.setMusicLists(musicInfoList);
                        LogUtils.setLog("sizemu" + musicInfoList.size());
                        if (musicInfoList.get(0).getName() != null) {
                            updateMuscieUI();
                        }
                        LogUtils.setLog(tag, "the music name is " + responseData.getData().get(0).getName());
                    } else {
                        LogUtils.setLog(tag, "that data is eorro");
                    }
                }

                @Override
                public void onFailed(int code, String message) {
                    if (ErrorCode.TOKEN_EXPIRED == code) {
                        reTry();
                    }
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
                viewHolder.machineLv.setVisibility(View.VISIBLE);
                machineAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateMuscieUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewHolder.musicLv.setVisibility(View.VISIBLE);
                musicListAdapter.notifyDataSetChanged();
            }
        });
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private synchronized void reTry() {
        try {
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getMusicListFromSever();
                        getMachineListFromServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed() {
                    showToast(R.string.get_token_failed);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 启用或停止任务
    private synchronized void startOrStopTask(final int state) throws IOException {
        taskManageUtils.useOrStopTask(taskGuangboModel.getTaskid(), state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(tag, "the call back is sucess" + state);
                taskGuangboModel.setEnablestate(state);
                LogUtils.setLog("The enableSate is" + taskGuangboModel.getEnablestate());
                updateUIByModel();
            }

            @Override
            public void onTheSameStatu() {
                LogUtils.setLog(tag, "the call back is onTheSameStatu" + state);
                taskGuangboModel.setEnablestate(state);
                updateUIByModel();
            }

            @Override
            public void onRetry() {

            }
        });
    }

    // 调节声音
    private synchronized void setTaskVoice(boolean cutDown) throws IOException {
        int voice = taskGuangboModel.getVolume();
        if (cutDown) {
            voice = voice - 5;
        } else {
            voice = voice + 5;
        }
        final int finalVoice = voice;
        //音量调节上传服务器
        taskManageUtils.setTaskVoice(taskGuangboModel.getTaskid(), voice, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                taskGuangboModel.setVolume(finalVoice);
                updateUIByModel();
            }

            @Override
            public void onTheSameStatu() {
                updateUIByModel();
            }

            @Override
            public void onRetry() {

            }
        });
    }

    //执行或停止方案
    private synchronized void runOrStopTask(final int state) throws IOException {
        taskManageUtils.runOrStopTask(taskGuangboModel.getTaskid(), state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(tag, "the call back is sucess" + state);
                showToast("指令执行");
                taskGuangboModel.setTaskstate(state);
                updateUIByModel();
            }

            @Override
            public void onTheSameStatu() {
                LogUtils.setLog(tag, "the call back is onTheSameStatu" + state);
                taskGuangboModel.setTaskstate(state);
                updateUIByModel();
            }

            @Override
            public void onRetry() {

            }
        });
    }

    //通过model的数值更新UI
    private void updateUIByModel() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (taskGuangboModel != null) {
                    viewHolder.taskName_tv.setText(taskGuangboModel.getTaskname());//名称
                    viewHolder.startTime_tv.setText(taskGuangboModel.getStartdate());//开始日期
                    viewHolder.endTime_tv.setText(taskGuangboModel.getEnddate());//结束日期
                    viewHolder.runTime_tv.setText(taskGuangboModel.getStarttime());//执行时间
                    if (flag == IntConstans.keyGuangbo) {//时长
                        viewHolder.play_time.setText(TaskMainMethod.setTimeLength(Integer.parseInt(taskGuangboModel.getTimelength()), Integer.parseInt(taskGuangboModel.getTimelengthtype())));
                    } else if (flag == IntConstans.keyWenZiYuYin) {
                        viewHolder.play_time.setText(TaskMainMethod.setTimeLength(Integer.parseInt(taskGuangboModel.getTimelength()), 2));
                    } else {
                        viewHolder.play_time.setText(TaskMainMethod.setTimeLength(Integer.parseInt(taskGuangboModel.getTimelength()), 1));
                    }
                    viewHolder.levelNo_tv.setText(taskGuangboModel.getPriority() + "");//优先级
                    viewHolder.voiveNo_tv.setText(taskGuangboModel.getVolume() + "");//音量

                    setWeekDay(taskGuangboModel.getExecmode());
                    LogUtils.setLog("the task Enablestate is " + taskGuangboModel.getEnablestate());
                    switch (taskGuangboModel.getEnablestate()) {//任务启停
                        case 0:
                            viewHolder.startOrStopTasks.setChecked(false);
                            break;
                        case 1:
                            viewHolder.startOrStopTasks.setChecked(true);
                            break;
                    }
                    LogUtils.setLog(mTag + flag + "kk");

                    if (taskGuangboModel.getTaskstate() == 0) {
                        viewHolder.runOrStopTask.setText("运行任务");
                        viewHolder.runOrStopTask.setBackgroundColor(Color.parseColor("#D3D3D3"));
                    } else {
                        viewHolder.runOrStopTask.setText("停止任务");
                        viewHolder.runOrStopTask.setBackgroundColor(Color.parseColor("#3986f9"));
                    }

                } else {
                    showToast("data is missing ");
                }
            }
        });
    }

    private class ViewHolder {

        public TextView content;
        private Button title_left;
        private TextView title_tv, taskName_tv, startTime_tv, endTime_tv, play_time, voiveNo_tv, levelNo_tv, runTime_tv;
        private TextView textType1, textType2, textType3, textType4, textType5, textType6;
        private View view1, view3, view4, view5, view6;
        private MyListView musicList_lv;
        private MyGridView machineList_gv;
        private ScrollView taskDetail_sv;
        private Button turnDown_bt, turnUp_bt, runOrStopTask;
        private Switch startOrStopTasks;
        private ImageView weekday[] = new ImageView[7];
        private LinearLayout layout_view_oneweek, machineLv, musicLv, contentLv;
        private ImageView day1, day2, day3, day4, day5, day6, day7;
    }

    private void initView() {
        viewHolder.layout_view_oneweek = (LinearLayout) findViewById(R.id.layout_view_oneweeks);
        viewHolder.layout_view_oneweek.setVisibility(View.VISIBLE);
        viewHolder.machineLv = (LinearLayout) findViewById(R.id.machine_lv);
        viewHolder.musicLv = (LinearLayout) findViewById(R.id.music_lv);
        viewHolder.contentLv = (LinearLayout) findViewById(R.id.content_lv);
        viewHolder.content = (TextView) findViewById(R.id.content);
        viewHolder.title_tv = (TextView) findViewById(R.id.title_text);
        viewHolder.title_tv.setText("任务详情");
        viewHolder.title_left = (Button) findViewById(R.id.title_go_back);
        viewHolder.title_left.setVisibility(View.VISIBLE);
        viewHolder.title_left.setOnClickListener(this);
        viewHolder.view1 = findViewById(R.id.start_time);
        viewHolder.view3 = findViewById(R.id.end_time);
        viewHolder.view4 = findViewById(R.id.run_time);
        viewHolder.view5 = findViewById(R.id.level_no);
        viewHolder.view6 = findViewById(R.id.play_time);
        viewHolder.textType1 = (TextView) viewHolder.view1.findViewById(R.id.text_name);
        viewHolder.textType2 = (TextView) findViewById(R.id.text_name);
        viewHolder.textType2.setText("开始日期");
        viewHolder.textType3 = (TextView) viewHolder.view3.findViewById(R.id.text_name);
        viewHolder.textType3.setText("结束日期");
        viewHolder.textType4 = (TextView) viewHolder.view4.findViewById(R.id.text_name);
        viewHolder.textType4.setText("执行时间");
        viewHolder.textType5 = (TextView) viewHolder.view5.findViewById(R.id.text_name);
        viewHolder.textType5.setText("任务级别");
        viewHolder.textType6 = (TextView) viewHolder.view6.findViewById(R.id.text_name);
        viewHolder.textType6.setText("播放时长");
        viewHolder.taskName_tv = (TextView) findViewById(R.id.task_name);
        viewHolder.startTime_tv = (TextView) viewHolder.view1.findViewById(R.id.text_info);
        viewHolder.startTime_tv.setTextSize(16);
        viewHolder.endTime_tv = (TextView) viewHolder.view3.findViewById(R.id.text_info);
        viewHolder.endTime_tv.setTextSize(16);
        viewHolder.runTime_tv = (TextView) viewHolder.view4.findViewById(R.id.text_info);
        viewHolder.levelNo_tv = (TextView) viewHolder.view5.findViewById(R.id.text_info);
        viewHolder.play_time = (TextView) viewHolder.view6.findViewById(R.id.text_info);
        viewHolder.voiveNo_tv = (TextView) findViewById(R.id.task_voice);
        viewHolder.startOrStopTasks = (Switch) findViewById(R.id.use_or_stop);
        viewHolder.startOrStopTasks.setOnClickListener(this);
        viewHolder.musicList_lv = (MyListView) findViewById(R.id.music_list);
        viewHolder.machineList_gv = (MyGridView) findViewById(R.id.machine_list);
        viewHolder.taskDetail_sv = (ScrollView) findViewById(R.id.task_detail_sv);
        machineAdapter = new GVMachineAdapter(mContext, machineInfoList);
        viewHolder.machineList_gv.setAdapter(machineAdapter);
        musicListAdapter = new MusicListAdapter(mContext, musicInfoList);
        viewHolder.musicList_lv.setAdapter(musicListAdapter);
        viewHolder.musicList_lv.setFocusable(false);
        viewHolder.taskDetail_sv.smoothScrollTo(0, 0);
        viewHolder.turnDown_bt = (Button) findViewById(R.id.button_turn_dowm);
        viewHolder.turnDown_bt.setOnClickListener(this);
        viewHolder.turnUp_bt = (Button) findViewById(R.id.button_turn_up);
        viewHolder.turnUp_bt.setOnClickListener(this);

        viewHolder.runOrStopTask = (Button) findViewById(R.id.run_or_stop);
        viewHolder.runOrStopTask.setOnClickListener(this);

        viewHolder.day1 = (ImageView) findViewById(R.id.day1);
        viewHolder.day2 = (ImageView) findViewById(R.id.day2);
        viewHolder.day3 = (ImageView) findViewById(R.id.day3);
        viewHolder.day4 = (ImageView) findViewById(R.id.day4);
        viewHolder.day5 = (ImageView) findViewById(R.id.day5);
        viewHolder.day6 = (ImageView) findViewById(R.id.day6);
        viewHolder.day7 = (ImageView) findViewById(R.id.day7);
        viewHolder.weekday[0] = viewHolder.day1;
        viewHolder.weekday[1] = viewHolder.day2;
        viewHolder.weekday[2] = viewHolder.day3;
        viewHolder.weekday[3] = viewHolder.day4;
        viewHolder.weekday[4] = viewHolder.day5;
        viewHolder.weekday[5] = viewHolder.day6;
        viewHolder.weekday[6] = viewHolder.day7;
    }

    //设置星期几的显示
    private void setWeekDay(int execmode) {
        ArrayList<Integer> day = new ArrayList<Integer>();
        String b = Integer.toBinaryString(execmode);
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            int e = c - '0';
            day.add(e);
        }
        for (int i = 0; i < 7 - b.length(); i++) {
            day.add(0, 0);
        }
        for (int i = 0; i < 7; i++) {
            if (day.get(0) == 0) {
                viewHolder.weekday[0].setImageResource(R.mipmap.week_day1_false);
                ;
            } else {
                viewHolder.weekday[0].setImageResource(R.mipmap.week_day1_true);
            }
            if (day.get(1) == 0) {
                viewHolder.weekday[1].setImageResource(R.mipmap.week_day2_false);

            } else {
                viewHolder.weekday[1].setImageResource(R.mipmap.week_day2_true);
            }
            if (day.get(2) == 0) {
                viewHolder.weekday[2].setImageResource(R.mipmap.week_day3_false);
                ;
            } else {
                viewHolder.weekday[2].setImageResource(R.mipmap.week_day3_true);
            }
            if (day.get(3) == 0) {
                viewHolder.weekday[3].setImageResource(R.mipmap.week_day4_false);
                ;
            } else {
                viewHolder.weekday[3].setImageResource(R.mipmap.week_day4_true);
            }
            if (day.get(4) == 0) {
                viewHolder.weekday[4].setImageResource(R.mipmap.week_day5_false);
                ;
            } else {
                viewHolder.weekday[4].setImageResource(R.mipmap.week_day5_true);
            }
            if (day.get(5) == 0) {
                viewHolder.weekday[5].setImageResource(R.mipmap.week_day6_false);
                ;
            } else {
                viewHolder.weekday[5].setImageResource(R.mipmap.week_day6_true);
            }
            if (day.get(6) == 0) {
                viewHolder.weekday[6].setImageResource(R.mipmap.week_day7_false);
                ;
            } else {
                viewHolder.weekday[6].setImageResource(R.mipmap.week_day7_true);
            }
        }
    }


}
