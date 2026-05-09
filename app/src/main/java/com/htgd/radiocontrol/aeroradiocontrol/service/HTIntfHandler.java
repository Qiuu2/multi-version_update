package com.htgd.radiocontrol.aeroradiocontrol.service;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.htapplib.CallBackIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.ConnectActivity;
import com.htgd.radiocontrol.aeroradiocontrol.activity.LoginActivity;
import com.htgd.radiocontrol.aeroradiocontrol.activity.TaskRuningActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusBeRequestSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusCode;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusConnectString;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusFinish;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusReFresh;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRefuse;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStartSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStopPlay;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStopSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusWaitingForAnswer;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusStopShortTask;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;


/**
 * Created by wzq on 2017/9/29.
 */

public class HTIntfHandler implements CallBackIntf {

    private final String TAG = "HTIntf";
    private final String mTag = "HTIntf";
    private Context mContext;


    public HTIntfHandler(Context context) {
        this.mContext = context;
    }

    public void onsetdevicestate(int a) {
        LogUtils.setLog(mTag, "onsetdevicestate callback is ");
    }

    public void onreboot() {
        LogUtils.setLog(mTag, "onstartencode callback is ");
    }

    public void onsettime(short year, byte month, byte day, byte hour, byte minute, byte second) {
        LogUtils.setLog(mTag, "onsettime callback is " + year + month + day + hour + minute + second);
    }

    public void onsetvolume(int volume) {
        LogUtils.setLog(mTag, "onsetvolume callback is " + volume);
        //EventBus.getDefault().post(new EventBusGetVolume(volume));

    }

    //下载升级文件
    public void onupdate(String s) {
        LogUtils.setLog(mTag, "code" + "下载信息来了" + s);
        //  EventBus.getDefault().post(new EventBusDown(s));

    }

    //主动发起寻呼
    @Override
    public void onstartencode() {
        LogUtils.setLog(mTag, "onstartencode the call is start " + "start call  by  my self");
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(Constring.playstyle, Constring.caller);
                intent.setClass(mContext, ConnectActivity.class);
                ((Activity) mContext).startActivity(intent);

            }
        });
    }

    //主动停止寻呼
    @Override
    public void onstopencode() {
        LogUtils.setLog(TAG, "onstopencode the call is stop ");

        ((Activity) mContext).finish();
        EventBus.getDefault().post(new EventBusFinish("播放结束"));
    }


    //被寻呼开始
    @Override
    public void onstartplay(final String s) {
        LogUtils.setLog(mTag, "onstartplay the call is start " + "started call  by others");
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(Constring.CALER_NAME, s);
                intent.putExtra(Constring.playstyle, Constring.called);
                intent.setClass(mContext, ConnectActivity.class);
                ((Activity) mContext).startActivity(intent);
            }
        });
    }

    //被寻呼点播结束
    @Override
    public void onstopplay() {
        LogUtils.setLog(mTag, "onstopplay the call is stopping ");
        Cons.chooseMachine.clear();
        Cons.chooseProgList.clear();
        EventBus.getDefault().post(new EventBusStopPlay(""));//
    }

    //开始对讲
    @Override
    public void onstartspeech(final String s) {

        LogUtils.setLog(mTag, "onstartspeech some body is calling you" + "start speech" + s);
        EventBus.getDefault().post(new EventBusStartSpeech("some body is calling you"));
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(Constring.SPEECH_CALER_NAME, s);
                intent.putExtra(Constring.playstyle, Constring.speak);
                intent.setClass(mContext, ConnectActivity.class);
                ((Activity) mContext).startActivity(intent);
            }
        });
    }

    //停止对讲
    @Override
    public void onstopspeech() {
        LogUtils.setLog(TAG, "onstopspeech the call is stopping ");
        EventBus.getDefault().post(new EventBusStopSpeech());//
    }

    //开始执行快捷任务
    @Override
    public void onstartshortcuttask(String s) {
        LogUtils.setLog(TAG, "onstartshortcuttask the shortcuttask is starting ");
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setClass(mContext, TaskRuningActivity.class);
                ((Activity) mContext).startActivity(intent);
            }
        });
    }

    //停止执行快捷任务
    @Override
    public void onstopshortcuttask() {
        LogUtils.setLog(TAG, "onstopshortcuttask the call is stopping ");
       EventBus.getDefault().post(new EventBusStopShortTask("stop shorttask"));
    }

    @Override
    public void onlogin(final int i) {//功能服务器登录状态
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtils.setLog(mTag + "the onlogin callback is " + i);
                switch (i) {
                    case 1:
                        ToastUtil.showToast(mContext, "登录成功");
                        EventBus.getDefault().post(new EventBusConnectString("1"));
                        break;
                    case -1:
                        ToastUtil.showToast(mContext, "登陆失败");
                        EventBus.getDefault().post(new EventBusConnectString("登陆失败"));
                        break;
                    case -2:
                        ToastUtil.showToast(mContext, "登录超时,请检查ip设置");
                        EventBus.getDefault().post(new EventBusConnectString("登录超时,请检查ip设置"));
                        break;
                    case -3:
                        ToastUtil.showToast(mContext, "用户名密码错误");
                        EventBus.getDefault().post(new EventBusConnectString("用户名密码错误"));
                        break;
                    case -4:
                        ToastUtil.showToast(mContext, "此用户已在其他地方登录");
                        htIntf.release();//立即释放已登录，需查看debug消息验证
                        EventBus.getDefault().post(new EventBusConnectString("此用户已在其他地方登录"));
                        break;
                    case -5:
                        ToastUtil.showToast(mContext, "用户不支持登录");
                        EventBus.getDefault().post(new EventBusConnectString("用户不支持登录"));

                        break;
                    case -6:
                        ToastUtil.showToast(mContext, "设备不支持登录");
                        EventBus.getDefault().post(new EventBusConnectString("设备不支持登录"));
                        break;
                }
            }
        });

    }



    //对讲时等待对方应答
    @Override
    public void onspeechwait() {
        LogUtils.setLog(mTag, "onspeechwait wating for answer ");
        EventBus.getDefault().post(new EventBusWaitingForAnswer("wating for answer"));
    }

    //对讲时对方拒绝接听
    @Override
    public void onspeechrefuse() {
        LogUtils.setLog(mTag, "onspeechrefuse the call is be refuse ");

        EventBus.getDefault().post(new EventBusRefuse("用户已拒绝"));
    }




    //开始播放 及播放歌曲时歌曲切换    S歌曲名称以及播放临时语音
    @Override
    public void onstartondemand(final String s) {
        LogUtils.setLog(mTag, "onstartondemand the music is playing " + s);
        if (!( new  MyApplication() .getInstances().currentActivity() instanceof ConnectActivity)) {
            LogUtils.setLog(mTag,"点播发起前的activity"+ new  MyApplication() .getInstances().currentActivity().getLocalClassName());
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra(Constring.PLAY_TYPE, 1);
                    intent.putExtra(Constring.playstyle, Constring.play);
                    intent.setClass(mContext, ConnectActivity.class);
                    
                    ((Activity) mContext).startActivity(intent);
                }
            });
        }
    }


    //主bei动停止点播
    @Override
    public void onstopondemand() {
        LogUtils.setLog(mTag, "onstop ondemand the dianbo is stop ");
        // updateStopTime();
        Cons.chooseMachine.clear();
        Cons.chooseProgList.clear();

        EventBus.getDefault().post(new EventBusStopPlay("tingzhidianbo"));
        LogUtils.setLog(mTag, "fa l xiaoxi  ");
    }

    //  被其它人发起对讲
    @Override
    public void onspeechrequest(final String s) {
        LogUtils.setLog(mTag, s + "onspeechrequest is calling you");
        EventBus.getDefault().post(new EventBusBeRequestSpeech(s));
    }


    public void initCallBack(String var0, int var1, String var2, String var3, int var4, String var5, byte var6) {
        htIntf.setcallbackinterface(this);

        htIntf.init(mContext, var0, var1, var2, var3, var4, var5,var6);
        htIntf.connectserver();
    }


    @Override
    public void onconnect(final boolean b) {

        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (b) {
                    /*GlobalVariable.SEVER_CONNECT_STATU = true;
                    GlobalVariable.MOBILE_ID = htIntf.getterminalid();*/
                    EventBus.getDefault().post(new EventBusReFresh());
                    EventBus.getDefault().post(new EventBusConnectString(""));
                } else {//手动关闭网络会立即执行onconnect需要一段时间显示

                    /*GlobalVariable.SEVER_CONNECT_STATU = false;*/
                    EventBus.getDefault().post(new EventBusConnectString("服务器已断开"));
                    htIntf.connectserver();
                }
            }
        });
    }
    @Override
    public void oncmderror(final int i) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtils.setLog(mTag + "the onlogin callback is " + i);
                switch (i) {
                    case 1:
                        ToastUtil.showToast(mContext, "终端正忙");
                        break;
                    case 2:
                        ToastUtil.showToast(mContext, "操作超时");
                        break;
                    case 3:
                        ToastUtil.showToast(mContext, "无效操作");
                        break;
                    case 4:
                        ToastUtil.showToast(mContext, "操作不支持");
                        break;
                    case 5:
                        ToastUtil.showToast(mContext, "操作失败");
                        break;
                    case 6:
                        ToastUtil.showToast(mContext, "状态错误");
                        break;
                    case 7:
                        ToastUtil.showToast(mContext, "读取设备失败");
                        break;
                }
            }
        });
    }


}
