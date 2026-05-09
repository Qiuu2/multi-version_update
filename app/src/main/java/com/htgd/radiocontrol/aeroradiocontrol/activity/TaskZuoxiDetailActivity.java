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
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GVMachineAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MusicListAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskZuoxiModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;

import java.io.IOException;
import java.util.ArrayList;
import radiocontrol.htgd.com.aeroradiocontrol.widget.MyGridView;
import radiocontrol.htgd.com.aeroradiocontrol.widget.MyListView;

/**
 * Created by wzq on 2017-08-15.
 */
public class TaskZuoxiDetailActivity extends BaseActivity implements View.OnClickListener {
    private Context mContext;
    private final String tag = "TaskZuoxiDetailActivity";
    private ArrayList<MachineInfo> machineInfoList = new ArrayList<>();
    private ArrayList<MusicInfoModel> musicInfoList = new ArrayList<>();
    private ViewHolder viewHolder = new ViewHolder();
    private TaskGuangboModel taskZuoxiModel;
    private GVMachineAdapter machineAdapter;
    private MusicListAdapter musicListAdapter;
    private TaskManageUtils taskManageUtils;
    private int projectstate;
    private String Tag="TaskZuoxiDetailActivity";


    @Override
    protected int getLayoutId() {
        return R.layout.activity_scheme_detail;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        taskManageUtils = new TaskManageUtils(mContext);
        initView();
        getDateAndUpdateUI();
        LogUtils.setLog(Tag,"dangqianstate"+taskZuoxiModel.getTaskstate());
    }
    //
    private synchronized void getDateAndUpdateUI() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        taskZuoxiModel = (TaskGuangboModel) bundle.getSerializable(Constant.BUNDLE_KEY_MODEL);
//        projectstate = Integer.parseInt(bundle.get("projectstate").toString());
//        taskZuoxiModel.setProjectstate(projectstate);
        updateUIByModel();
        MusicInfoModel model = new MusicInfoModel();
        model.setName(taskZuoxiModel.getMedianame());
        model.setAll(1);
        musicInfoList.add(model);
        musicListAdapter = new MusicListAdapter(mContext, musicInfoList);
        viewHolder.musicList_lv.setAdapter(musicListAdapter);
        viewHolder.musicList_lv.setFocusable(false);
        try {
            getMachineListFromServer();
            getMusicListFromSever();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private class ViewHolder {
        private Button title_left;
        private TextView title_tv, taskName_tv, startTime_tv, endTime_tv, voiveNo_tv, levelNo_tv, runTime_tv, startOrStopTasks;
        private TextView textType1, textType2, textType3, textType4, textType5;
        private View view1, view2, view3, view4, view5;
        private MyListView musicList_lv;
        private ImageView weekday[] = new ImageView[7];
        private MyGridView machineList_gv;
        private ScrollView taskDetail_sv;
        private LinearLayout layout_view_oneweek;
        private Button turnDown_bt, turnUp_bt,runOrStopTask;
        private ImageView day1, day2, day3, day4, day5, day6, day7;
    }

    private void initView() {
        viewHolder.layout_view_oneweek = (LinearLayout) findViewById(R.id.layout_view_oneweeks);
        viewHolder.layout_view_oneweek.setVisibility(View.VISIBLE);
        viewHolder.title_tv = (TextView) findViewById(R.id.title_text);
        viewHolder.title_tv.setText("任务详情");
        viewHolder.title_left = (Button) findViewById(R.id.title_go_back);
        viewHolder.title_left.setVisibility(View.VISIBLE);
        viewHolder.title_left.setOnClickListener(this);

        viewHolder.view1 = findViewById(R.id.start_time);
        viewHolder.view2 = findViewById(R.id.use_stop);
        viewHolder.view3 = findViewById(R.id.end_time);
        viewHolder.view4 = findViewById(R.id.run_time);
        viewHolder.view5 = findViewById(R.id.level_no);


        viewHolder.textType1 = (TextView) viewHolder.view1.findViewById(R.id.text_name);
        viewHolder.textType1.setText("开始日期");
        viewHolder.textType2 = (TextView) viewHolder.view2.findViewById(R.id.text_name);
        viewHolder.textType2.setText("启用状态");
        viewHolder.textType3 = (TextView) viewHolder.view3.findViewById(R.id.text_name);
        viewHolder.textType3.setText("结束日期");
        viewHolder.textType4 = (TextView) viewHolder.view4.findViewById(R.id.text_name);
        viewHolder.textType4.setText("执行时间");
        viewHolder.textType5 = (TextView) viewHolder.view5.findViewById(R.id.text_name);
        viewHolder.textType5.setText("任务级别");
        viewHolder.taskName_tv = (TextView) findViewById(R.id.task_name);
        viewHolder.startTime_tv = (TextView) viewHolder.view1.findViewById(R.id.text_info);
        viewHolder.startOrStopTasks = (TextView) viewHolder.view2.findViewById(R.id.text_info);
        viewHolder.endTime_tv = (TextView) viewHolder.view3.findViewById(R.id.text_info);
        viewHolder.runTime_tv = (TextView) viewHolder.view4.findViewById(R.id.text_info);
        viewHolder.levelNo_tv = (TextView) viewHolder.view5.findViewById(R.id.text_info);
        viewHolder.voiveNo_tv = (TextView) findViewById(R.id.task_voice);
        viewHolder.musicList_lv = (MyListView) findViewById(R.id.music_list);
        viewHolder.machineList_gv = (MyGridView) findViewById(R.id.machine_list);
        viewHolder.taskDetail_sv = (ScrollView) findViewById(R.id.task_detail_sv);
        viewHolder.turnDown_bt = (Button) findViewById(R.id.button_turn_dowm);
        viewHolder.turnDown_bt.setOnClickListener(this);
        viewHolder.turnUp_bt = (Button) findViewById(R.id.button_turn_up);
        viewHolder.turnUp_bt.setOnClickListener(this);
        viewHolder.runOrStopTask = (Button) findViewById(R.id.run_or_stop);

        viewHolder.runOrStopTask.setOnClickListener(this);
        machineAdapter = new GVMachineAdapter(mContext, machineInfoList);
        viewHolder.machineList_gv.setAdapter(machineAdapter);
        viewHolder.taskDetail_sv.smoothScrollTo(0, 0);

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
             case R.id.run_or_stop:
               if(taskZuoxiModel.getTaskstate()==0) {
                   try {
                       runOrStopTask(1);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }else {
                   try {
                       runOrStopTask(0);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }

        }
    }

    // 调节声音
    private synchronized void setTaskVoice(boolean cutDown) throws IOException {
        int voice = taskZuoxiModel.getVolume();
        if (cutDown) {
            voice = voice - 5;
        } else {
            voice = voice + 5;
        }
        final int finalVoice = voice;
        //音量调节上传服务器
        taskManageUtils.setTaskVoice(taskZuoxiModel.getTaskid()+"", voice, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                taskZuoxiModel.setVolume(finalVoice);

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.voiveNo_tv.setText(taskZuoxiModel.getVolume() + "");
                    }
                });
            }

            @Override
            public void onTheSameStatu() {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.voiveNo_tv.setText(taskZuoxiModel.getVolume() + "");
                    }
                });
            }

            @Override
            public void onRetry() {

            }
        });
    }

    //填充更新布局
    private void updateUIByModel() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (taskZuoxiModel != null) {
                    viewHolder.taskName_tv.setText(taskZuoxiModel.getName());
                    viewHolder.startTime_tv.setText(taskZuoxiModel.getStartdate());
                    viewHolder.endTime_tv.setText(taskZuoxiModel.getEnddate());
                    viewHolder.runTime_tv.setText((taskZuoxiModel.getStarttime()));
                    viewHolder.levelNo_tv.setText(taskZuoxiModel.getPriority() + "");
                    viewHolder.voiveNo_tv.setText(taskZuoxiModel.getVolume() + "");
                    setWeekDay(taskZuoxiModel.getExecmode());

                    LogUtils.setLog(Tag,"iii："+taskZuoxiModel.getPriority() + "," +taskZuoxiModel.getVolume() + "");
                    switch (taskZuoxiModel.getProjectstate()) {

                        case 0:
                            viewHolder.startOrStopTasks.setText("启用");
                            break;
                        case 1:
                            viewHolder.startOrStopTasks.setText("停用");

                            break;
                    }
                    LogUtils.setLog(Tag,"XIANSHIANNIU"+taskZuoxiModel.getTaskstate());
                    switch (taskZuoxiModel.getTaskstate()) {//任务执停
                        case 0:
                            LogUtils.setLog(Tag,"XIANSHIANNIU2");
                            viewHolder.runOrStopTask.setText("运行任务");
                            viewHolder.runOrStopTask.setBackgroundColor(Color.parseColor("#D3D3D3"));
                            break;
                        case 1:
                            LogUtils.setLog(Tag,"XIANSHIANNIU6");
                            viewHolder.runOrStopTask.setText("停止任务");
                            viewHolder.runOrStopTask.setBackgroundColor(Color.parseColor("#3986f9"));
                            break;
                        case 3:
                            LogUtils.setLog(Tag,"XIANSHIANNIU6");
                            viewHolder.runOrStopTask.setText("停止任务");
                            viewHolder.runOrStopTask.setBackgroundColor(Color.parseColor("#3986f9"));
                            break;
                    }
                } else {
                    showToast("data is missing ");
                }
            }
        });
    }



    private void getMachineListFromServer() throws IOException {
        LogUtils.setLog(Tag,taskZuoxiModel.getTaskid()+"任务id");
        if (taskZuoxiModel != null && Integer.parseInt(taskZuoxiModel.getTaskid()) != 0) {
            String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskMachines + "/" + taskZuoxiModel.getTaskid();
            RequestManger.getInstance().get(url, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                    if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                        machineInfoList = responseData.getData();
                        LogUtils.setLog(Tag,"设备数量"+machineInfoList.size());
                        machineAdapter.setMachineInfos(machineInfoList);
                        updateMachineUI();
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
            LogUtils.setLog("the taskZuoxiModel or taskId is null");
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

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
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

    //更新设备
    private synchronized void getMusicListFromSever() throws IOException {
        if (taskZuoxiModel != null && Integer.parseInt(taskZuoxiModel.getTaskid()) != 0) {
            String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskMusics + "/" + taskZuoxiModel.getTaskid();
            RequestManger.getInstance().get(url, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                    if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                        musicInfoList = responseData.getData();
                        musicListAdapter.setMusicLists(musicInfoList);
                        updateMuscieUI();
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
            LogUtils.setLog("the taskZuoxiModel or taskId is null");
        }
    }

    //更新音乐
    private void updateMuscieUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                musicListAdapter.notifyDataSetChanged();
            }
        });
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

    //执行或停止方案
    private synchronized void runOrStopTask(final int state) throws IOException {
        taskManageUtils.runOrStopTask(taskZuoxiModel.getTaskid()+"", state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(tag, "the call back is sucess" + state);
                showToast("指令执行");
                taskZuoxiModel.setTaskstate(state);
                updateUIByModel();
            }

            @Override
            public void onTheSameStatu() {
                LogUtils.setLog(tag, "the call back is onTheSameStatu" + state);
                taskZuoxiModel.setTaskstate(state);
                updateUIByModel();

            }

            @Override
            public void onRetry() {

            }
        });
    }
}
