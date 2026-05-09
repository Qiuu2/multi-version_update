package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.LoginActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;

import androidx.annotation.IdRes;

/**
 * Created by wzw on 2017/12/9.
 */

public class DialogAttrSet implements View.OnClickListener, DialogInterface.OnDismissListener {


    private final Context mContext;
    private final OnViewClickListener listener;
    private Dialog dialog;
    private View dialogView;
    private Button confirm_bt, cancel_bt;
    private LinearLayout content_volume, content_time, content_speed;
    private SeekBar volume_bar, cycle_bar, speed_bar;
    private TextView volume_text, cycle_text, speed_text;
    private RadioGroup rg;
    private TempTTSModel model;
    private RadioButton rb_man, rb_woman;
    private LinearLayout engineTip;

    public DialogAttrSet(Context context, TempTTSModel model, OnViewClickListener listener) {
        this.mContext = context;
        this.model = model;
        this.listener = listener;
        initView();
    }

    private void initView() {
        dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_attr_set, null);
        confirm_bt = (Button) dialogView.findViewById(R.id.confirm_bt);
        engineTip = (LinearLayout) dialogView.findViewById(R.id.engine_tip);
        confirm_bt.setOnClickListener(this);
        dialog = new Dialog(mContext);
        dialog.setTitle("设置属性");
        dialog.setContentView(dialogView);
        dialog.setOnDismissListener(this);
        dialog.setCanceledOnTouchOutside(false);
        setDialogUnableDismiss();
        initSeekBar();
        initRadioGroup();
        judgeEngine();
    }
    public class TTSListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {

        }
    }
    public void judgeEngine(){
        TextToSpeech  tts = new TextToSpeech(mContext, new  TTSListener());
        if(tts.getDefaultEngine().contains(CacheConstants.XUNFEI)){//有讯飞引擎
           engineTip.setVisibility(View.GONE);CacheConstants.HAS_XUNFEI_ENGINE=true;
        }else{
            engineTip.setVisibility(View.VISIBLE);
            CacheConstants.HAS_XUNFEI_ENGINE=false;
        }
        engineTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction("com.android.settings.TTS_SETTINGS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        });

    }

    /**
     * 设置谭框不能消失
     */
    public void setDialogUnableDismiss() {
        dialog.setCancelable(false);
    }

    public void setDialogAbleDismiss() {
        dialog.setCancelable(true);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    private void initSeekBar() {
        content_volume = (LinearLayout) dialogView.findViewById(R.id.content_volume);
        content_speed = (LinearLayout) dialogView.findViewById(R.id.content_speed);
        content_time = (LinearLayout) dialogView.findViewById(R.id.content_time);

        volume_bar = (SeekBar) dialogView.findViewById(R.id.volume_bar);

        cycle_bar = (SeekBar) dialogView.findViewById(R.id.time_bar);

        speed_bar = (SeekBar) dialogView.findViewById(R.id.speed_bar);

        volume_text = (TextView) dialogView.findViewById(R.id.volume_text);

        cycle_text = (TextView) dialogView.findViewById(R.id.cycle_text);

        speed_text = (TextView) dialogView.findViewById(R.id.speed_text);
        if (model.getVolume() != null) {
            volume_bar.setProgress(Integer.parseInt(model.getVolume()));
            volume_text.setText(model.getVolume());
        }
        if (model.getTimelength() != null) {
            cycle_bar.setProgress(Integer.parseInt(model.getTimelength()));
            cycle_text.setText(model.getTimelength());
        }
        if (model.getSpeed() != null) {
            speed_bar.setProgress(Integer.parseInt(model.getSpeed()));
            speed_text.setText(model.getSpeed());
        }


        volume_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume_text.setText(progress + "");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                model.setVolume(seekBar.getProgress() + "");
                PreferencesUtil.getInstance().keepField("mvolume",seekBar.getProgress() + "",mContext);
            }
        });
        speed_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed_text.setText(progress + "");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                model.setSpeed(seekBar.getProgress() + "");
                PreferencesUtil.getInstance().keepField("mspeed",seekBar.getProgress() + "",mContext);
            }
        });
        cycle_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cycle_text.setText(progress  + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                model.setTimelength(seekBar.getProgress()  + "");
                PreferencesUtil.getInstance().keepField("mTimeLength",seekBar.getProgress() + "",mContext);
            }
        });
        volume_bar.setMax(100);
        cycle_bar.setMax(100);

        speed_bar.setMax(10);

    }

    private void initRadioGroup() {


        rg = (RadioGroup) dialogView.findViewById(R.id.rg);
        rb_man = (RadioButton) dialogView.findViewById(R.id.man);
        rb_woman = (RadioButton) dialogView.findViewById(R.id.woman);
        if (model.getMale() != null && model.getMale().equals("1")) {
            rb_man.setChecked(true);

        } else if (model.getMale() != null && model.getMale().equals("0")) {

            rb_woman.setChecked(true);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
                if (radioButton.getText().toString().equals("男声")) {
                    model.setMale("1");
                    PreferencesUtil.getInstance().keepField("mmale",  "1",mContext);
                } else {
                    model.setMale("0");
                    PreferencesUtil.getInstance().keepField("mmale",  "0",mContext);
                }

            }
        });
    }

    public interface OnViewClickListener {
        /***
         * 响应确定按钮
         *
         * @param v
         */
        public void onConfirmClick(View v);


        /***
         * 对话框消失回调
         */
        public void dialogDismiss();


    }

    /***
     * 显示对话框
     */
    public Dialog show() {

        if (dialog != null) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            if (!((Activity) mContext).isFinishing() && !dialog.isShowing()) {
                dialog.show();
            }
        }
        return dialog;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm_bt://确定按钮
                if (listener != null) {

                    listener.onConfirmClick(v);
                }
                if(!CacheConstants.HAS_XUNFEI_ENGINE){
                    ToastUtil.showToast(mContext,"非讯飞引擎参数设置无效");
                }
                if (dialog != null) {
                    dialog.cancel();
                }
                break;

        }
    }
}
