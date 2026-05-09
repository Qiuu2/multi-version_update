package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusCode;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.VoiceRatingBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by wzq on 2017-07-21.
 *  处于呼叫或者被呼叫状态的activity
 */
public class BeCallActivity extends BaseActivity {
    private VoiceRatingBar voiceRatingBar;
    private ViewHolder viewHolder = new ViewHolder();
    private Context mContext;

    @Override
    protected int getLayoutId() {
        mContext =this;
        return R.layout.activity_call_and_becall;
    }

    @Override
    protected void initSubViews() {

        viewHolder.title_tv = (TextView)findViewById(R.id.title_text);
        viewHolder.title_tv.setText("寻呼");
        String callName = getIntent().getStringExtra(Constring.CALER_NAME);
        viewHolder.callerName_tv = (TextView)findViewById(R.id.center_text);
        viewHolder.callerName_tv.setText(callName+"正在呼叫");
        //networkConnect =(ImageView)findViewById(R.id.connect_image);
        viewHolder.center_image = (ImageView)findViewById(R.id.center_image);
        viewHolder.center_text = (TextView)findViewById(R.id.center_text);
        viewHolder.center_image.setImageResource(R.drawable.aimin_talk_other);
        viewHolder.stopCall_bt = (Button)findViewById(R.id.stop_call);
        viewHolder.stopCall_bt.setVisibility(View.GONE);
        AnimationDrawable animationDrawable1 = (AnimationDrawable) viewHolder.center_image.getDrawable();
        animationDrawable1.start();


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
        private ImageView title_right,title_left;
        private ImageView center_image;
        private TextView  title_tv,center_text;
        private Button stopCall_bt;

        public TextView callerName_tv;
    }

    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusCode event) {
        LogUtils.setLog("the event is on here");
        this.finish();
    }
}
