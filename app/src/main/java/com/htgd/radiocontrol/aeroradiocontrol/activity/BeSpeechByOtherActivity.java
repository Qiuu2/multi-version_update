package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusCode;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRefuse;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStartSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusString;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by wzq on 2017/10/26.
 * 被发起对讲时弹出的控制界面
 */

public class    BeSpeechByOtherActivity extends BaseActivity implements View.OnClickListener{
    private ViewHolder viewHolder = new ViewHolder();
    private Context mContext;
    private final String mTAG = "BeSpeechByOtherActivity";
    private Timer reconnectTimer;
    private TimerTask reconnectTimerTask;
    private Timer speechTimer ;
    @Override
    protected int getLayoutId() {
        mContext =this;
        return R.layout.activity_speech_to_other;
    }
    @Override
    protected void initSubViews() {
        String callName = getIntent().getStringExtra(Constring.caller);
        LogUtils.setLog(mTAG,"the callname is "+callName);
        viewHolder.title_tv = (TextView)findViewById(R.id.title_text);
        viewHolder.title_tv.setText("对讲");
       // networkConnect =(ImageView)findViewById(R.id.connect_image);
        viewHolder.stopCall_bt = (Button)findViewById(R.id.stop_call);
        viewHolder.stopCall_bt.setOnClickListener(this);
        viewHolder.callerName_tv = (TextView)findViewById(R.id.center_text);
        viewHolder.twoButtonView  =(LinearLayout)findViewById(R.id.two_button_view);
        viewHolder.answerCall_bt =(Button)findViewById(R.id.answer_call_bt);
        viewHolder.answerCall_bt.setOnClickListener(this);
        viewHolder.callerName_tv.setText(callName+"正在呼叫");
        viewHolder.refuseCall_bt=(Button)findViewById(R.id.refuse_call_bt);
        viewHolder.refuseCall_bt.setOnClickListener(this);
        viewHolder.phoneImage_iv =(ImageView)findViewById(R.id.center_image);
        viewHolder.phoneImage_iv.setImageResource(R.drawable.aimin_talk_other);
        AnimationDrawable animationDrawable1 = (AnimationDrawable) viewHolder.phoneImage_iv.getDrawable();
        viewHolder.stopCall_bt.setVisibility(View.GONE);
        viewHolder.twoButtonView.setVisibility(View.VISIBLE);
        animationDrawable1.start();

        startAlarm();


        connectWhenTimeOut();

    }

    private void connectWhenTimeOut() {
        speechTimer = new Timer();
        speechTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        answer();
                        LogUtils.setLog(mTAG,"几秒自动接听"+Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.overtimeConnectTime, mContext)));
                    }
                });

            }
        }, /*6000*/Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.overtimeConnectTime, mContext))*1000);//无人应答时自动连接
    }

    private void startAlarm() {
        try {
            MyApplication.getInstances().mp.reset();
            //int mediaid = Integer.parseInt(PreferencesUtil.getInstance().getField(Constants.ringtone, mContext));
           // LogUtils.setLog(mTag, "铃声id" + mediaid);
            MyApplication.getInstances().mp = MediaPlayer.create(mContext, getSystemDefultRingtoneUri());
            MyApplication.getInstances().mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

       /* try {
            new  MyApplication() .getInstances().mp.reset();
            int mediaid = Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.ringtone, mContext));
            LogUtils.setLog(mTAG, "铃声id" + mediaid);
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            new  MyApplication() .getInstances().mp = MediaPlayer.create(mContext, mediaid);
            new  MyApplication() .getInstances().mp.start();
            new  MyApplication() .getInstances().mp.setLooping(true);

            int mode = mAudioManager.getMode();
            LogUtils.setLog(mTAG, mode + "volume mode");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
    private void stopAlarm(){
        if(MyApplication.getInstances().mp!=null) {
            MyApplication.getInstances().mp.stop();
        }
    }


    private Uri getSystemDefultRingtoneUri() {
        return RingtoneManager.getActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_RINGTONE);
    }
    private void stopSpeechTimer(Timer speechtimer) {
        if (speechtimer != null) {

            speechtimer.cancel();
            speechtimer = null;
        }
        if (reconnectTimerTask != null) {
            reconnectTimerTask.cancel();
            reconnectTimerTask = null;
        }
    }
   private void   answer(){
         stopAlarm();

         htIntf.acceptspeechrequest();
         stopSpeechTimer(reconnectTimer);
         viewHolder.stopCall_bt.setVisibility(View.VISIBLE);
         viewHolder.twoButtonView.setVisibility(View.GONE);
         new  MyApplication() .getInstances().mp.stop();
     }
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.stop_call:
                LogUtils.setLog("shut down the speech");
                htIntf.stopspeech();
                viewHolder.phoneImage_iv.clearAnimation();
                finish();
                break;
            case R.id.answer_call_bt:
                answer();
                break;
            case R.id.refuse_call_bt:
                stopAlarm();
                htIntf.stopspeech();
                stopSpeechTimer(reconnectTimer);
                new  MyApplication() .getInstances().mp.stop();
                showToast("已拒接对方来电");
                finish();
                break;
        }
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return isCosumenBackKey();
        }
        return false;
    }
    private boolean isCosumenBackKey() {
        // 这儿做返回键的控制，如果自己处理返回键逻辑就返回true，如果返回false,代表继续向下传递back事件，由系统取控制
        return true;
    }

    private class ViewHolder{
        private TextView  title_tv;
        private Button stopCall_bt;
        private ImageView phoneImage_iv;
        private LinearLayout  twoButtonView;
        private TextView callerName_tv;
        private Button answerCall_bt,refuseCall_bt;
    }

    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        stopAlarm();
    }

    //对方挂断对讲结束
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusCode event) {
        LogUtils.setLog(mTAG+"the event is on here");
        showToast("对方已挂断");
        this.finish();
    }
    // 对方已接听
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusString event) {
        LogUtils.setLog(mTAG+"the event is on here");
        viewHolder.phoneImage_iv.setImageResource(R.drawable.aimin_phone_together);
        AnimationDrawable animationDrawable1 = (AnimationDrawable) viewHolder.phoneImage_iv.getDrawable();
        animationDrawable1.start();
    }
    // 对方已挂断
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusRefuse event) {
        LogUtils.setLog(mTAG+"EventBusRefuse the event is on here");
        this.finish();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusStartSpeech event) {
        LogUtils.setLog(mTAG, "the event is on here");
        finish();
    }
}
