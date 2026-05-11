package com.htgd.radiocontrol.aeroradiocontrol.base;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.GetTokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogToFile;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
import com.htgd.radiocontrol.screanadaption.AutoSize;
import com.htgd.radiocontrol.screanadaption.AutoSizeConfig;
import com.htgd.radiocontrol.screanadaption.AutoSizeLog;
import com.htgd.radiocontrol.screanadaption.ScreenUtils;
import com.htgd.radiocontrol.screanadaption.external.ExternalAdaptInfo;
import com.htgd.radiocontrol.screanadaption.external.ExternalAdaptManager;
import com.htgd.radiocontrol.screanadaption.internal.CustomAdapt;
import com.htgd.radiocontrol.screanadaption.onAdaptListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import androidx.multidex.MultiDex;
import cat.ereza.customactivityoncrash.activity.DefaultErrorActivity;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.Cons.tokenUpdatable;

/**
 * 作者：wzq
 * 时间：2020/8/3:18:47
 * 邮箱：535708929
 * 说明：
 */
public class MyApplication extends Application {
    public static Stack<Activity> activityStack;
    private static MyApplication instances;
    public MediaPlayer mp = new MediaPlayer();
    private Timer mTimer;
    private TimerTask mTimerTask;
    private String mTag = "MyApplication";



    @Override
    public void onCreate() {
        super.onCreate();
        instances = this;
        MultiDex.install(this);
        configUnits();
        setDefault();
         initBaiduMap();
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
            }
        });

    }
   //百度地图初始化
    private void initBaiduMap() {

        LogUtils.setLog(mTag,"百度地图初始化成功00");

        // Baidu Map SDK v8.0+ and Location SDK v9.6+ enforce China PIPL
        // privacy-compliance: the app must explicitly declare it has obtained
        // user consent BEFORE SDKInitializer.initialize() or
        // any LocationClient is constructed. Skipping this throws at runtime.
        SDKInitializer.setAgreePrivacy(this, true);
        LocationClient.setAgreePrivacy(true);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(this);
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);
        LogUtils.setLog(mTag,"百度地图初始化成功");
    }

    public void setDefault() {
        if (PreferencesUtil.getInstance().getField(Constring.mvolume, getContext()) == null ||
                PreferencesUtil.getInstance().getField(Constring.mvolume, getContext()) == "") {
            PreferencesUtil.getInstance().keepField(CacheConstants.overtimeConnect, "false", getContext());//设置默认超时不接听
            PreferencesUtil.getInstance().keepField(CacheConstants.overtimeConnectTime, "10000", getContext());//设置默认超时不接听
            PreferencesUtil.getInstance().keepField(CacheConstants.waittime, "60s", getContext());//设置默认锁屏时间
            PreferencesUtil.getInstance().keepField(CacheConstants.callvolume, "80", getContext());
            PreferencesUtil.getInstance().keepField(CacheConstants.speakvolume, "80", getContext());
            PreferencesUtil.getInstance().keepField(CacheConstants.playvolume, "80", getContext());
            PreferencesUtil.getInstance().keepField(CacheConstants.localvolume, "80", getContext());
            PreferencesUtil.getInstance().keepField(Constring.versionsp, "0", getContext());
            PreferencesUtil.getInstance().keepField(Constring.EXCEPTION_LOGIN, "false", getContext());
            //临时语音属性
            PreferencesUtil.getInstance().keepField(Constring.mspeed, "5", getContext());
            PreferencesUtil.getInstance().keepField(Constring.mvolume, "80", getContext());
            PreferencesUtil.getInstance().keepField(Constring.mmale, "1", getContext());
            PreferencesUtil.getInstance().keepField(Constring.mTimeLength, "1", getContext());

            LogUtils.setLog(mTag, "set default");
        } else {
            LogUtils.setLog(mTag, PreferencesUtil.getInstance().getField(Constring.mvolume, getContext()) + "set defaults");
        }
    }
    public void appExit(Context context) {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
        //适配初始化
    private void configUnits() {
        AutoSize.checkAndInit(this);
        AutoSizeConfig.getInstance()
                .setCustomFragment(true)
                .setExcludeFontScale(true)

//                .setPrivateFontScale(0.8f)
                .setOnAdaptListener(new onAdaptListener() {
                    @Override
                    public void onAdaptBefore(Object target, Activity activity) {
                        //使用以下代码, 可以解决横竖屏切换时的屏幕适配问题
                        //使用以下代码, 可支持 Android 的分屏或缩放模式, 但前提是在分屏或缩放模式下当用户改变您 App 的窗口大小时
                        //系统会重绘当前的页面, 经测试在某些机型, 某些情况下系统不会主动重绘当前页面, 所以这时您需要自行重绘当前页面
                        //ScreenUtils.getScreenSize(activity) 的参数一定要不要传 Application!!!
                       //   AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
                      //  AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                        AutoSizeLog.d(String.format(Locale.ENGLISH, "%s onAdaptBefore!", target.getClass().getName()));
                    }

                    @Override
                    public void onAdaptAfter(Object target, Activity activity) {
                        AutoSizeLog.d(String.format(Locale.ENGLISH, "%s onAdaptAfter!", target.getClass().getName()));
                    }
                })

        //是否打印 AutoSize 的内部日志, 默认为 true, 如果您不想 AutoSize 打印日志, 则请设置为 false
//                .setLog(false)

        //是否使用设备的实际尺寸做适配, 默认为 false, 如果设置为 false, 在以屏幕高度为基准进行适配时
        //AutoSize 会将屏幕总高度减去状态栏高度来做适配
        //设置为 true 则使用设备的实际屏幕高度, 不会减去状态栏高度
        //在全面屏或刘海屏幕设备中, 获取到的屏幕高度可能不包含状态栏高度, 所以在全面屏设备中不需要减去状态栏高度，所以可以 setUseDeviceSize(true)
//                .setUseDeviceSize(true)

        //是否全局按照宽度进行等比例适配, 默认为 true, 如果设置为 false, AutoSize 会全局按照高度进行适配
                .setBaseOnWidth(true)

        //设置屏幕适配逻辑策略类, 一般不用设置, 使用框架默认的就好
//                .setAutoAdaptStrategy(new AutoAdaptStrategy())
        ;
        customAdaptForExternal();
    }

    /**
     * 给外部的三方库 {@link Activity} 自定义适配参数, 因为三方库的 {@link Activity} 并不能通过实现
     * {@link CustomAdapt} 接口的方式来提供自定义适配参数 (因为远程依赖改不了源码)
     * 所以使用 {@link ExternalAdaptManager} 来替代实现接口的方式, 来提供自定义适配参数
     */
    private void customAdaptForExternal() {
        /**
         * {@link ExternalAdaptManager} 是一个管理外部三方库的适配信息和状态的管理类, 详细介绍请看 {@link ExternalAdaptManager} 的类注释
         */
        ExternalAdaptManager externalAdaptManager = AutoSizeConfig.getInstance().getExternalAdaptManager()
                .addExternalAdaptInfoOfActivity(DefaultErrorActivity.class, new ExternalAdaptInfo(true, getResources().getDisplayMetrics().xdpi));
    }

    public void startReconnect() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                public void run() {
                    if (tokenUpdatable) {
                        try {
                            updateToken(getApplicationContext(), new updatelister() {
                                @Override
                                public void onSucess() {

                                }

                                @Override
                                public void onFailed() {
                                    Toast.makeText(getApplicationContext(), "服务器异常", Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            if (mTimer != null && mTimerTask != null)
                mTimer.schedule(mTimerTask, 10000, 30000);
        }
    }

    interface updatelister {
        void onSucess();

        void onFailed();
    }

    public void updateToken(final Context mContext, final updatelister lister) throws IOException {
        final GetTokenModel model = PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel, GetTokenModel.class, mContext);
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.getAuthorization);
        myRequest.setBodyMap(map);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TokenModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TokenModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TokenModel tokenModel = responseData.getData().get(0);
                    if (tokenModel != null && !ValueUtil.isEmpty(tokenModel.getToken())) {
                        ServerToken.serverToken = Constant.token_tag + tokenModel.getToken();
                        LogUtils.setLog(mTag, "更新了token" +  ServerToken.serverToken);
                        PreferencesUtil.getInstance().keepEntity(Constant.key_tokenString,  ServerToken.serverToken, mContext);
                        lister.onSucess();
                    } else {
                        lister.onFailed();
                        LogUtils.setLog(mTag, "更新token失败");
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.sever_exception), Toast.LENGTH_LONG).show();
                    }
                } else {
                    lister.onFailed();
                    LogUtils.setLog(mTag, "更新token失败");
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.sever_exception), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "更新token时" + code + message);
                lister.onFailed();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.conntect_time_out), Toast.LENGTH_LONG).show();
            }
        });


    }

    public static MyApplication getInstances() {
        return instances;
    }

    public static Context getContext() {
        return instances.getApplicationContext();
    }

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {

        if (activityStack == null) {
            activityStack = new Stack<Activity>();
        }
        activityStack.add(activity);
        LogUtils.setLog(mTag, "堆栈添加活动" + activity.getLocalClassName());
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        for (int i = 0; i < activityStack.size(); i++) {
            if (activityStack.get(i) != null) {
                LogUtils.setLog("the activity  is finish " + activityStack.get(i).getClass().getCanonicalName());
                LogUtils.setLog("the activity  de size " + activityStack.size());
                activityStack.get(i).finish();
            }
        }
        activityStack.clear();
    }

    /**
     * 结束指定activity外所有Activity
     */
    public void finishActivityExceptThi(Activity activity) {
        for (int i = 0; i < activityStack.size(); i++) {
            if (null != activityStack.get(i) && activity != activityStack.get(i)) {
                LogUtils.setLog("the activity  is finish " + activityStack.get(i).getClass().getCanonicalName());
                activityStack.get(i).finish();
            }
        }
        // activityStack.clear();
        LogUtils.setLog("duizhanchangdu" + activityStack.size());
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            activityStack.remove(activity);
            activity.finish();
            activity = null;
        }
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    public static Activity currentActivity() {
        Activity activity = activityStack.lastElement();
        return activity;
    }
}
