package com.htgd.radiocontrol.aeroradiocontrol.base;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constring.topActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Toast;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.BeSpeechByOtherActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusBeRequestSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusCode;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.GetTokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.AndroidVersion;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
import com.htgd.radiocontrol.screanadaption.AutoSizeConfig;
import com.htgd.radiocontrol.screanadaption.internal.CustomAdapt;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.io.IOException;
import java.util.HashMap;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by wzq on 2017-07-13.
 */
public abstract class BaseActivity extends AppCompatActivity implements CustomAdapt {

    private final String mTag = "BaseActivity";
    public String currentActivity;
    public static int mediaMaxVolume;

    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        applyEdgeToEdgeOptOut();
        setContentView(getLayoutId());
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        currentActivity = this.getLocalClassName();

        initSubViews();
        MyApplication.getInstances().addActivity(this);
    }

    /**
     * Restore the pre-Android-15 "fit system windows" behavior.
     *
     * Apps targeting SDK 35+ on Android 15+ get edge-to-edge enabled by
     * default — content is drawn behind the status and navigation bars.
     * The legacy layouts in this app were designed for the older inset
     * model and would have their top/bottom regions clipped by the
     * system bars. Calling setDecorFitsSystemWindows(true) opts back
     * into the classic behavior for every BaseActivity subclass.
     *
     * Long-term, individual screens should be migrated to handle
     * WindowInsets explicitly so they can take advantage of edge-to-edge.
     * That migration is out of scope for the multi-version upgrade.
     */
    private void applyEdgeToEdgeOptOut() {
        if (AndroidVersion.defaultsToEdgeToEdge()) {
            getWindow().setDecorFitsSystemWindows(true);
        }
    }


    @Override
    public boolean isBaseOnWidth() {
        return true;
    }
    @Override
    public float getSizeInDp() {
        return getResources().getDisplayMetrics().xdpi;
    }
    /**
     * 需要注意的是暂停 AndroidAutoSize 后, AndroidAutoSize 只是停止了对后续还没有启动的 {@link Activity} 进行适配的工作
     * 但对已经启动且已经适配的 {@link Activity} 不会有任何影响
     *
     * @param view {@link View}
     */
    public void stop(View view) {
        Toast.makeText(getApplicationContext(), "AndroidAutoSize stops working!", Toast.LENGTH_SHORT).show();
        AutoSizeConfig.getInstance().stop(this);
    }

    /**
     * 需要注意的是重新启动 AndroidAutoSize 后, AndroidAutoSize 只是重新开始了对后续还没有启动的 {@link Activity} 进行适配的工作
     * 但对已经启动且在 stop 期间未适配的 {@link Activity} 不会有任何影响
     *
     * @param view {@link View}
     */
    public void restart(View view) {
        Toast.makeText(getApplicationContext(), "AndroidAutoSize continues to work", Toast.LENGTH_SHORT).show();
        AutoSizeConfig.getInstance().restart();
    }





    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.setLog("baseActivity", "onStart is start");
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        // 结束Activity&从堆栈中移除
        MyApplication.getInstances().finishActivity(this);
    }

    /**
     * 文字提
     *
     * @param text
     */
    public void showToast(final CharSequence text) {
        ToastUtil.showToastOnUIThread(this, text);
    }


    public void showToast(final int textId) {
        showToast(getText(textId));
    }

    abstract protected int getLayoutId();

    abstract protected void initSubViews();

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

                        lister.onSucess();
                    } else {
                        lister.onFailed();
                        showToast(R.string.sever_exception);
                    }
                } else {
                    lister.onFailed();
                    showToast(R.string.sever_exception);
                }

            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog("the request failed data back is" + code + message);
                lister.onFailed();
                showToast(R.string.conntect_time_out);
            }
        });


    }

    public interface updatelister {
        void onSucess();

        void onFailed();
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusCode event) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)//来电
    public void onEvent(EventBusBeRequestSpeech event) {
        String callername = event.getMessage();
        LogUtils.setLog(mTag, topActivity+"跳接听界面0"+currentActivity);
        if (currentActivity.contains( topActivity)) {
            LogUtils.setLog(mTag, "跳接听界面");
            Intent intent = new Intent(this, BeSpeechByOtherActivity.class);
            intent.putExtra(Constring.caller, callername);
            startActivity(intent);
        }
    }
}
