package com.htgd.radiocontrol.aeroradiocontrol.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.TtsModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2020/11/4:14:18
 * 邮箱：535708929
 * 说明：星期几选择
 */
public class DialogWeekSelect extends Dialog {
    private  Consumer<String> consumer;
    private TaskGuangboModel model;
    private TtsModel models;
    private CheckBox monday;
    private CheckBox tuesday;
    private CheckBox wednesday;
    private CheckBox thursday;
    private CheckBox friday;
    private CheckBox saturday;
    private CheckBox sunday;
    private Context mContext;
    private String mTag="DialogWeekSelect";

    public DialogWeekSelect(@NonNull Context mContext , TaskGuangboModel model, Boolean isFirst, Consumer<String> consumer) {
        super(mContext);
        this.mContext=mContext;
        this.consumer=consumer;
        this.model=model;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.week_select);
        initView();
    }

    private void initView() {
        monday = (CheckBox) findViewById(R.id.monday);
        tuesday = (CheckBox) findViewById(R.id.tuesday);
        wednesday = (CheckBox) findViewById(R.id.wednesday);
        thursday = (CheckBox) findViewById(R.id.thursday);
        friday = (CheckBox) findViewById(R.id.friday);
        saturday = (CheckBox) findViewById(R.id.saturday);
        sunday = (CheckBox) findViewById(R.id.sunday);
        Button confirm = (Button) findViewById(R.id.confirm);


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!sunday.isChecked()&&!monday.isChecked()&&!tuesday.isChecked()&&!wednesday.isChecked()&&!thursday.isChecked()&&!friday.isChecked()&&!saturday.isChecked()){
                    ToastUtil.showToast(mContext, ChinaConstants.please_select_weekday);

                }else {
                    LogUtils.setLog("执行模式设置"+model.getExecmode());
                    if(model!=null) {
                        model.setExecmode(setExecmode());
                    }else{
                        models.setExecmode(setExecmode());
                    }
                    dismiss();
                }
                LogUtils.setLog("执行模式设置"+model.getExecmode());

                Observable.just(TaskMainMethod.setWeekDay(model.getExecmode())).subscribe(consumer);

            }
        });

    }

    //设置星期几的显示
    private int setExecmode() {
        ArrayList<Integer> day = new ArrayList<Integer>();

        if (sunday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (monday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (tuesday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (wednesday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (thursday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (friday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        if (saturday.isChecked()) {
            day.add(1);
        } else {
            day.add(0);
        }
        String erjinzhi="";
        for (int i = 0; i < 7; i++) {
            erjinzhi=erjinzhi+day.get(i);
        }
        int execMode = Integer.parseInt(erjinzhi, 2);
        LogUtils.setLog(mTag,"执行模式星期几"+execMode);
        return  execMode;

    }
}
