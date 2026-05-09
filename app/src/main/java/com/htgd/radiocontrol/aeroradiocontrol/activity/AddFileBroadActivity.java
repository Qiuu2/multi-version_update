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
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
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
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.DialogWeekSelect;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.MusicPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.TerminalPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.TimePop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.Editbox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.SpinnerBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.TitleLayout;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.VolumeView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.functions.Consumer; 

/**
 * 作者：wzq
 * 时间：2020/10/21:17:04
 * 邮箱：535708929
 * 说明：添加网络任务界面
 */
public class AddFileBroadActivity extends BaseActivity {

    private TaskGuangboModel model;
    private Context mContext;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<MachineInfo>();
    private String mTag="AddFileBroadActivity";
    private ArrayList<MusicInfoModel> chooseMediaList=new ArrayList<>();
    private RadioGroup rg;
    private RadioButton rb_man, rb_woman;
    private ButtonBox playtimelength;
    private SpinnerBox cycletimes;
    private Editbox edTaskname;
    private VolumeView volumeView;
    private ButtonBox endDatess;
    private ButtonBox startDate;
    private ButtonBox music;
    private ButtonBox terminal;
    private ButtonBox playTime;
    private SpinnerBox preopen;
    private SpinnerBox priority;
    private SpinnerBox execMode;
    private Bundle bundle;
    private int terminalsize=0;
    private boolean isFirst=true;
    private TaskMainMethod taskMainMethod;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_filebroad;
    }

    @Override
    protected void initSubViews() {
        mContext=this;
        taskMainMethod = new TaskMainMethod(mContext);
        Intent intent = getIntent();
        bundle = intent.getExtras();
        model = new TaskGuangboModel("11", 10, 0, 80, 10, 0, TimeUtils.getDate(),
                TimeUtils.getDate(), 127, 2, "万志强", TimeUtils.getTime(), TimeUtils.getTime(),
                "1", 1, "东风破", "方案名称", 0, "33",
                0,0,"string",0,0);
        
        //初始化时长和次数
        rb_man = (RadioButton) findViewById(R.id.man);
        rb_woman = (RadioButton) findViewById(R.id.woman);
        TitleLayout titleView = (TitleLayout) findViewById(R.id.title_layout);
        //设置任务名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.taskname);
        model.setTaskname(edTaskname.getTaskname());
        //设置音量6
        volumeView = (VolumeView) findViewById(R.id.volume_view);
        int volume = volumeView.getVolume(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                model.setVolume(Integer.parseInt(s));
            }
        });
        //设置头
        if(bundle==null) {
            titleView.settitle(ChinaConstants.add_file_broad);
        }else{
            titleView.settitle(ChinaConstants.change_file_broad);
        }

        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                submitFunction();
            }
        });
        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                finish();
            }
        });
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
                model.setPriority(Integer.parseInt(s));
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
                          goToSelectWeek();

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
        playTime = (ButtonBox) findViewById(R.id.play_time);
        playTime.setName(ChinaConstants.starttime);
        playTime.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext,ChinaConstants.starttime,model,playTime.getButton());

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
                        terminal.setButtonText("已选"+s+"台");
                    }
                });

            }
        });
        //设置媒体列表
        music = (ButtonBox) findViewById(R.id.music);
        music.setName(ChinaConstants.musiclist);
        music.setButtonTextHint(mContext.getResources().getString(R.string.pleasegetprog));
        music.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               new MusicPop(mContext,currentActivity, chooseMediaList,chooseMachineList, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        music.setButtonText("已选"+chooseMediaList.size()+"首" );
                    }
                });

            }
        });
        //设置开始日期
        startDate = (ButtonBox) findViewById(R.id.start_date);
        startDate.setName(ChinaConstants.startdate);
        startDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext,ChinaConstants.startdate,model,startDate.getButton());
            }
        });
        //设置结束日期
        endDatess = (ButtonBox) findViewById(R.id.end_date);
        endDatess.setName(ChinaConstants.enddate);
        endDatess.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {    new TimePop(mContext,ChinaConstants.enddate,model,endDatess.getButton());
                LogUtils.setLog(mTag,"结束日期"+model.getEnddate());
            }
        });
        //设置播放时长
        playtimelength = (ButtonBox) findViewById(R.id.play_time_length);
        playtimelength.setName(ChinaConstants.playtimelength);
        playtimelength.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePop(mContext,ChinaConstants.playtimelength,model,playtimelength.getButton());
            }
        });
        //设置循环次数
        cycletimes = (SpinnerBox) findViewById(R.id.cycle_times);
        cycletimes.setName(ChinaConstants.playtimelength);
        cycletimes.setSpinner(getResources().getStringArray(R.array.cycle));
        cycletimes.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
            model.setTimelength(s);
            }
        });
        //设置选择时长还是次数
        rg = (RadioGroup) findViewById(R.id.rg);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                if (radioButton.getText().toString().contains("时长")) {
                    model.setTimelengthtype(Constring.FileBroadTimeLength);
                    selectHideOrShow(Integer.parseInt(Constring.FileBroadTimeLength));
                    LogUtils.setLog(mTag,"设置时长类型为秒");

                } else {
                    model.setTimelengthtype(Constring.FileBroadCycle);
                    selectHideOrShow(Integer.parseInt(Constring.FileBroadCycle));
                    LogUtils.setLog(mTag,"设置时长类型为次");
                }
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

    private void goToSelectWeek() {
        DialogWeekSelect dialogWeekSelect = new DialogWeekSelect(mContext, model,isFirst, new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                execMode.setSpinnerRightText(s.replace(ChinaConstants.WeekDay,""));
            }
        });
        dialogWeekSelect.show();
    }

    //提交按钮的功能
    private void submitFunction() {
        if (edTaskname.getTaskname() != null&&chooseMachineList.size() > 0&&chooseMediaList.size()>0) {
            model.setTaskname(edTaskname.getTaskname());
            if(bundle!=null){
                try {
                    taskMainMethod.putFileBroadTaskRefresh(model,chooseMachineList,chooseMediaList );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {

                if(model.getTimelength().contains(":")) {//格式为11:11:11时
                    model.setTimelength(new TaskMainMethod(mContext).setTimeLengthToSecond(model.getTimelength()));
                }
                try {
                    taskMainMethod.postFileBroadTask(model,chooseMachineList,chooseMediaList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            finish();
        } else if(edTaskname.getTaskname() == null){
            showToast(ChinaConstants.please_write_taskname);
        } else if(chooseMachineList.size()>0&&chooseMediaList.size()==0){
                showToast("请先选择媒体" );
                LogUtils.setLog(mTag,"选择的终端数量"+chooseMachineList.size());
            }else if(chooseMediaList.size()>0&&chooseMachineList.size()==0){
                LogUtils.setLog(mTag,"选择的媒体数量"+chooseMediaList.size());
                showToast("请先选择终端");
            }
        }


    private void selectHideOrShow(int i){
        if(i==Integer.parseInt(Constring.FileBroadCycle)){
            playtimelength.setVisibility(View.GONE);
            cycletimes.setVisibility(View.VISIBLE);
            cycletimes.setName(ChinaConstants.cycletime);

        }else{
            playtimelength.setVisibility(View.VISIBLE);
            playtimelength.setName(ChinaConstants.playtimelength);
            cycletimes.setVisibility(View.GONE);
        }
    }


    private void initModel() {

        if(bundle==null){
            LogUtils.setLog(mTag,"新添加的model"+ TimeUtils.getTime());
            TimeUtils.getDate();
        }else {
            model = (TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL);
            LogUtils.setLog(mTag,"旧model填充"+model.getTimelength());
            model.setTasktype(2);
            model.setCmdargs("44");
            model.setLiveterminalname("4545");
            model.setSechename("ds");
            model.setMedianame("sdd");
        }
    }

    private void initView() {
        initModel();
        edTaskname.setEdit(model.getTaskname());
        startDate.setButtonText(model.getStartdate());
        endDatess.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        volumeView.setVolume(model.getVolume());
        initTimeLength();
        initSpinner();
        if(bundle!=null){
            try {
                new TaskMainMethod(mContext). getMachineListFromServer(Integer.parseInt(model.getTaskid()),chooseMachineList,terminal);
            } catch (IOException e) {
                e.printStackTrace();
            }try {
                getMusicListFromSever(Integer.parseInt(model.getTaskid()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        terminalsize=chooseMachineList.size();
    }
    private void initTimeLength(){
        selectHideOrShow(Integer.parseInt(model.getTimelengthtype()));
        if(model.getTimelengthtype().equals(Constring.FileBroadCycle)){
            rb_woman.setChecked(true);
            cycletimes.setSpinnerSelected(model.getTimelength());
        }else{
            rb_man.setChecked(true);
            if(model.getTimelength().contains(":")){
                playtimelength.setButtonText(model.getTimelength());
            }else{
                playtimelength.setButtonText(TaskMainMethod.timelengthSecondToTime(model.getTimelength()).toString());
            }

        }
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





    private synchronized void getMusicListFromSever(int i) throws IOException {
        String url =   Constant.serveraddress  + Constant.getTaskMusics + "/" + model.getTaskid();
        RequestManger.getInstance().get(url, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    chooseMediaList.clear();
                    if(responseData.getData().get(0).getName()!=null){
                        for (int j = 0; j < responseData.getData().size(); j++) {
                            chooseMediaList.add(responseData.getData().get(j));
                        }
                    }


                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            music.setButtonText("已选"+chooseMediaList.size()+"首" );
                        }
                    });
                }
            }
            @Override
            public void onFailed(int code, String message) {
            }
        });
    }

    //上传终端
    private void postTaskTerminal(String task_id, final String group_id, String terminalid) throws IOException {
        MyRequestBuilder requestdata = new MyRequestBuilder(mContext);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", task_id);
        map.put("groupid", group_id);//分组名称
        map.put("terminalid", terminalid);
        map.put("area", "255");
        requestdata.setUrl(Constant.postTaskTerminal);
        requestdata.setNeedToken(true);
        requestdata.setBodyMap(map);
        RequestManger.getInstance().postHashMap(requestdata, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag,"上传终端"+terminalsize);
                if (terminalsize < chooseMachineList.size()) {

                    try {
                        postTaskTerminal(model.getTaskid(), "0" + "", chooseMachineList.get(terminalsize).getId() + "");//
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else if(terminalsize==chooseMachineList.size()){
                    finish();
                }
                terminalsize++;
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "zhongduanshangchuanshibai" + message);
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        showToast(getString(R.string.upfialed));
                    }
                });
            }
        });
    }

}