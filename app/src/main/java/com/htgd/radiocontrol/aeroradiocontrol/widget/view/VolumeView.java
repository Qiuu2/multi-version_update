package com.htgd.radiocontrol.aeroradiocontrol.widget.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2020/12/30:9:47
 * 邮箱：535708929
 * 说明：音量控制
 */
public class VolumeView extends LinearLayout {
    private final View view;
    private String mTag = "VolumeView";
    private SeekBar volumeBar;
    private TextView volumeTv;

    public VolumeView(Context context, @Nullable AttributeSet attrs ) {
        super(context, attrs);
        view = LayoutInflater.from(context).inflate(R.layout.weight_task_volume, this);
    }



    public void setVolume(int volume) {
        LogUtils.setLog(mTag,"设置音量"+volume);
        volumeBar.setProgress(volume);
        volumeTv.setText(volume+"");
    }

    public int getVolume(final Consumer<String> s) {

        final int[] progre = {80};
        volumeBar = (SeekBar) view.findViewById(R.id.volume_bar);
        volumeTv = (TextView) view.findViewById(R.id.volume_tv);
        volumeBar.setProgress(80);
        volumeTv.setText("80");
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeTv.setText(progress + "");
                Observable.just(progress+"").subscribe(s);
                LogUtils.setLog(mTag, "yinliangs" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progre[0] = seekBar.getProgress();
            }
        });
        volumeTv.setText(progre[0] + "");
        LogUtils.setLog(mTag, "yinliang" + progre[0]);
        return progre[0];
    }

}
