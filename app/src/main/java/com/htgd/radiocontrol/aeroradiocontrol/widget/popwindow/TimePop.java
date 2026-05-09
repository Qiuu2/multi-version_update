package com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;


import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.TtsModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2021/2/1:17:25
 * 邮箱：535708929
 * 说明：选择时间pop
 */
public class TimePop {

    private Consumer<String> consumer;
    private   Button button;
    private String  name;
    private Context mContext;
    private String  mTag="TimePop";
    private TaskGuangboModel model;
    private TtsModel models;
    public TimePop(Context context, String name, TaskGuangboModel model, Button button) {
        this.mContext = context;
        this.name=name;
        this.model=model;
        this.button=button;
        if(name.contains("日期")){
            LogUtils.setLog(mTag,"显示日期弹窗" );
            showDatePicker();
        }else {
            showTimePicker();
            LogUtils.setLog(mTag,"显示时间弹窗" );
        }
    }
    public TimePop(Context context, String name, TtsModel model, Button button) {
        this.mContext = context;
        this.name=name;
        this.models=model;
        this.button=button;
        if(name.contains("日期")){
            LogUtils.setLog(mTag,"显示日期弹窗" );
            showDatePicker();
        }else {
            showTimePicker();
            LogUtils.setLog(mTag,"显示时间弹窗" );
        }
    }
    public TimePop(Context context, Consumer<String> consumer){
        this.consumer=consumer;
        this.mContext = context;
        showTimeAndDataPicker();
    }
    private void showDatePicker() {
        Calendar selectedDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();

        //正确设置方式 原因：注意事项有说明
        startDate.set(2021, 0, 1);
        endDate.set(2050, 11, 31);

        TimePickerView pvTime = new TimePickerBuilder(mContext, new   OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {//选中事件回调
                SimpleDateFormat dateFormat2=new SimpleDateFormat("yyyy-MM-dd");
                String  dates=dateFormat2.format(date);
                button.setText(dates);
                LogUtils.setLog(mTag,"button set text "+dates);
                if(name.contains("开始")){
                    if(model!=null){
                        model.setStartdate(dates);
                    }
                    if(models!=null){
                       models.setStartdate(dates);
                    }
                }else{
                    if(model!=null){
                        model.setEnddate(dates);
                    }
                    if(models!=null){
                        models.setEnddate(dates);
                    }

                }
            }
        })
                .setType(new boolean[]{true, true, true, false, false, false})// 默认全部显示
                .setCancelText("取消")//取消按钮文字
                .setSubmitText("确认")//确认按钮文字
                .setTitleSize(20)//标题文字大小
                .setTitleText("日期选择")//标题文字
                .setOutSideCancelable(true)//点击屏幕，点在控件外部范围时，是否取消显示
                .isCyclic(true)//是否循环滚动
                .setTitleColor(Color.BLACK)//标题文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色 Night mode
                .setBgColor(Color.WHITE)//滚轮背景颜色 Night mode
                .setDate(selectedDate)// 如果不设置的话，默认是系统时间*/
                .setRangDate(startDate, endDate)//起始终止年月日设定
                .setLabel("年", "月", "日", "时", "分", "秒")//默认设置为年月日时分秒
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .isDialog(true)//是否显示为对话框样式
                .build();

        pvTime.show();
    }

    private void showTimePicker() {
        TimePickerView pvTime = new TimePickerBuilder(mContext, new   OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {//选中事件回调
                SimpleDateFormat dateFormat2=new SimpleDateFormat("HH:mm:ss");
                String  times=dateFormat2.format(date);
                button.setText(times);
                LogUtils.setLog(mTag,"button set text "+times);
                if(name.contains(ChinaConstants.starttime)){
                    model.setStarttime(times);
                }else{
                    model.setTimelength(times);
                }
                LogUtils.setLog(mTag,"所选时间"+times);
            }
        })
                .setType(new boolean[]{ false, false, false,true, true, true})// 默认全部显示
                .setCancelText("取消")//取消按钮文字
                .setSubmitText("确认")//确认按钮文字
                .setTitleSize(20)//标题文字大小
                .setTitleText("时间设定")//标题文字
                .setOutSideCancelable(true)//点击屏幕，点在控件外部范围时，是否取消显示
                .isCyclic(true)//是否循环滚动
                .setTitleColor(Color.BLACK)//标题文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色 Night mode
                .setBgColor(Color.WHITE)//滚轮背景颜色 Night mode
                .setLabel("年", "月", "日", "时", "分", "秒")//默认设置为年月日时分秒
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .isDialog(true)//是否显示为对话框样式
                .build();

        pvTime.show();
    }
    //设置日期和时间
    private void showTimeAndDataPicker() {
        Calendar selectedDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();

        //正确设置方式 原因：注意事项有说明
        startDate.set(2021, 0, 1);
        endDate.set(2050, 11, 31);
        TimePickerView pvTime = new TimePickerBuilder(mContext, new   OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {//选中事件回调
                SimpleDateFormat dateFormat2=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String  times=dateFormat2.format(date);
                 Observable.just(times).subscribe(consumer);
                LogUtils.setLog(mTag,"button set text "+times);

            }
        })
                .setType(new boolean[]{ true, true, true,true, true, true})// 默认全部显示
                .setCancelText("取消")//取消按钮文字
                .setSubmitText("确认")//确认按钮文字
                .setTitleSize(20)//标题文字大小
                .setTitleText("时长选择")//标题文字
                .setOutSideCancelable(true)//点击屏幕，点在控件外部范围时，是否取消显示
                .isCyclic(true)//是否循环滚动
                .setTitleColor(Color.BLACK)//标题文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色 Night mode
                .setBgColor(Color.WHITE)//滚轮背景颜色 Night mode
                .setLabel("年", "月", "日", "时", "分", "秒")//默认设置为年月日时分秒
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .isDialog(true)//是否显示为对话框样式
                .build();

        pvTime.show();
    }


}
