package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusCode;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusFinish;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRefuse;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStartSpeech;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
/**
 * Created by wzq on 2017/10/8.
 * 发起对讲界面
 */

public class CallOtherActivity extends BaseActivity implements View.OnClickListener{
    private ViewHolder viewHolder = new ViewHolder();
    private Context mContext;

    private int voiceNomnber;
    private String mTag="CallOtherActivity";

    @Override
    protected int getLayoutId() {
        mContext =this;
        return R.layout.activity_call_and_becall;
    }

    @Override
    protected void initSubViews() {

        viewHolder.title_tv = (TextView)findViewById(R.id.title_text);
        viewHolder.title_tv.setText("发起对讲");
        viewHolder.stopCall_bt = (Button)findViewById(R.id.stop_call);
        viewHolder.stopCall_bt.setOnClickListener(this);
        viewHolder.stopCall_bt.setVisibility(View.VISIBLE);
        viewHolder.stopCall_bt.setText("取消对讲");
        viewHolder.seekBarView = findViewById(R.id.seek_bar_all);
        viewHolder.seekBarView.setVisibility(View.GONE);
        viewHolder.seekBar  =(SeekBar)findViewById(R.id.volume_bar);

        viewHolder.voiceNumber = (TextView)findViewById(R.id.volume_tv);
        viewHolder.centerText=(TextView)findViewById(R.id.center_text);
          viewHolder.centerText.setVisibility(View.GONE);
        viewHolder.leftImage =(ImageView)findViewById(R.id.left_voice_image);
        viewHolder.center_image =(ImageView)findViewById(R.id.center_image);
        viewHolder.center_image.setImageResource(R.drawable.aimin_talk_other);

        AnimationDrawable animationDrawable1 = (AnimationDrawable) viewHolder.center_image.getDrawable();
        animationDrawable1.start();
        viewHolder.seekBar.setProgress(Constant.VOICE_NOMBER_INIT);
        viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 设置“与系统默认SeekBar对应的TextView”的值
                viewHolder.voiceNumber.setText(String.valueOf(seekBar.getProgress()));
               voiceNomnber =progress;
            }
            //开始滚动
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            //停止滚动
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(voiceNomnber == 0){
                    viewHolder.leftImage.setImageResource(R.mipmap.voice_off);
                }else{
                    viewHolder.leftImage.setImageResource(R.mipmap.voice_low);
                }
                 LogUtils.setLog("tiaojieyinliang");
                htIntf.setpagingterminalvolume(voiceNomnber);
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.stop_call://取消发起对讲
                htIntf.stopspeech();
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
    // 调节声音
    private synchronized void setTaskVoice(boolean cutDown) throws IOException {
        final int finalVoice = voiceNomnber;
    }
    private class ViewHolder{
        private TextView  title_tv;
        private Button stopCall_bt;
        private SeekBar seekBar;
        private TextView voiceNumber;
        private ImageView leftImage;
        private View seekBarView;
        private ImageView center_image;
        public TextView centerText;
    }

    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)//来电框取消
    public void onEvent(EventBusRefuse event) {
         finish();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusCode event) {
        LogUtils.setLog("the event is on here");
        this.finish();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent1(EventBusFinish event) {
        LogUtils.setLog( "the event is on here");
        this.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusStartSpeech event) {
        LogUtils.setLog(mTag, "the event is on here");
       finish();
    }
}
