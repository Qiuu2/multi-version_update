package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TimeUtils;
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
 * 时间：2020/11/17:11:26
 * 邮箱：535708929
 * 说明：添加方案活动
 */
public class AddSchemeActivity extends BaseActivity {
    private TaskGuangboModel model;
    private String mTag = "AddSchemeActivity";
    private TitleLayout titleView;
    private Editbox edTaskname;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<MachineInfo>();
    private Context mContext;
    private Bundle bundle;
    private int Request_Model = 1;

    private ButtonBox playtimelength, playTime, terminal,startDate,endDatess;
    private SpinnerBox cycletimes, music,execMode,priority,preopen,sendmod;
    private RadioGroup rg;
    private RadioButton rb_man, rb_woman;
    private ArrayList<MusicInfoModel> musicList = new ArrayList<>();
    private String[] musicLists;
    private VolumeView volumeView;
    private boolean isFirst=true;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_scheme_all;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        initModel();
        titleView = (TitleLayout) findViewById(R.id.title_layout);
        //设置头
        if (bundle == null) {
            titleView.settitle(ChinaConstants.add_scheme);
        } else {
            titleView.settitle(ChinaConstants.change_scheme);
        }
        titleView.setRightButtons();
        titleView.setRightButtonVisible(false);

        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                finish();
            }
        });
        //设置方案名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.schemename);
        model.setSechename(edTaskname.getTaskname());
        //设置预开电源
          preopen = (SpinnerBox) findViewById(R.id.preopen);
        preopen.setSpinner(getResources().getStringArray(R.array.preopen));
        preopen.setName(ChinaConstants.preopen);
        preopen.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {

                model.setPrepower(Integer.parseInt(s));
                preopen.setSpinnerRightText(mContext.getResources().getString(R.string.times));
            }
        });
        //设置优先级
          priority = (SpinnerBox) findViewById(R.id.priority);
        priority.setSpinner(getResources().getStringArray(R.array.priority));
        priority.setName(ChinaConstants.priority);
        priority.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {

                model.setLevel(Integer.parseInt(s));
            }
        });
        //设置执行模式
        execMode = (SpinnerBox) findViewById(R.id.exec_mode);
        execMode.setSpinner(getResources().getStringArray(R.array.exec_mode));
        execMode.setName(ChinaConstants.execmode);

        execMode.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                LogUtils.setLog(mTag,"执行模式点击了");
                if (s.equals(ChinaConstants.WeekDay) ) {
                    if(!isFirst) {

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
        //设置开始日期
         startDate = (ButtonBox) findViewById(R.id.start_date);
        startDate.setName(ChinaConstants.startdate);
        startDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               new TimePop(mContext, ChinaConstants.startdate, model, startDate.getButton());
            }
        });
        //设置结束日期
          endDatess = (ButtonBox) findViewById(R.id.end_date);
        endDatess.setName(ChinaConstants.enddate);
        endDatess.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePop(mContext, ChinaConstants.enddate, model, endDatess.getButton());
            }
        });
        //设置任务
        ButtonBox tasks = (ButtonBox) findViewById(R.id.tasks);
        if (bundle == null) {
            tasks.setName(ChinaConstants.addtask);
            tasks.setButtonTextHint(mContext.getResources().getString(R.string.pleaseaddtask));
            tasks.setButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                     if(edTaskname.getTaskname().length()!=0) {
                         model.setSechename(edTaskname.getTaskname());
                         Intent intent = new Intent(mContext, AddSchemeTaskActivity.class);
                         Bundle bundle = new Bundle();
                         bundle.putSerializable(CacheConstants.NETWORK_MODEL_SCHEME, model);
                         LogUtils.setLog(mTag,"方案名称"+model.getTimelength()+model.getSechename());
                         bundle.putSerializable(CacheConstants.SCHEMECHOOSEMACHINE, chooseMachineList);
                         for (int i = 0; i < musicLists.length; i++) {
                             if(model.getMedianame().equals(musicLists[i])){
                                 bundle.putSerializable(CacheConstants.SCHEMECHOOSEMUSIC, musicList.get(i));
                             }
                         }
                         intent.putExtras(bundle);
                         startActivityForResult(intent, Request_Model);
                         finish();
                     }else{
                         showToast(ChinaConstants.please_write_shemename);
                     }
                }
            });
        } else {//选择需要修改的任务
            tasks.setName(ChinaConstants.selectTask);
            tasks.setButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // new DialogTaskSelect(mContext,model);
                }
            });
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
                        terminal.setButtonText("已选"+s+"台");
                    }
                });
            }
        });

        //设置音量
          volumeView = (VolumeView) findViewById(R.id.volume_view);
        int volume = volumeView.getVolume(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                model.setVolume(Integer.parseInt(s));
            }
        });


        //设置播放时长
        playtimelength = (ButtonBox) findViewById(R.id.play_time_length);
        playtimelength.setName(ChinaConstants.playtimelength);
        playtimelength.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePop timePop = new TimePop(mContext, ChinaConstants.playtimelength, model, playtimelength.getButton());

            }
        });
        //设置循环次数
        cycletimes = (SpinnerBox) findViewById(R.id.cycle_times);
        cycletimes.setName(ChinaConstants.cycletime);
        cycletimes.setSpinner(getResources().getStringArray(R.array.cycle));
        cycletimes.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {

                model.setTimelength(s);
            }
        });
        //设置选择时长还是次数
        rg = (RadioGroup) findViewById(R.id.rg);
        //初始化时长和次数
        rb_man = (RadioButton) findViewById(R.id.man);
        rb_woman = (RadioButton) findViewById(R.id.woman);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                if (radioButton.getText().toString().contains("时长")) {
                    model.setTimelengthtype("0");
                    selectHideOrShow(0);
                } else {
                    model.setTimelengthtype("1");
                    selectHideOrShow(1);
                }
            }
        });
        //设置媒体
        try {
            getMusicFromServer(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //设置播放时间
        playTime = (ButtonBox) findViewById(R.id.start_time);
        playTime.setName(ChinaConstants.starttime);
        playTime.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePop(mContext, ChinaConstants.starttime, model, playTime.getButton());
            }
        });
        initView();
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

    private void selectHideOrShow(int i) {
        if (i == 0) {
            playtimelength.setVisibility(View.VISIBLE);
            cycletimes.setVisibility(View.GONE);
        } else {
            playtimelength.setVisibility(View.GONE);
            cycletimes.setVisibility(View.VISIBLE);
        }
    }
    private void initView() {

        edTaskname.setEdit(model.getTaskname());
        startDate.setButtonText(model.getStartdate());
        endDatess.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        playtimelength.setButtonText(model.getTimelength());
        volumeView.setVolume(model.getVolume());
        initSpinner();
    }

    private void initSpinner() {
        if(model.getExecmode()==127) {
            execMode.setSpinnerSelected("每天");
        }else if(model.getExecmode()==0){
            execMode.setSpinnerSelected("手动");
        }else{
            execMode.setSpinnerSelected(ChinaConstants.WeekDay);
            execMode.setSpinnerRightText(TaskMainMethod.setWeekDay(model.getExecmode()).replace(ChinaConstants.WeekDay,""));
        }
        preopen.setSpinnerSelected(model.getPrepower()+"");
        preopen.setSpinnerRightText(mContext.getResources().getString(R.string.times));
        priority.setSpinnerSelected(model.getPriority()+"");


    }


    private void getMusicFromServer(int folderid) throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMusicInfo + folderid, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);

                if (responseData.getData().get(0).getAll() != 0 && responseData.getData().get(0).getName() != null) {
                    LogUtils.setLog(mTag, "diyishouge" + responseData.getData().get(0).getName());
                    musicList = responseData.getData();
                    musicLists = new String[musicList.size()];
                    for (int i = 0; i < musicList.size(); i++) {
                        musicLists[i] = musicList.get(i).getName();
                    }

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.setLog(mTag, "媒体个数" + musicLists.length);
                            music = (SpinnerBox) findViewById(R.id.scheme_music);
                            music.setSpinner(musicLists);
                            music.setName(ChinaConstants.taskmusic);

                            music.setListener(new SpinnerBox.GetResultListener() {
                                @Override
                                public void getResult(String s) {
                                    model.setMedianame(s);
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "response failed");
            }
        });
    }



    private void initModel() {
        Intent intent = getIntent();
        bundle = intent.getExtras();
        if (bundle == null) {
            LogUtils.setLog(mTag, "新添加的model" + TimeUtils.getTime());
            model = new TaskGuangboModel("1", 2, 9, 66, 3, 2, TimeUtils.getDate(), TimeUtils.getDate(), 127, 3, "", "11:20:20", TimeUtils.getTime(), "1", 1, "string", "string", 0, "4", 16, 0, "string", 4800, 2);

        } else {
            LogUtils.setLog(mTag, "旧model填充");
            model = (TaskGuangboModel) bundle.getSerializable(CacheConstants.CHANGE_SCHEME);

        }

    }

    private void logModel() {
        LogUtils.setLog(mTag, "任务名称" + model.getTaskname());
        LogUtils.setLog(mTag, "开始日期" + model.getStartdate());
        LogUtils.setLog(mTag, "结束日期" + model.getEnddate());
        LogUtils.setLog(mTag, "任务音量" + model.getVolume());
        LogUtils.setLog(mTag, "开始时间" + model.getStarttime());
        LogUtils.setLog(mTag, "播放时长" + model.getTimelength());
        LogUtils.setLog(mTag, "采样率" + model.getSamplerate());
        LogUtils.setLog(mTag, "比特率" + model.getBandrate());
        if (model.getTimelength().contains(":")) {
            model.setTimelength(timeToLength(model.getTimelength()));
        }

        LogUtils.setLog(mTag, "播放时长" + model.getTimelength());

    }

    private String timeToLength(String s) {

        StringBuffer a = new StringBuffer(s);
        StringBuffer b = new StringBuffer(s);
        StringBuffer c = new StringBuffer(s);
        LogUtils.setLog(mTag, c.substring(6, 7) + "截取的" + a.substring(0, 2) + b);
        return Integer.parseInt(a.substring(0, 2)) * 3600 + Integer.parseInt(b.substring(3, 5)) * 60 + Integer.parseInt(c.substring(6, 7)) + "";
    }



}
