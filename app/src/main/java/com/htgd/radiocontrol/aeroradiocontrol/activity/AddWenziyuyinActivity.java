package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TtsTaskContentModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TtsTaskContentRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
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
 * 时间：2020/11/16:10:31
 * 邮箱：535708929
 * 说明：添加文字语音
 */
public class AddWenziyuyinActivity extends BaseActivity {
    private TaskGuangboModel model;
    private String mTag = "AddWenziyuyinActivity";
    private TitleLayout titleView;
    private Editbox edTaskname, content;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<MachineInfo>();
    private AddWenziyuyinActivity mContext;
    private SpinnerBox gender, preopen,  priority, execMode,  playtimelength,speed,hostlist;
    private ButtonBox playTime ,startDate,endDatess,terminal;
    private VolumeView volumeView;
    private Bundle bundle;
    private ArrayList<TtsTaskContentModel> ttsContentModelList;
    private int ttsTerminalId=-1;
    private Boolean isFirst=new Boolean(true);
    private TaskMainMethod taskMainMethod;
    private String[] aa=new String[VariableConstant.ttsHostList.size()];
    private ArrayList<MachineInfo> hostMachineList = new ArrayList<MachineInfo>();



    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_wenziyuyin;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
          taskMainMethod = new TaskMainMethod(mContext);
        model = new TaskGuangboModel("1", 2, 1, 66, 3, 1, "2021-12-30", "2021-12-30", 0, 17, "333", "11:20:20", "1", "1", 1, 54, 1, 0, " ");
        Intent intent = getIntent();
        bundle = intent.getExtras();


        titleView = (TitleLayout) findViewById(R.id.title_layout);
        //设置头右边
        if(bundle==null) {
            titleView.settitle(ChinaConstants.add_tts);
        }else{
            titleView.settitle(ChinaConstants.modify_tts);
        }
        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                if (edTaskname.getTaskname() != null&&content.getTaskname()!=null&&chooseMachineList.size() > 0) {
                    model.setTaskname(edTaskname.getTaskname());
                    model.setContent(content.getTaskname());
                    if (ttsTerminalId == -1&&bundle==null) {
                        showToast(ChinaConstants.NoTtsTerminal);
                    } else {
                        if (bundle != null) {
                            LogUtils.setLog(mTag, "可以更新任务 终端数"+chooseMachineList.size());
                            try {
                                taskMainMethod.putTtsTaskRefresh(model,chooseMachineList);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                taskMainMethod.postTtsTask(model,chooseMachineList);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    finish();
                } else if(edTaskname.getTaskname() == null){
                    showToast(ChinaConstants.please_write_taskname);
                }else if(content.getTaskname()==null){
                    showToast(ChinaConstants.please_write_content);
                }else if(chooseMachineList.size() == 0) {
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
        //设置任务名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.taskname);
        model.setTaskname(edTaskname.getTaskname());

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
            }
        });
        //设置执行模式
        execMode = (SpinnerBox) findViewById(R.id.exec_mode);
        execMode.setSpinner(getResources().getStringArray(R.array.exec_mode));
        execMode.setName(ChinaConstants.execmode);

        execMode.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                LogUtils.setLog(mTag,"执行模式点击了2"+isFirst);
                if (s.equals(ChinaConstants.WeekDay)) {
                    if(!isFirst) {
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
        //设置声音模式
        gender = (SpinnerBox) findViewById(R.id.gender);
        gender.setSpinner(getResources().getStringArray(R.array.gender));
        gender.setName(ChinaConstants.gender);
        gender.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                if (s.equals(mContext.getResources().getString(R.string.man))) {
                    LogUtils.setLog(mTag,"选了男声");
                    model.setMale(1);
                } else {
                    model.setMale(0);
                }
            }
        });

        //设置循环次数
        playtimelength = (SpinnerBox) findViewById(R.id.cycle_times);
        playtimelength.setName(ChinaConstants.cycletime);
        playtimelength.setSpinner(getResources().getStringArray(R.array.cycle));
        playtimelength.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                 model.setTimelength( s );
            }
        });
        //设置播放时间
        playTime = (ButtonBox) findViewById(R.id.start_time);
        playTime.setName(ChinaConstants.starttime);
        playTime.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext,ChinaConstants.starttime,model,playTime.getButton());
            }
        });
        //设置速率
        speed = (SpinnerBox) findViewById(R.id.speed);
        speed.setName(ChinaConstants.speed);
        speed.setSpinner(getResources().getStringArray(R.array.cycle));
        speed.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                model.setSpeed(Integer.parseInt(s) );
            }
        });
        //设置主机列表
        hostlist = (SpinnerBox) findViewById(R.id.hostlist);
        hostlist.setName(ChinaConstants.hostlist);

        for (int i = 0; i < VariableConstant.ttsHostList.size(); i++) {

            aa[i] = VariableConstant.ttsHostList.get(i).getName();
        }

        hostlist.setSpinner(aa);
        if(VariableConstant.ttsHostList.size()==0) {
            String[]ss= new String[1];
            ss[0]="无tts主机";
            hostlist.setSpinner(ss);
        }
        hostlist.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                for (int i = 0; i < hostMachineList.size(); i++) {
                    if (VariableConstant.ttsHostList.get(i).getName() == aa[i]) {
                        model.setCmd(VariableConstant.ttsHostList.get(i).getId());
                    }
                }

            }
        });

         //设置开始日期
        startDate = (ButtonBox) findViewById(R.id.start_date);
        startDate.setName(ChinaConstants.startdate);
        startDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePop timePop = new TimePop(mContext,ChinaConstants.startdate,model,startDate.getButton());
                //startDate.setButtonText(model.getStartdate());
                //  LogUtils.setLog(mTag,"开始日期"+model.getStartdate());
            }
        });
        //设置结束日期
        endDatess = (ButtonBox) findViewById(R.id.end_date);
        endDatess.setName(ChinaConstants.enddate);
        endDatess.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext,ChinaConstants.enddate,model,endDatess.getButton());
                // endDatess.setButtonText(model.getEnddate());
                LogUtils.setLog(mTag,"结束日期"+model.getEnddate());
            }
        });
        //设置文字内容
        content = (Editbox) findViewById(R.id.content);
        content.setTextName(ChinaConstants.content);
        model.setContent(content.getTaskname());

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

        initView();

    }
    private void initView() {
        initModel();
        edTaskname.setEdit(model.getTaskname());
        LogUtils.setLog(mTag,"任务id"+model.getTaskid());
        startDate.setButtonText(model.getStartdate());
        endDatess.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        volumeView.setVolume(model.getVolume());
        if(bundle!=null){
            try {
                LogUtils.setLog(mTag,"获取文字内容");
                getTtsTaskContents();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
               taskMainMethod. getMachineListFromServer(Integer.parseInt(model.getTaskid()),chooseMachineList,terminal );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        initSpinner();
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

    private void getTtsTaskContents() throws IOException {
        RequestManger.getInstance().get( Constant.serveraddress + Constant.getTtsTaskContent + "/" + model.getTaskid(), new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TtsTaskContentRsp responseData = JsonUtil.getInstance().deSerializeString(response, TtsTaskContentRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ttsContentModelList = responseData.getData();
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.setLog(mTag,"获取的tts任务内容"+ttsContentModelList.get(0).getContents()+"速率"+ttsContentModelList.get(0).getSpeed()+"男女"+ttsContentModelList.get(0).getMale());
                            content.setEdit(ttsContentModelList.get(0).getContents());
                            model.setContent(ttsContentModelList.get(0).getContents());
                            model.setSpeed(ttsContentModelList.get(0).getSpeed());
                            model.setMale(ttsContentModelList.get(0).getMale());
                            speed.setSpinnerSelected(model.getSpeed()+"");
                            if(model.getMale()==1){
                                gender.setSpinnerSelected(mContext.getResources().getString(R.string.man));
                            }else{
                                gender.setSpinnerSelected(mContext.getResources().getString(R.string.woman));
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
        playtimelength.setSpinnerSelected(model.getTimelength());
       speed.setSpinnerSelected(model.getSpeed()+"");
        if(model.getMale()==1) {

            gender.setSpinnerSelected(mContext.getResources().getString(R.string.man));
        }else{
            gender.setSpinnerSelected(mContext.getResources().getString(R.string.woman));
        }
        if(VariableConstant.ttsHostList.size()>0) {
            for (int i = 0; i < VariableConstant.allMachineList.size(); i++) {
                if (model.getCmd() == VariableConstant.allMachineList.get(i).getId()) {
                    hostlist.setSpinnerSelected(VariableConstant.allMachineList.get(i).getName());
                }
            }
        }
    }


    private void initModel() {
        if(bundle==null){
            LogUtils.setLog(mTag,"新添加的model"+ TimeUtils.getTime());
        }else {
            LogUtils.setLog(mTag,"旧model填充");
            model = (TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL);
            model.setTasktype(IntConstans.keyWenZiYuYin);
            LogUtils.setLog(mTag,"旧model填充"+"renwuming"+model.getTaskname()+model.getVolume() );
        }
    }

    // 获取tts主机设备信息
    private synchronized void getMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelistAll  , new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {

                    for (int i = 0; i < responseData.getData().size(); i++) {
                        if(responseData.getData().get(i).getType()==22||responseData.getData().get(i).getType()==32) {//获取tts主机
                            ttsTerminalId=responseData.getData().get(i).getId();
                            LogUtils.setLog(mTag,"获取到tts主机id"+ttsTerminalId);
                            model.setCmd(ttsTerminalId);
                         }
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }
}
