package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusGetVolume;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusShutService;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStopPlay;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStopSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.service.UpFileService;
import com.htgd.radiocontrol.aeroradiocontrol.utils.AudioUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.io.IOException;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constring.temptag;
import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import androidx.annotation.RequiresApi;

/**
 * 作者：wzq
 * 时间：2019/1/22:10:18
 * 邮箱：535708929
 * 说明：寻呼对讲点播连接时的界面
 */
public class ConnectActivity extends BaseActivity implements View.OnClickListener {
    private ViewHolder viewHolder = new ViewHolder();
    private Context mContext;
    private int voiceNomnber;
    private String mTag = "ConnectActivity";
    private String style;
    private ImageView ivGif;
    private TaskManageUtils taskManageUtils;
    private int DIANBO = 3/*发起点播*/, RENWU = 1, XUNHU = 6, DUIJIANG = 2, KUAIJIEXUNHU = 5;//被点播=1，发起临时语音=1
    private Notification notification;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_call_and_becall;
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.setLog(mTag, "onresume");
    }


    //停止蓝牙采集音频

//        manager.setBluetoothScoOn(false);
//        manager.stopBluetoothSco();

    private void initUi() {
        viewHolder.title_tv = (TextView) findViewById(R.id.title_text);
        viewHolder.title_back = (TextView) findViewById(R.id.title_go_back);
        viewHolder.title_back.setVisibility(View.GONE);
        viewHolder.stopCall_bt = (Button) findViewById(R.id.stop_call);
        viewHolder.stopCall_bt.setOnClickListener(this);
        viewHolder.stopCall_bt.setVisibility(View.VISIBLE);
        viewHolder.centerText = (TextView) findViewById(R.id.center_text);
        viewHolder.centerText.setVisibility(View.GONE);
        viewHolder.center_image = (ImageView) findViewById(R.id.center_image);
        viewHolder.seekBarView = (View)findViewById(R.id.seek_bar_all);
        viewHolder.seekBarView.setVisibility(View.VISIBLE);
        viewHolder.volume_style = (TextView) findViewById(R.id.volumestyle);
        viewHolder.cVolume = (TextView) findViewById(R.id.volume_tv);
        viewHolder.seekBar = (SeekBar) findViewById(R.id.volume_bar);
       // ivGif = (ImageView) findViewById(R.id.center_image);


    }
    @Override
    protected void initSubViews() {
        mContext = this;
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //开启蓝牙采集音频

         manager.setBluetoothScoOn(true);
        manager.startBluetoothSco();

        taskManageUtils = new TaskManageUtils(mContext);
        initUi();

        if (!EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().register(this);
        }
        style = getIntent().getExtras().getString("style");
      /*  viewHolder.time_length.setBase(SystemClock.elapsedRealtime());
        viewHolder.time_length.start();*/
        if (style.equals(Constring.caller)) {//发起寻呼
            htIntf.setpagingterminalvolume(Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext))/**6+10*/);
            viewHolder.seekBar.setProgress(Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext)));
            viewHolder.title_tv.setText(getResources().getString(R.string.callOthers));
            viewHolder.volume_style.setText(/*"目标音量："*/ChinaConstants.TARGET_VOLUME);
            viewHolder.cVolume.setText(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext));
            setCallGifs();
        } else if (style.equals(Constring.called)) {
            viewHolder.title_tv.setText(getResources().getString(R.string.othercalls));
            viewHolder.stopCall_bt.setVisibility(View.GONE);
            setCallGifs();
        } else if (style.equals(Constring.speak)) {
            viewHolder.seekBar.setProgress(Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.speakvolume, mContext)));
             AudioUtil.getInstance(mContext).setMediaVolume(viewHolder.seekBar.getProgress() * mediaMaxVolume / 100);
            viewHolder.cVolume.setText(PreferencesUtil.getInstance().getField(CacheConstants.speakvolume, mContext));
            viewHolder.title_tv.setText(getResources().getString(R.string.speak));
            setSpeakGifs();
        } else if (style.equals(Constring.play)) {
            viewHolder.title_tv.setText(getResources().getString(R.string.play));
            setPlayGifs();
            if (Cons.chooseProgList.size() > 0) {  //点播发起方
              LogUtils.setLog(mTag,"点播发起方");
                if (htIntf.getworkstate() == 1 && Cons.chooseProgList.get(0).getName().contains(temptag)) {//临时语音发起
                    LogUtils.setLog(mTag,"临时语音发起方");
                    viewHolder.title_tv.setText(ChinaConstants.TEMP_VOICE);
                    viewHolder.volume_style.setText(ChinaConstants.TARGET_VOLUME);
                    viewHolder.seekBar.setProgress(Integer.parseInt(PreferencesUtil.getInstance().getField("mvolume", mContext)));
                    viewHolder.cVolume.setText(PreferencesUtil.getInstance().getField("mvolume", mContext));
                    try {
                        setTaskVoice((Integer.parseInt(PreferencesUtil.getInstance().getField("mvolume", mContext))));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (htIntf.getworkstate() == 3) {//点播发起
                    viewHolder.volume_style.setText("目标音量：");
                    viewHolder.seekBar.setProgress(Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext)));
                    viewHolder.cVolume.setText(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext));
                    htIntf.setondemandvolume(viewHolder.seekBar.getProgress());
                }
                LogUtils.setLog(mTag, style + "连接类型ssds");
            }else  {//点播接收方
                 LogUtils.setLog(mTag, "接收点播" + Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext)));

                viewHolder.stopCall_bt.setVisibility(View.GONE);
                 viewHolder.seekBar.setProgress(Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext)));
                AudioUtil.getInstance(mContext).setMediaVolume(viewHolder.seekBar.getProgress() * mediaMaxVolume / 100);
                viewHolder.cVolume.setText(String.valueOf(viewHolder.seekBar.getProgress()));
            }

        }

        viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 设置“与系统默认SeekBar对应的TextView”的值
                viewHolder.cVolume.setText(String.valueOf(seekBar.getProgress()));
                voiceNomnber = progress;


            }

            //开始滚动
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //停止滚动
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSavedVolume();
                if (style.equals(Constring.caller)) {
                    htIntf.setpagingterminalvolume(voiceNomnber);
                } else if (style.equals(Constring.speak) || style.equals(Constring.called)) {//对讲调节自身音量
                    AudioUtil.getInstance(mContext).setMediaVolume(seekBar.getProgress() * mediaMaxVolume / 100);
                } else if (style.equals(Constring.play)) {
                    if (htIntf.getworkstate() == 3) {//点播发起
                        htIntf.setondemandvolume(voiceNomnber);
                    }
                    if (Cons.chooseProgList.size() > 0) {
                        if (Cons.chooseProgList.get(0).getName().contains(temptag) && htIntf.getworkstate() == 1) {//临时语音发起
                            try {
                                setTaskVoice(voiceNomnber);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } else {
                    AudioUtil.getInstance(mContext).setMediaVolume(seekBar.getProgress() * mediaMaxVolume / 100);
                }

            }
        });

    }

    private void setSavedVolume() {
        if (style.equals(Constring.caller)) {
            PreferencesUtil.getInstance().keepField(CacheConstants.callvolume, voiceNomnber + "", mContext);

        } else if (style.equals(Constring.speak)) {
            PreferencesUtil.getInstance().keepField(CacheConstants.speakvolume, voiceNomnber + "", mContext);
        } else if (style.equals(Constring.play) && htIntf.getworkstate() == 3) {
            PreferencesUtil.getInstance().keepField(CacheConstants.playvolume, voiceNomnber + "", mContext);
        }
    }




    private void setCallGifs() {//设置区分动画
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
      // Glide.with(mContext).load(R.drawable.call_anim).apply(options).into(viewHolder.center_image);
    }

    private void setSpeakGifs() {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
     // Glide.with(mContext).load(R.drawable.speak_anim).apply(options).into(viewHolder.center_image);
    }

    private void setPlayGifs() {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
      // Glide.with(mContext).load(R.drawable.call_anim).apply(options).into(viewHolder.center_image);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.stop_call:
               LogUtils.setLog(mTag,"workstate"+htIntf.getworkstate());
                if (htIntf.getworkstate() == DIANBO) {
                    htIntf.stopondemand();
                    htIntf.stoppaging();
                } else if (htIntf.getworkstate() == DUIJIANG) {
                    htIntf.stopspeech();
                } else if (htIntf.getworkstate() == XUNHU) {
                    htIntf.stoppaging();
                } else if (htIntf.getworkstate() == KUAIJIEXUNHU) {
                    htIntf.stoppaging();
                } else if (htIntf.getworkstate() == RENWU) {//临时语音
                    try {
                        runOrStopTask(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //关闭服务
                    Intent stopIntent = new Intent(this, UpFileService.class);
                    stopService(stopIntent);
                } else if (htIntf.getworkstate() == 0) {
                    finish();
                }

                Cons.chooseMachine.clear();
                Cons.chooseProgList.clear();

                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showToast("关闭任务");
        LogUtils.setLog(mTag,"workstate"+htIntf.getworkstate());
        if (htIntf.getworkstate() == DIANBO) {
            htIntf.stopondemand();
            htIntf.stoppaging();
        } else if (htIntf.getworkstate() == DUIJIANG) {
            htIntf.stopspeech();
        } else if (htIntf.getworkstate() == XUNHU) {
            htIntf.stoppaging();
        } else if (htIntf.getworkstate() == KUAIJIEXUNHU) {
            htIntf.stoppaging();
        } else if (htIntf.getworkstate() == RENWU) {//临时语音
            try {
                runOrStopTask(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //关闭服务
            Intent stopIntent = new Intent(this, UpFileService.class);
            stopService(stopIntent);
        } else if (htIntf.getworkstate() == 0) {
            finish();
        }

        Cons.chooseMachine.clear();
        Cons.chooseProgList.clear();

        finish();
    }

    // 调节声音
    private synchronized void setTaskVoice(int voice) throws IOException {

        String task_id = PreferencesUtil.getInstance().getField(Constring.TASK_ID, mContext);
        LogUtils.setLog(mTag, "调节音量的任务id" + task_id);
        taskManageUtils.setTaskVoice( task_id , voice, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {

            }

            @Override
            public void onTheSameStatu() {

            }

            @Override
            public void onRetry() {

            }
        });
    }

    //执行或停止方案
    private synchronized void runOrStopTask(final int state) throws IOException {
        String task_id = PreferencesUtil.getInstance().getField(Constring.TASK_ID, mContext);
        LogUtils.setLog(mTag, "tingzhi" + task_id);
        taskManageUtils.runOrStopTask( task_id , state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(mTag, "the call back is sucess" + state);
            }

            @Override
            public void onTheSameStatu() {
                LogUtils.setLog(mTag, "the call back is onTheSameStatu" + state);
            }

            @Override
            public void onRetry() {
            }
        });
    }

    private class ViewHolder {
        private TextView title_tv;
        private TextView title_back;
        private Chronometer timelength;
        private Button stopCall_bt;
        public TextView centerText, cVolume, volume_style;
      //  public Chronometer time_length;
        public SeekBar seekBar;
        public ImageView center_image;
        public View seekBarView;
    }

    public void onDestroy() {
        super.onDestroy();
        LogUtils.setLog(mTag, "ondestroy");
        if (EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().unregister(this);
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent3(EventBusShutService event) {
        LogUtils.setLog(mTag + "stop service" + event.getMessage());
        Intent stopIntent = new Intent(this, UpFileService.class);
        stopService(stopIntent);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusStopPlay event) {
        LogUtils.setLog(mTag, "the event is on here");
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusGetVolume event) {
        LogUtils.setLog(mTag, "the event is get volume" + event.getCount());
        if (Cons.chooseProgList.size() > 0) {
            if (!(Cons.chooseProgList.get(0).getName().contains("-") && htIntf.getworkstate() == 1)) {//非临时语音
                LogUtils.setLog(mTag, "被点播方接收音量" + event.getCount());
                AudioUtil.getInstance(mContext).setMediaVolume((event.getCount() * mediaMaxVolume) / 100);//被寻呼时，改变音量
                viewHolder.cVolume.setText(event.getCount() + "");
                viewHolder.seekBar.setProgress(event.getCount());
            }
        } else {
            LogUtils.setLog(mTag, "接收方接收音量" + event.getCount());
            AudioUtil.getInstance(mContext).setMediaVolume(event.getCount() * mediaMaxVolume / 100);//被寻呼时，改变音量
            viewHolder.cVolume.setText(event.getCount() + "");
            viewHolder.seekBar.setProgress(event.getCount());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusStopSpeech event) {
        finish();
    }


}
