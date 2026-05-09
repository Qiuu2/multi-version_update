package com.htgd.radiocontrol.aeroradiocontrol.httptask;


import static com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants.EveryDay;
import static com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants.WeekDay;

import android.app.Activity;
import android.content.Context;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;

/**
 * 作者：wzq
 * 时间：2021/4/9:13:35
 * 邮箱：535708929
 * 说明：网络任务里的公共方法
 */
public class TaskMainMethod {

    private Context mContext;
    private String mTag = "TaskMainMethod";
    private ArrayList<MachineInfo> chooseMachineList;
    private int terminalsize = 0;
    private int mediaSize = 0;
    private ArrayList<MusicInfoModel> chooseMediaList;
    private TaskGuangboModel model;

    public TaskMainMethod(Context context) {
        this.mContext = context;
    }

    public static StringBuffer timelengthSecondToTime(String s) {
        int a = Integer.parseInt(s) / 3600;
        int b = Integer.parseInt(s) % 3600 / 60;
        int c = Integer.parseInt(s) % 60;
        String a1 = a + "";
        String b1 = b + "";
        String c1 = c + "";
        if (a < 10) {
            a1 = "0" + a;
        }
        if (b < 10) {
            b1 = "0" + b;
        }
        if (c < 10) {
            c1 = "0" + c;
        }
        StringBuffer time = new StringBuffer(a1 + ":" + b1 + ":" + c1);
        return time;
    }

    //设置星期几的显示
    public static String setWeekDay(int execmode) {
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
        String ji = WeekDay;
        for (int i = 0; i < 7; i++) {
            if (day.get(i) == 1) {
                if (i == 0) {
                    ji = ji + ChinaConstants.SunDay;
                } else if (i == 1) {
                    ji = ji + ChinaConstants.MonDay;
                } else if (i == 2) {
                    ji = ji + ChinaConstants.TuesDay;
                } else if (i == 3) {
                    ji = ji + ChinaConstants.WednesDay;
                } else if (i == 4) {
                    ji = ji + ChinaConstants.ThursDay;
                } else if (i == 5) {
                    ji = ji + ChinaConstants.FriDay;
                } else if (i == 6) {
                    ji = ji + ChinaConstants.SaturDay;
                }
            }
        }
        LogUtils.setLog("星期文字长度" + ji.length());
        if (ji.length() == 9) {
            ji = EveryDay;
        } else if (ji.length() == 2) {
            ji = ChinaConstants.Mannual;
        }
        return ji;
    }

    public String setTimeLengthToSecond(String lengths) {
        String time;
        StringBuffer a = new StringBuffer(lengths);
        StringBuffer b = new StringBuffer(lengths);
        StringBuffer c = new StringBuffer(lengths);
        time = Integer.parseInt(a.substring(0, 2)) * 3600 + Integer.parseInt(b.substring(3, 5)) * 60 + Integer.parseInt(c.substring(6, 8)) + "";
        LogUtils.setLog(mTag, time + "时长秒数");
        return time;
    }

    public static String setTimeLength(int lengths, int lengthunit) {
        String time = "";

        if (lengthunit == Integer.parseInt(IntConstans.FileBroadCycle)) {
            time = lengths + "次";

        } else {
            if (lengths / 60 < 1) {
                time = lengths % 60 + "s";
            } else if (lengths / 3600 < 1) {
                time = lengths / 60 + "m" + lengths % 60 + "s";
            } else if (lengths / 86400 < 1) {
                time = lengths / 3600 + "h" + (lengths % 3600) / 60 + "m" + (lengths % 3600) % 60 + "s";
            } else if (lengths / 2592000 < 1) {
                time = lengths / 86400 + "d" + (lengths % 86400) / 3600 + "h" + ((lengths % 86400) % 3600) / 60 + "m" + ((lengths % 86400) % 3600) % 60 + "s";
            }
        }
        return time;
    }

    public synchronized void getMachineListFromServer(int i, final ArrayList<MachineInfo> chooseMachineList,final ButtonBox terminal ) throws IOException {

        String url = PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskMachines + "/" + i;
        RequestManger.getInstance().get(url, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                final MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (responseData.getData().get(0).getName() != null) {
                                for (int j = 0; j < responseData.getData().size(); j++) {
                                    chooseMachineList.add(responseData.getData().get(j));
                                }
                            }
                            terminal.setButtonText("已选" + chooseMachineList.size() + "台");
                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //删除任务终端
    public synchronized void deleteTaskTerminal(final int taskid) throws IOException {

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
                        if (chooseMachineList.size() > 0) {
                            try {
                                LogUtils.setLog(mTag, "上传更新的终端"+"groupid"+chooseMachineList.get(terminalsize).getGroupid());
                                postTaskTerminal(taskid+"", chooseMachineList.get(terminalsize).getGroupid() + "", chooseMachineList.get(terminalsize).getId() + "");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    } //删除任务媒体

    public synchronized void deleteMedia(final int taskid) throws IOException {

        HashMap<String, String> map = new HashMap<>();
        map.put("id", "" + taskid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.deleteTaskMedia);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().deleteHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "删除任务媒体成功");
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {


                    if (chooseMediaList.size() > 0) {
                        try {
                            LogUtils.setLog(mTag, "上传更新的媒体");
                            postSetTaskMusic(taskid+"", chooseMediaList.get(mediaSize).getMediaid() + "");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    public void putTaskRefresh(final TaskGuangboModel model, final ArrayList<MachineInfo> chooseMachineList) throws IOException {
        this.model = model;
        this.chooseMachineList = chooseMachineList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.putTaskInfo);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().putHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getState())) {
                        LogUtils.setLog(mTag, "更新任务提交成功" + tmodel.getState());
                        try {
                            deleteTaskTerminal(Integer.parseInt(model.getTaskid()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (chooseMachineList.size() > 0) {
                            try {
                                deleteTaskTerminal(Integer.parseInt(model.getTaskid()));
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

    //提交任务
    public void postTask(final TaskGuangboModel model, final ArrayList<MachineInfo> chooseMachineList) throws IOException {
        this.model=model;
        this.chooseMachineList = chooseMachineList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postTaskInfo);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if(tmodel.getTaskid()!=null){
                        model.setTaskid(tmodel.getTaskid());
                    }
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getState())) {
                        LogUtils.setLog(mTag, "任务提交成功" + tmodel.getState());
                       if(tmodel.getState()=="0") {
                           if (chooseMachineList.size() > 0) {
                               try {
                                   LogUtils.setLog(mTag, "上传任务终端" + chooseMachineList.get(terminalsize).getId() + chooseMachineList.get(terminalsize).getName());
                                   postTaskTerminal(model.getTaskid(), chooseMachineList.get(0).getGroupid() + "", chooseMachineList.get(0).getId() + "");//

                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                           }
                       }else if(tmodel.getState()=="15"){
                           ToastUtil.showToast(mContext,"任务名重复");
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
    public void postTaskTerminal(final String task_id, final String group_id, String terminalid) throws IOException {
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
                terminalsize++;
                LogUtils.setLog(mTag, "上传终端" + terminalsize);
                if (terminalsize < chooseMachineList.size()) {
                    try {
                        postTaskTerminal(task_id, chooseMachineList.get(terminalsize).getGroupid() + "", chooseMachineList.get(terminalsize).getId() + "");//
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (terminalsize == chooseMachineList.size()) {
                    //ToastUtil.showToast(mContext,mContext.getResources().getString(R.string.taskrefreshsuccess));

                }

            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "zhongduanshangchuanshibai" + message);
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        ToastUtil.showToast(mContext, mContext.getString(R.string.upfialed));
                    }
                });
            }
        });
    }

    //任务媒体绑定
    public void postSetTaskMusic(final String taskid, String mediaid) throws IOException {
        LogUtils.setLog(mTag, "添加任务媒体" + mediaid);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", taskid);
        map.put("mediaid", mediaid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.setTaskMusic);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag, "媒体添加成功");
                mediaSize++;
                if (mediaSize < chooseMediaList.size()) {
                    try {
                        postSetTaskMusic(taskid, chooseMediaList.get(mediaSize).getMediaid() + "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {
            }
        });
    }

    //上传tts任务
    public void postTtsTask(final TaskGuangboModel model, final ArrayList<MachineInfo> chooseMachineList) throws IOException {
        this.chooseMachineList = chooseMachineList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postTtsTaskInfo);
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
                        LogUtils.setLog(mTag, "添加任务提交成功" + tmodel.getState() + "id" + tmodel.getTaskid());
                        if (chooseMachineList.size() > 0) {
                            LogUtils.setLog(mTag, "已选终端" + chooseMachineList.size());
                            try {

                                postTaskTerminal(model.getTaskid(), chooseMachineList.get(terminalsize).getGroupid() + "", chooseMachineList.get(terminalsize).getId() + "");//
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

    //更新tts任务
    public void putTtsTaskRefresh(final TaskGuangboModel model, ArrayList<MachineInfo> chooseMachineList) throws IOException {
        this.model = model;
        this.chooseMachineList = chooseMachineList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postTtsTaskInfo);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().putHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {

                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    LogUtils.setLog(mTag, "更新任务提交成功" + tmodel.getState());


                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getState())) {
                        try {
                            deleteTaskTerminal(Integer.parseInt(model.getTaskid()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //更新文件广播任务
    public void putFileBroadTaskRefresh(final TaskGuangboModel model, final ArrayList<MachineInfo> chooseMachineList, final ArrayList<MusicInfoModel> chooseMediaList) throws IOException {
        this.chooseMachineList = chooseMachineList;
        this.chooseMediaList = chooseMediaList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postTaskInfo);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().putHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {

                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getState())) {

                        LogUtils.setLog(mTag, "更新任务提交成功" + tmodel.getState() + "id" + tmodel.getTaskid());
                        if (chooseMachineList.size() > 0) {
                            try {
                                deleteTaskTerminal(Integer.parseInt(model.getTaskid()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        if (chooseMediaList.size() > 0) {
                            try {
                                deleteMedia(Integer.parseInt(model.getTaskid()));
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

    public synchronized void deleteTask(int taskid) throws IOException {

        HashMap<String, String> map = new HashMap<>();
        map.put("id",""+taskid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.deleteTask);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().deleteHaspMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTag,"删除任务成功");
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getTaskid())) {

                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

            }
        });
    }

    //
    public void postFileBroadTask(final TaskGuangboModel model, final ArrayList<MachineInfo> chooseMachineList, final ArrayList<MusicInfoModel> chooseMediaList) throws IOException {
        this.chooseMachineList = chooseMachineList;
        this.chooseMediaList = chooseMediaList;
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postTaskInfo);
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
                        LogUtils.setLog(mTag, "添加任务提交成功" + tmodel.getState() + "id" + tmodel.getTaskid());
                        if (chooseMachineList.size() > 0) {
                            try {
                                postTaskTerminal(model.getTaskid(), chooseMachineList.get(terminalsize).getGroupid() + "", chooseMachineList.get(terminalsize).getId() + "");//
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (chooseMediaList.size() > 0) {
                            try {
                                postSetTaskMusic(model.getTaskid(), chooseMediaList.get(mediaSize).getMediaid() + "");
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

}
