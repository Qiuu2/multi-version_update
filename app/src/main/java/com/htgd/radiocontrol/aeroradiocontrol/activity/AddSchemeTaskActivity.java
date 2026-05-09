package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

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
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TimeUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
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
import java.util.HashMap;

import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2021/3/3:9:46
 * 邮箱：535708929
 * 说明：添加作息方案任务
 */
public class AddSchemeTaskActivity extends BaseActivity {

    private ButtonBox playtimelength, playTime;
    private Context mContext;
    private SpinnerBox cycletimes;
    private RadioGroup rg;
    private TaskGuangboModel model;
    private String mTag = "AddSchemeTaskActivity";
    private SpinnerBox music,preopen,priority,execMode,sendmod;
    private ArrayList<MusicInfoModel> musicList = new ArrayList<>();
    private String[] musicLists;
    private TitleLayout titleView;
    private Editbox edTaskname;
    private RadioButton rb_man, rb_woman;
    private TaskGuangboModel modelFromListChange;
    private ArrayList<MachineInfo> chooseMachineList=new ArrayList();
    private ButtonBox tasks,startDate,endDate,terminal;
    private int tasknum = 0;
    private VolumeView volumeView;
    private MusicInfoModel musicSelected;//添加作息方案处来的
    private int terminalsize=0;
    private TaskGuangboModel modelFromAdd;
    private TaskGuangboModel modelFromListAdd;
    private int listsize=0;
    private boolean isFirst=true;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_scheme_all;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        initModel();
        //设置任务名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.taskname);
        model.setTaskname(edTaskname.getTaskname());
        //设置头
        titleView = (TitleLayout) findViewById(R.id.title_layout);
        if (modelFromListChange == null) {
            titleView.settitle(ChinaConstants.add_scheme_task);
        } else {
            titleView.settitle(ChinaConstants.change_scheme_task);
        }

        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                submit();
                finish();
            }
        });
        titleView.setleftButton(new TitleLayout.Listener() {
            @Override
            public void right() {
                Intent intent = new Intent();
                intent.putExtra(CacheConstants.SchemeTaskModel, model);
                setResult(1, intent);
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
                        DialogWeekSelect dialogWeekSelect = new DialogWeekSelect(mContext, model,isFirst, new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                execMode.setSpinnerRightText(TaskMainMethod.setWeekDay(model.getExecmode()).replace(ChinaConstants.WeekDay,""));
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
        endDate = (ButtonBox) findViewById(R.id.end_date);
        endDate.setName(ChinaConstants.enddate);
        endDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePop(mContext, ChinaConstants.enddate, model, endDate.getButton());

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
        //设置播放时长
        playtimelength = (ButtonBox) findViewById(R.id.play_time_length);
        playtimelength.setName(ChinaConstants.playtimelength);
        playtimelength.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePop(mContext, ChinaConstants.playtimelength, model, playtimelength.getButton());

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
        //初始化时长和次数
        rb_man = (RadioButton) findViewById(R.id.man);
        rb_woman = (RadioButton) findViewById(R.id.woman);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                if (radioButton.getText().toString().contains("时长")) {
                    model.setTimelengthtype(Constring.FileBroadTimeLength);
                    selectHideOrShow(Integer.parseInt(Constring.FileBroadTimeLength));
                    LogUtils.setLog(mTag,"设置时长类型为秒");
                    playtimelength.setButtonText(model.getTimelength());

                } else {
                    model.setTimelengthtype(Constring.FileBroadCycle);
                    selectHideOrShow(Integer.parseInt(Constring.FileBroadCycle));
                    LogUtils.setLog(mTag,"设置时长类型为次");
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
                TimePop timePop = new TimePop(mContext, ChinaConstants.starttime, model, playTime.getButton());

            }
        });
        //设置音量
        volumeView  = (VolumeView) findViewById(R.id.volume_view);
        int volume = volumeView.getVolume(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                model.setVolume(Integer.parseInt(s));
            }
        });


        //设置任务提交数量
        tasks = (ButtonBox) findViewById(R.id.tasks);
        tasks.setName(ChinaConstants.tasknum);
        tasks.setButtonText(tasknum+"");
        tasks.setBtnShowOrHide(false);
       /* if(modelFromAdd!=null){
            titleView.setRightButton("完成", new TitleLayout.Listener() {
                @Override
                public void right() {
                    finish();
                }
            });
            tasks.setName(ChinaConstants.tasksubmit);
            tasks.setButtonTextHint("添加任务");
            tasks.setButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogUtils.setLog(mTag,"提交任务");
                  submit();
                }
            });
        }*/
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

    private void submit() {
        if (edTaskname.getTaskname() != null) {
            model.setTaskname(edTaskname.getTaskname());
        } else {
            showToast(ChinaConstants.please_write_taskname);
        }
        LogUtils.setLog(mTag,"提交作息任务");
        if(modelFromListChange==null) {
            LogUtils.setLog(mTag,"添加作息任务");
            //if (tasknum == 0 ) {
                LogUtils.setLog(mTag,"添加作息任务0");
                if(model.getTimelength().contains(":")||model.getTimelength().length()>7) {
                    LogUtils.setLog(mTag,"修改转了格式");
                    model.setTimelength(new TaskMainMethod(mContext).setTimeLengthToSecond(model.getTimelength()));
                }
          //  }
            if(chooseMachineList.size()>0) {
                try {
                    postTask();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                showToast(ChinaConstants.please_choose_machine);
            }
        }else{
            if(model.getTimelength().contains(":")||model.getTimelength().length()>7) {
                LogUtils.setLog(mTag,"添加转了格式");
                model.setTimelength(new TaskMainMethod(mContext).setTimeLengthToSecond(model.getTimelength()));
            }
            if(chooseMachineList.size()>0) {
                try {
                    putTaskRefresh();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                showToast(ChinaConstants.please_choose_machine);
            }
        }

    }

    private void initView() {

        if(modelFromListChange!=null) {
            LogUtils.setLog(mTag,"修改任务的名称"+model.getTaskname()+model.getName());
            edTaskname.setEdit(model.getName());
        }else {
            LogUtils.setLog(mTag,"youdu");
        }
        startDate.setButtonText(model.getStartdate());
        endDate.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        if(chooseMachineList.size()>0){
            terminal.setButtonText("已选"+chooseMachineList.size()+"台");
        }

        volumeView.setVolume(model.getVolume());
        tasknum=listsize;
        tasks.setButtonText(listsize+"");
        if(modelFromListChange!=null){
            LogUtils.setLog(mTag,"设置任务数量消失");
            tasks.setVisibility(View.INVISIBLE);
        }
        if(modelFromAdd==null ){
            try {
                new  TaskMainMethod(mContext). getMachineListFromServer(Integer.parseInt(model.getTaskid()),chooseMachineList,terminal);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        initTimeLength();
        initSpinner();

        terminalsize = chooseMachineList.size();
    }
    private void initTimeLength(){
        selectHideOrShow(Integer.parseInt(model.getTimelengthtype()));
        LogUtils.setLog(mTag,"时长类型"+model.getTimelengthtype());
        if(model.getTimelengthtype().equals(Constring.FileBroadCycle)){
            rb_woman.setChecked(true);
            cycletimes.setSpinnerSelected(model.getTimelength());
        }else{
            rb_man.setChecked(true);
            if(!model.getTimelength().toString().contains(":")){
                playtimelength.setButtonText(TaskMainMethod.timelengthSecondToTime(model.getTimelength()).toString());
            }else{
                playtimelength.setButtonText(model.getTimelength());
            }

        }
    }
    private void initSpinner() {
        if (model.getExecmode() == 127) {
            execMode.setSpinnerSelected("每天");
        } else if (model.getExecmode() == 0) {
            execMode.setSpinnerSelected("手动");
        } else {
            execMode.setSpinnerSelected(ChinaConstants.WeekDay);
            execMode.setSpinnerRightText(TaskMainMethod.setWeekDay(model.getExecmode()).replace(ChinaConstants.WeekDay,""));
        }
        preopen.setSpinnerSelected(model.getPrepower() + "");
        preopen.setSpinnerRightText(mContext.getResources().getString(R.string.times));
        priority.setSpinnerSelected(model.getPriority() + "");



    }

    //初始化模型
    private void initModel() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        model = new TaskGuangboModel("1", 2, 9, 80, 3, 2, TimeUtils.getDate(),
                TimeUtils.getDate(), 127, 1, "万志强", TimeUtils.getTime(), "22",
                "0", 1, "string", "110", 0, "44",
                32, 0, "string", 4800, 2);
        modelFromAdd = (TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL_SCHEME);

        if (modelFromAdd != null) {
            model=modelFromAdd;
            chooseMachineList = (ArrayList) bundle.getSerializable(CacheConstants.SCHEMECHOOSEMACHINE);
            musicSelected = (MusicInfoModel) bundle.getSerializable(CacheConstants.SCHEMECHOOSEMUSIC);
            LogUtils.setLog(mTag, "需操作的属性" + chooseMachineList.size());
            LogUtils.setLog(mTag, "添加作息方案新添加的model" + model.getTimelength());
        }

        modelFromListAdd = (TaskGuangboModel) bundle.getSerializable(CacheConstants.add_scheme_task_from_list);
        if (modelFromListAdd != null) {
            model=modelFromListAdd;
            listsize = (int) bundle.getSerializable(CacheConstants.add_scheme_task_from_list_size);
            model.setSechename(model.getInfo());
            model.setTimelength(model.getLength()+"");
            model.setTimelengthtype(model.getLengthtype()+"");
            model.setCmdargs("11");
            model.setTasktype(1);
            LogUtils.setLog(mTag, "方案任务列表处来的model时长" + model.getTimelength());
        }

        modelFromListChange = (TaskGuangboModel) bundle.getSerializable(CacheConstants.change_schemetask);
        if (modelFromListChange != null) {
            model=modelFromListChange;
            model.setSechename(model.getInfo());
            model.setTaskname(model.getName());
            model.setTimelengthtype(model.getLengthtype()+"");
            model.setCmdargs("11");
            model.setLiveterminalname("11");
            model.setTimelength( model.getLength()+"");
            model.setTasktype(1);
            LogUtils.setLog(mTag, "方案任务列表处修改来的" + model.getTaskname()+model.getTimelength());
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



    private void putTaskRefresh() throws IOException {
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.putSchemeTask);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().putHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {

                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getState())) {

                        LogUtils.setLog(mTag, "更新任务提交成功" + tmodel.getState()  +"已选终端"+chooseMachineList.size());

                        if(chooseMachineList.size()>0) {
                            try {

                                deleteTaskTerminal(Integer.parseInt(model.getTaskid()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }else{
                            finish();
                        }

                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //删除任务终端
    public synchronized void deleteTaskTerminal(int taskid) throws IOException {

        HashMap<String, String> map = new HashMap<>();
        map.put("id", "" + taskid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.deleteTaskTerminal);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().deleteHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "删除任务终端成功");
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);

                    if (tmodel != null   ) {
                        if (chooseMachineList.size() > 0) {
                            try {
                                terminalsize=0;
                                LogUtils.setLog(mTag, "上传更新的终端"+chooseMachineList.get(terminalsize).getGroupid());
                                LogUtils.setLog(mTag, "总共多少终端"+chooseMachineList.size());
                                postTaskTerminal(model.getTaskid(), chooseMachineList.get(terminalsize).getGroupid() + "", chooseMachineList.get(terminalsize).getId() + "");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }



            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    private void postTask() throws IOException {
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postSchemeTask);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {

                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getTaskid())) {
                        model.setTaskid(tmodel.getTaskid());
                        LogUtils.setLog(mTag, "添加任务提交成功" + tmodel.getState() + "id" + tmodel.getTaskid()+"已选终端"+terminalsize);
                        tasknum++;
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("添加方案任务成功");
                                tasks.setButtonText(tasknum + "");
                                edTaskname.setEdit("");
                               /* if(modelFromListChange==null) {
                                    LogUtils.setLog(mTag, "清空已选设备"  );
                                    chooseMachineList.clear();
                                    terminalsize=0;
                                }*/
                            }
                        });
                        if(modelFromListChange==null){
                            terminalsize=0;
                        }
                        if(chooseMachineList.size()>0) {
                            try {
                                LogUtils.setLog(mTag,"");
                                postTaskTerminal(model.getTaskid(), chooseMachineList.get(terminalsize ).getGroupid()+"", chooseMachineList.get(terminalsize).getId() + "");//
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
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
        LogUtils.setLog(mTag,"上传终端"+terminalsize);
        RequestManger.getInstance().postHashMap(requestdata, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                terminalsize++;
                LogUtils.setLog(mTag,"上传终端chengg"+terminalsize);
                if (terminalsize < chooseMachineList.size()) {
                    try {
                        postTaskTerminal(model.getTaskid(), chooseMachineList.get(terminalsize).getGroupid()+ "", chooseMachineList.get(terminalsize).getId() + "");//
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    finish();
                }

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
                        LogUtils.setLog(mTag, "媒体名称" +musicList.get(i).getName()+"媒体id"+musicList.get(i).getMediaid());
                    }

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.setLog(mTag, "媒体个数" + musicLists.length);
                            music = (SpinnerBox) findViewById(R.id.scheme_music);
                            music.setSpinner(musicLists);
                            music.setName(ChinaConstants.taskmusic);
                            if(musicSelected!=null){
                                music.setSpinnerSelected(model.getMedianame());
                            }
                            music.setListener(new SpinnerBox.GetResultListener() {
                                @Override
                                public void getResult(String s) {
                                    model.setMedianame(s);
                                    for (int i = 0; i < musicList.size(); i++) {
                                        if(musicList.get(i).getName().equals(s)){
                                            model.setMediaid(musicList.get(i).getMediaid());
                                        }
                                    }
                                    LogUtils.setLog(mTag,"媒体名称和id"+s+model.getMediaid());
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



}
