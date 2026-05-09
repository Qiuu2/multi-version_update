package com.htgd.radiocontrol.aeroradiocontrol.widget.dialog;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.utils.AudioUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;


/**
 * 作者：wzq
 * 时间：2018/11/5:9:18
 * 邮箱：535708929
 * 说明：音量设置对话框
 */
public class VolumeSetDialog   implements View.OnClickListener {
    private Context mContext;
    private Button btConfirm;
    private Button btCancle;
    private TextView callVolumeNum;
    private TextView speakVolumeNum;
    private TextView playVolumeNum;
    private SeekBar callVolume;
    private SeekBar speakVolume;
    private SeekBar playVolume;
    private String mTag = "VolumeDialog";
    private int mediaMaxVolume;
    private View view;

    public VolumeSetDialog(Context context ) {
        super();
        this.mContext = context;
        mediaMaxVolume = AudioUtil.getInstance(mContext).getMediaMaxVolume();
        initView();
    }

    private void initView() {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        view = inflater.inflate(R.layout.dialog_volume, null);
        btConfirm = (Button) view.  findViewById(R.id.positiveButton1);
        btConfirm.setOnClickListener(this);
        btCancle = (Button)   view. findViewById(R.id.positiveButton2);
        btCancle.setOnClickListener(this);
        callVolumeNum = (TextView) view.   findViewById(R.id.call_vol_num);
        speakVolumeNum = (TextView)  view.  findViewById(R.id.speak_vol_num);
        playVolumeNum = (TextView)  view.  findViewById(R.id.play_vol_num);
        callVolume = (SeekBar)  view.  findViewById(R.id.call_volume);
        speakVolume = (SeekBar)  view.  findViewById(R.id.speak_volume);
        playVolume = (SeekBar)  view.  findViewById(R.id.play_volume);

        callVolume.setMax(AudioUtil.getInstance(mContext).getMediaMaxVolume());
        speakVolume.setMax(AudioUtil.getInstance(mContext).getMediaMaxVolume());
        playVolume.setMax(AudioUtil.getInstance(mContext).getMediaMaxVolume());
        if(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext)==null) {
           PreferencesUtil.getInstance().keepField(CacheConstants.callvolume,"80",mContext);
           PreferencesUtil.getInstance().keepField(CacheConstants.speakvolume,"80",mContext);
           PreferencesUtil.getInstance().keepField(CacheConstants.playvolume,"80",mContext);
        }
        callVolumeNum.setText(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext));
        callVolume.setProgress((Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.callvolume, mContext))) * mediaMaxVolume / 100);
        speakVolumeNum.setText(PreferencesUtil.getInstance().getField(CacheConstants.speakvolume, mContext));
        speakVolume.setProgress((Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.speakvolume, mContext))) * mediaMaxVolume / 100);
        playVolumeNum.setText(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext));
        playVolume.setProgress((Integer.parseInt(PreferencesUtil.getInstance().getField(CacheConstants.playvolume, mContext))) * mediaMaxVolume / 100);

        callVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                callVolumeNum.setText(callVolume.getProgress()*100/mediaMaxVolume + "");

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        speakVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speakVolumeNum.setText(speakVolume.getProgress()*100/mediaMaxVolume + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        playVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playVolumeNum.setText(playVolume.getProgress()*100/mediaMaxVolume + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.positiveButton1://确定按钮
                PreferencesUtil.getInstance().keepField(CacheConstants.callvolume,callVolumeNum.getText().toString() ,mContext);
                PreferencesUtil.getInstance().keepField(CacheConstants.speakvolume,speakVolumeNum.getText().toString(),mContext);
                PreferencesUtil.getInstance().keepField(CacheConstants.playvolume,playVolumeNum.getText().toString(),mContext);

                break;
            case R.id.positiveButton2://取消按钮

                break;
        }
    }
}
