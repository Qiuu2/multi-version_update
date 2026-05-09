package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.app.Activity;
import android.content.Context;

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.SeverStateRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskStateRsp;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by wzq on 2017-08-16.
 *  用于处理任务状况
 */
public class TaskManageUtils {
    private Context mContext;
    public TaskManageUtils(Context  context){
        this.mContext = context ;
    }
    // 启用或停止作息任务
    public  void startOrStopProject(final String taskname,final int state, final onTaskLister lister) throws IOException {
        MyRequestBuilder requestBuilder = new MyRequestBuilder(mContext);
        requestBuilder.setNeedToken(true);
        requestBuilder.setUrl(Constant.postChangeTaskStatu);
        requestBuilder.setBodyMap(new HashMap<String, String>() {{
            put("sechename", taskname);
            put("state", state+"" );
        }});
        RequestManger.getInstance().postHashMap(requestBuilder, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskStateRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskStateRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    int state = responseData.getData().get(0).getState();
                    switch ( state) {
                        case ErrorCode.ChangeSucess:
                            LogUtils.setLog("change sucess");
                            LogUtils.setLog("the state is"+state);
                            lister.onChangeSucess();
                            break;
                        case ErrorCode.TheStateIsSame:
                            lister.onTheSameStatu();
                            LogUtils.setLog("meibian");
                            break;
                        default:
                            ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                            break;
                    }
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg );
                }
            }

            @Override
            public void onFailed(int code, String message) {
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    lister.onRetry();
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
        });
    }
    // 运行或停止方案
    public void runOrStopTask(final String taskid,final int state, final onTaskLister lister) throws IOException {
        MyRequestBuilder requestBuilder = new MyRequestBuilder(mContext);
        requestBuilder.setNeedToken(true);
        requestBuilder.setUrl(Constant.postRunOrStopTask);
        requestBuilder.setBodyMap(new HashMap<String, String>() {{
            put("id", taskid+"");
            put("state", state + "");
        }});
        RequestManger.getInstance().postHashMap(requestBuilder, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskStateRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskStateRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    int state = responseData.getData().get(0).getState();
                    switch (state) {
                        case ErrorCode.ChangeSucess:
                            lister.onChangeSucess();
                            break;
                        case ErrorCode.TheStateIsSame:
                            lister.onTheSameStatu();
                            break;
                        default:
                           // ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                            break;
                    }
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }

            @Override
            public void onFailed(int code, String message) {
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    lister.onRetry();
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
        });
    }
    // 启用或在停止执行方案
    public void useOrStopTask(final String taskid,final int state, final onTaskLister lister) throws IOException {
        MyRequestBuilder requestBuilder = new MyRequestBuilder(mContext);
        requestBuilder.setNeedToken(true);
        requestBuilder.setUrl(Constant.postUserOrStopTask);
        requestBuilder.setBodyMap(new HashMap<String, String>() {{
            put("id", taskid+"");
            put("state", state + "");
        }});
        RequestManger.getInstance().postHashMap(requestBuilder, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskStateRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskStateRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    int responseState = responseData.getData().get(0).getState();
                    switch (responseState) {
                        case ErrorCode.ChangeSucess:
                            LogUtils.setLog("change sucess");
                            LogUtils.setLog("the state is"+responseState);
                            lister.onChangeSucess();
                            break;
                        case ErrorCode.TheStateIsSame:
                            LogUtils.setLog("the sanme state");
                            lister.onTheSameStatu();
                            break;
                        default:
                             ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                            break;
                    }
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
            @Override
            public void onFailed(int code, String message) {
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    lister.onRetry();
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
        });
    }

    //更改任务音量
    public void setTaskVoice(final String taskid,final int state, final onTaskLister lister) throws IOException {
        MyRequestBuilder requestBuilder = new MyRequestBuilder(mContext);
        requestBuilder.setNeedToken(true);
        requestBuilder.setUrl(Constant.postSetTaskVoice);
        requestBuilder.setBodyMap(new HashMap<String, String>() {{
            put("id", taskid );
            put("state", state + "");
        }});
        RequestManger.getInstance().postHashMap(requestBuilder, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskStateRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskStateRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    int state = responseData.getData().get(0).getState();
                    switch (state) {
                        case ErrorCode.ChangeSucess:
                             ToastUtil.showToastOnUIThread((Activity)mContext, "调节成功");
                            lister.onChangeSucess();
                            break;
                        case ErrorCode.TheStateIsSame:
                             ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.theStateIsTheSame);
                            lister.onTheSameStatu();
                            break;
                        default:
                            ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                            break;
                    }
                } else {
                   ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
            @Override
            public void onFailed(int code, String message) {
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    lister.onRetry();
                } else {
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.Connect_ServerFailedMsg);
                }
            }
        });
    }


    //获取SDK所连服务器状态里的端口号
    public void getServeNomber(final onGetLister lister) throws IOException {
        String url = PreferencesUtil.getInstance().getField("serverAddress",mContext)+ Constant.getServerState;
        RequestManger.getInstance().get(url, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                SeverStateRsp responseData = JsonUtil.getInstance().deSerializeString(response,SeverStateRsp.class);
                if(responseData!=null && responseData.getData()!=null  && responseData.getData().size()>0  && responseData.getData().get(0).getCtrlport()!=0){
                    Constant.SDK_SERVER_NOMBER = responseData.getData().get(0).getCtrlport();
                    lister.onGetSucess();
                }else{
                     ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.GET_SERVER_NOMBER_FAILED);
                    lister.onGetFaid();
                }
            }

            @Override
            public void onFailed(int code, String message) {
                lister.onGetFaid();
                ToastUtil.showToastOnUIThread((Activity)mContext, ErrorCode.GET_SERVER_NOMBER_FAILED);
            }
        });
    }
    public interface   onTaskLister{
        void onChangeSucess();
        void onTheSameStatu();
        void onRetry();
    }

    public interface onGetLister{
        void onGetSucess();
        void onGetFaid();
    }
}
