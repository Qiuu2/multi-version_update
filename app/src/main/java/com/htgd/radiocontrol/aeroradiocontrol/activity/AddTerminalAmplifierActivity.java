package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static java.lang.Thread.sleep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TimeUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.DialogWeekSelect;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.TerminalPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.TimePop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.Editbox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.SpinnerBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.TitleLayout;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.VolumeView;

import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2020/11/12:10:52
 * 邮箱：535708929
 * 说明：添加终端功放
 */
public class AddTerminalAmplifierActivity extends BaseActivity {
    private TaskGuangboModel model;
    private AddTerminalAmplifierActivity mContext;
    private String mTag = "AddTerminalAmplifierActivity";
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<>();
    private int terminalsize = 0;
    private SpinnerBox execMode;
    private ButtonBox playTime, terminal, startDate, endDatess, playtimelength;
    private Editbox edTaskname;
    private VolumeView volumeView;
    private Bundle bundle;
    private boolean isFirst = true;
    private TaskMainMethod taskMainMethod;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_terminalamplifier;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        taskMainMethod = new TaskMainMethod(mContext);
        model = new  TaskGuangboModel("22", 10, 0, 80, 3, 2, TimeUtils.getDate(),
                TimeUtils.getDate(), 127, 5, "万志强", TimeUtils.getTime(), TimeUtils.getTime(),
                "0", 1, "东风破", "方案名称", 0, "22",
                0, 0, "string", 4800, 2);
        Intent intent = getIntent();
        bundle = intent.getExtras();
        TitleLayout titleView = (TitleLayout) findViewById(R.id.title_layout);
        //设置任务名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.taskname);
        model.setTaskname(edTaskname.getTaskname());
        //设置音量
        volumeView = (VolumeView) findViewById(R.id.volume_view);
        int volume = volumeView.getVolume(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                model.setVolume(Integer.parseInt(s));
            }
        });   

        //设置头右边
        if (bundle == null) {
            titleView.settitle(ChinaConstants.add_termnal_amplifier);
        } else {
            titleView.settitle(ChinaConstants.modify_termnal_amplifier);
        }
        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                if (edTaskname.getTaskname() != null && chooseMachineList.size() > 0) {
                    model.setTaskname(edTaskname.getTaskname());
                    if (model.getTimelength().contains(":") || model.getTimelength().length() > 7) {//格式为11:11:11时
                        model.setTimelength(taskMainMethod.setTimeLengthToSecond(model.getTimelength()));
                    }
                    if (bundle != null) {
                        try {
                            taskMainMethod.putTaskRefresh(model, chooseMachineList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            taskMainMethod.postTask(model, chooseMachineList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    finish();

                } else if (edTaskname.getTaskname() == null) {
                    showToast(ChinaConstants.please_write_taskname);
                } else if (chooseMachineList.size() == 0) {
                    showToast(ChinaConstants.please_choose_machine);
                }
            }
        });
        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                finish();
            }
        });
        //设置执行模式
        execMode = (SpinnerBox) findViewById(R.id.exec_mode);
        execMode.setSpinner(getResources().getStringArray(R.array.exec_mode));
        execMode.setName(ChinaConstants.execmode);

        execMode.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                LogUtils.setLog(mTag, "执行模式点击了"+isFirst);
                if (s.equals(ChinaConstants.WeekDay)) {
                    if (!isFirst) {
                        DialogWeekSelect dialogWeekSelect = new DialogWeekSelect(mContext, model, isFirst, new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                execMode.setSpinnerRightText(s.replace(ChinaConstants.WeekDay, ""));
                            }
                        });
                        dialogWeekSelect.show();
                    }
                } else if (s.equals("每天")) {
                    execMode.setSpinnerRightText("");
                    model.setExecmode(127);
                } else {
                    execMode.setSpinnerRightText("");
                    model.setExecmode(0);
                }
            }
        });
        //设置播放时间
        playTime = (ButtonBox) findViewById(R.id.start_time);
        playTime.setName(ChinaConstants.starttime);
        playTime.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext, ChinaConstants.starttime, model, playTime.getButton());

            }
        });
        //设置开始日期
        startDate = (ButtonBox) findViewById(R.id.start_date);
        startDate.setName(ChinaConstants.startdate);
        startDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePop timePop = new TimePop(mContext, ChinaConstants.startdate, model, startDate.getButton());
            }
        });
        //设置结束日期
        endDatess = (ButtonBox) findViewById(R.id.end_date);
        endDatess.setName(ChinaConstants.enddate);
        endDatess.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext, ChinaConstants.enddate, model, endDatess.getButton());
                LogUtils.setLog(mTag, "结束日期" + model.getEnddate());
            }
        });
        //设置播放时长
        playtimelength = (ButtonBox) findViewById(R.id.play_time_length);
        playtimelength.setName(ChinaConstants.playtimelength);
        playtimelength.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 new TimePop(mContext, ChinaConstants.playtimelength, model, playtimelength.getButton());


            }
        });
        //设置终端列表
        terminal = (ButtonBox) findViewById(R.id.terminal);
        terminal.setName(ChinaConstants.terminallist);
        terminal.setButtonTextHint(mContext.getResources().getString(R.string.pleasegetmachine));
        terminal.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            new TerminalPop(mContext, chooseMachineList, new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        terminal.setButtonText("已选" + s + "台");
                    }
                });

            }
        });
        initView();
        LogUtils.setLog(mTag, "初始化结束"+isFirst);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    sleep(2000);
                    isFirst = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void initView() {
        initModel();
        edTaskname.setEdit(model.getTaskname());
        startDate.setButtonText(model.getStartdate());
        endDatess.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        if (model.getTimelength().contains(":")) {
            playtimelength.setButtonText(model.getTimelength());
        } else {
            playtimelength.setButtonText(TaskMainMethod.timelengthSecondToTime(model.getTimelength()).toString());
        }
        volumeView.setVolume(model.getVolume());
        if (model.getExecmode() == 127) {
            execMode.setSpinnerSelected("每天");
        } else if (model.getExecmode() == 0) {
            execMode.setSpinnerSelected("手动");
        } else {
            execMode.setSpinnerSelected(ChinaConstants.WeekDay);
            LogUtils.setLog(mTag, "设置执行模式"+isFirst);
            execMode.setSpinnerRightText(TaskMainMethod.setWeekDay(model.getExecmode()).replace(ChinaConstants.WeekDay, ""));
        }
        if (bundle != null) {//修改任务时获取终端
            try {
                taskMainMethod.getMachineListFromServer(Integer.parseInt(model.getTaskid()), chooseMachineList,terminal );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        terminalsize = chooseMachineList.size();
    }

     
    private void initModel() {

        Intent intent = getIntent();
        bundle = intent.getExtras();

        if (bundle == null) {
            LogUtils.setLog(mTag, "新添加的model" + TimeUtils.getTime());
            TimeUtils.getDate();

        } else {

            model = ( TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL);
            LogUtils.setLog(mTag, "旧model填充" + model.getExecmode());
            suppleModel();
        }
    }

    private void suppleModel() {
        model.setSechename("sd");
        model.setMedianame("dsd");
        model.setTimelengthtype("0");
        model.setCmdargs("22");
        model.setLiveterminalname("string");
        model.setTasktype(5);
    }


}

