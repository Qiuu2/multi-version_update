package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
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
 * 时间：2020/11/5:18:08
 * 邮箱：535708929
 * 说明：添加采播管理任务
 */
public class AddCaiboActivity extends BaseActivity {
    private Context mContext;
    private TaskGuangboModel model;
    private String mTag = "AddCaiboActivity";
    private TitleLayout titleView;
    private Editbox edTaskname;
    private ArrayList<MachineInfo> chooseMachineList = new ArrayList<MachineInfo>();
    private SpinnerBox simplerate, preopen, preopenCaibo, priority, execMode,   bitrate,hostlist,channel;
    private ButtonBox playTime, terminal, startDate, endDatess, playtimelength;
    private VolumeView volumeView;
    private Bundle bundle;
    private boolean isFirst=true;
    private TaskMainMethod taskMainMethod;
    private String[] aa=new String[VariableConstant.caiboHostList.size()];
    private ArrayList<MachineInfo> hostMachineList = new ArrayList<>();




    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_caibo;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        initModel();
          taskMainMethod = new TaskMainMethod(mContext);
        titleView = (TitleLayout) findViewById(R.id.title_layout);
        //设置任务名称
        edTaskname = (Editbox) findViewById(R.id.taskname);
        edTaskname.setTextName(ChinaConstants.taskname);

        //设置音量
        volumeView = (VolumeView) findViewById(R.id.volume_view);
        int volume = volumeView.getVolume(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                model.setVolume(Integer.parseInt(s));
            }
        });

        //设置头
        if (bundle == null) {
            titleView.settitle(ChinaConstants.add_caibo);
        } else {
            titleView.settitle(ChinaConstants.modify_caibo);
        }
        titleView.setRightButton(ChinaConstants.ok, new TitleLayout.Listener() {
            @Override
            public void right() {
                if (edTaskname.getTaskname() != null) {
                    model.setTaskname(edTaskname.getTaskname());
                } else {
                    showToast(ChinaConstants.please_write_taskname);
                }
                if (chooseMachineList.size() > 0&&edTaskname.getTaskname() != null) {
                    if (model.getTimelength().contains(":")) {//格式为11:11:11时
                        model.setTimelength(taskMainMethod.setTimeLengthToSecond(model.getTimelength()));
                    }else {
                        LogUtils.setLog(mTag,"时长本身格式正常");
                    }
                    if (bundle != null) {
                        try {
                            taskMainMethod.putTaskRefresh(model,chooseMachineList );
                       finish();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            taskMainMethod.   postTask(model,chooseMachineList );
                            finish();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else  if(edTaskname.getTaskname() == null) {
                    showToast(ChinaConstants.please_write_taskname);
                }else if(chooseMachineList.size() == 0) {
                    showToast("请先选择终端");
                }
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

        //设置预开采播电源
        preopenCaibo = (SpinnerBox) findViewById(R.id.caibo_preopen);
        preopenCaibo.setSpinner(getResources().getStringArray(R.array.preopen));
        preopenCaibo.setName(ChinaConstants.preopenCaibo);
        preopenCaibo.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                model.setCaiboprepower(Integer.parseInt(s));
                preopenCaibo.setSpinnerRightText(mContext.getResources().getString(R.string.times));
            }
        });
        //设置优先级
        priority = (SpinnerBox) findViewById(R.id.priority);
        priority.setSpinner(getResources().getStringArray(R.array.priority));
        priority.setName(ChinaConstants.priority);
        priority.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                LogUtils.setLog(mTag,"所选的优先级"+s);
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
                       DialogWeekSelect dialogWeekSelect = new DialogWeekSelect(mContext, model,isFirst, new Consumer<String>() {
                           @Override
                           public void accept(String s) throws Exception {
                               execMode.setSpinnerRightText(s.replace(ChinaConstants.WeekDay,""));
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
        playTime = (ButtonBox) findViewById(R.id.play_time);
        playTime.setName(ChinaConstants.starttime);
        playTime.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext, ChinaConstants.starttime, model, playTime.getButton());

            }
        });
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
        //设置开始日期
        startDate = (ButtonBox) findViewById(R.id.start_date);
        startDate.setName(ChinaConstants.startdate);
        startDate.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePop timePop = new TimePop(mContext, ChinaConstants.startdate, model, startDate.getButton());
            }
        });
        //设置主机列表
        hostlist = (SpinnerBox) findViewById(R.id.hostlist);
        hostlist.setName(ChinaConstants.hostlist);

        for (int i = 0; i < VariableConstant.caiboHostList.size(); i++) {

            aa[i] = VariableConstant.caiboHostList.get(i).getName();
        }
        hostlist.setSpinner(aa);
        if(VariableConstant.caiboHostList.size()==0) {
            String[]ss= new String[1];
            ss[0]="无采播管理主机";
            hostlist.setSpinner(ss);
        }
        hostlist.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                for (int i = 0; i < hostMachineList.size(); i++) {
                    if (VariableConstant.caiboHostList.get(i).getName() == aa[i]) {
                        model.setCmd(VariableConstant.caiboHostList.get(i).getId());
                    }
                }

            }
        });
        //设置结束日期
        endDatess = (ButtonBox) findViewById(R.id.end_date);
        endDatess.setName(ChinaConstants.enddate);
        endDatess.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePop timePop = new TimePop(mContext, ChinaConstants.enddate, model, endDatess.getButton());
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

        //设置比特率
        bitrate = (SpinnerBox) findViewById(R.id.bitrate);
        bitrate.setName(ChinaConstants.bitrate);
        bitrate.setSpinner(getResources().getStringArray(R.array.bitrate));
        bitrate.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                model.setBandrate(Integer.parseInt(s.replace("Kbp/s", "")));
            }
        });
        //设置通道
        channel = (SpinnerBox) findViewById(R.id.channel);
        channel.setName(ChinaConstants.channel);
        channel.setSpinner(getResources().getStringArray(R.array.channel));
        channel.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                model.setCmdargs( s );
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

    private void initView() {
       // edTaskname.setEdit(model.getTaskname());
        LogUtils.setLog(mTag,"renwuming"+model.getTaskname());
        startDate.setButtonText(model.getStartdate());
        endDatess.setButtonText(model.getEnddate());
        playTime.setButtonText(model.getStarttime());
        LogUtils.setLog(mTag, "initView时长" + model.getTimelength());
        if (!model.getTimelength().contains(":")) {
            playtimelength.setButtonText(TaskMainMethod.timelengthSecondToTime(model.getTimelength()).toString());
        } else {
            playtimelength.setButtonText(model.getTimelength());
        }
        volumeView.setVolume(model.getVolume());
        initSpinner();
        if (bundle != null) {
            try {
                taskMainMethod. getMachineListFromServer(Integer.parseInt(model.getTaskid()),chooseMachineList,terminal );
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    // 获取采播管理器信息
    private synchronized void getMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelistAll  , new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {


                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {

                    for (int i = 0; i < responseData.getData().size(); i++) {
                        if(responseData.getData().get(i).getType()==31 ) {//获取采播管理器
                            LogUtils.setLog(mTag, "获取到采播主机id" + responseData.getData().get(i).getId());
                            model.setCmd(responseData.getData().get(i).getId());
                        }
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
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
        LogUtils.setLog(mTag,"预开电源"+model.getPrepower());
        preopen.setSpinnerSelected(model.getPrepower() + "");
        preopen.setSpinnerRightText(mContext.getResources().getString(R.string.times));
        priority.setSpinnerSelected(model.getPriority() + "");
        preopenCaibo.setSpinnerSelected(model.getCaiboprepower() + "");
        preopenCaibo.setSpinnerRightText(mContext.getResources().getString(R.string.times));
        LogUtils.setLog(mTag,"比特率"+model.getBandrate());
        bitrate.setSpinnerSelected(model.getBandrate() + "Kbp/s");
        LogUtils.setLog(mTag,"通道"+model.getCmdargs());
        channel.setSpinnerSelected(model.getCmdargs() );
        if(VariableConstant.caiboHostList.size()>0) {
            for (int i = 0; i < VariableConstant.allMachineList.size(); i++) {
                if (model.getCmd() == VariableConstant.allMachineList.get(i).getId()) {
                    hostlist.setSpinnerSelected(VariableConstant.allMachineList.get(i).getName());
                }
            }
        }
    }






    private void initModel() {
        Intent intent = getIntent();
        bundle = intent.getExtras();
        if (bundle == null) {
            LogUtils.setLog(mTag, "新添加的model" + TimeUtils.getTime());
            model = new TaskGuangboModel("1", 15, 9, 80, 3, 0, TimeUtils.getDate(),
                    TimeUtils.getDate(), 127, 3, "任务名", TimeUtils.getTime(), TimeUtils.getTime(),
                    "1", 1, "string", "110", 0, "1",
                    32, 0, "string", 4800, 2);
        } else {
            LogUtils.setLog(mTag, "旧model填充");
            model = (TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL);
            model.setSamplerate(4800);
            LogUtils.setLog(mTag, "旧model填充" + "renwuming" + model.getTaskname()+"预开电源"+model.getPrepower()+"优先级"+model.getPriority()+model.getLevel());
            suppleModel();
        }
    }

    private void suppleModel() {
        model.setSechename("sd");
        model.setMedianame("dsd");
        model.setTimelengthtype("2");
        model.setCmdargs(model.getChannel()+"");
        model.setLiveterminalname("string");
        model.setLevel(model.getPriority());
        model.setTasktype(3);
    }


}

