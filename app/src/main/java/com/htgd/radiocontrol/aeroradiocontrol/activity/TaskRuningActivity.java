package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusStopPlay;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusStopShortTask;
import com.htgd.radiocontrol.aeroradiocontrol.service.UpFileService;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

/**
 * Created by wzw on 2017/10/19.
 */
public class TaskRuningActivity extends BaseActivity implements View.OnClickListener{
    private ViewHolder viewHolder = new ViewHolder();
    private Context mContext;
    private int voiceNomnber;
    private String mTag="TaskRuningActivity";
    private TaskManageUtils taskManageUtils;

    @Override
    protected int getLayoutId() {
        mContext =this;
        taskManageUtils = new TaskManageUtils(mContext);
        return R.layout.activity_call_and_becall;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showToast("关闭任务");
        LogUtils.setLog(mTag,"workstate"+htIntf.getworkstate());

            try {
                runOrStopTask(0);
            } catch (IOException e) {
                e.printStackTrace();
            }


        finish();
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

    @Override
    protected void initSubViews() {
        if (!EventBus.getDefault().isRegistered(mContext)) {//加上判断
            EventBus.getDefault().register(mContext);
        }
        viewHolder.title_tv = (TextView)findViewById(R.id.title_text);
        viewHolder.title_tv.setText("任务执行");
        viewHolder.center_image = (ImageView)findViewById(R.id.center_image);
        viewHolder.center_text = (TextView)findViewById(R.id.center_text);
        viewHolder.center_text.setVisibility(View.GONE);
        viewHolder.taskRun_bt = (Button)findViewById(R.id.stop_call);
        viewHolder.taskRun_bt.setText("结束任务");
        viewHolder.taskRun_bt.setOnClickListener(this);
        viewHolder.taskRun_bt.setVisibility(View.VISIBLE);
        viewHolder.seekBarView = findViewById(R.id.seek_bar_all);
        viewHolder.seekBarView.setVisibility(View.VISIBLE);
        viewHolder.seekBar  =(SeekBar)findViewById(R.id.volume_bar);
        viewHolder.textView = (TextView)findViewById(R.id.volume_tv);
        viewHolder.leftImage =(ImageView)findViewById(R.id.left_voice_image);
        viewHolder.center_image.setImageResource(R.drawable.aimin_task_run);
        AnimationDrawable animationDrawable1 = (AnimationDrawable) viewHolder.center_image.getDrawable();
        animationDrawable1.start();
        LogUtils.setLog(mTag,Constant.VOICE_NOMBER_INIT+"");
        viewHolder.seekBar.setProgress(Cons.shorttask.getVolume());
        viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 设置“与系统默认SeekBar对应的TextView”的值
                voiceNomnber =progress;
                viewHolder.textView.setText(""+voiceNomnber);


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
                htIntf.setshortcuttaskvolume(voiceNomnber);
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.stop_call:
                htIntf.stopshortcuttask();
                LogUtils.setLog(mTag, "stop shorttask this");
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
        private ImageView center_image;
        private TextView  title_tv,center_text;
        private Button taskRun_bt;
        private View seekBarView;
        private SeekBar seekBar;
        private TextView textView;
        private ImageView leftImage;
    }

    public void onDestroy(){
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusStopShortTask event) {
        LogUtils.setLog(mTag, "stop shorttask");
        finish();
    }
}
