package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.DataModel;

import androidx.annotation.IdRes;


/**
 * Created by wzq on 2017-07-15.
 */
public class DialogTwoButton implements View.OnClickListener, DialogInterface.OnDismissListener {
    private final DataModel model;
    private int xuanxiang;
    private Dialog dialog;
    private String buttonText;
    private View dailogView;
    private Context mContext;
    private OnViewClickListener listener;
    private TextView title_tv, main_tv;
    private Button confirm_bt_1, confirm_bt_2;
    private ImageView centerImage;
    private boolean isCancle = true;
    private LinearLayout content, content_bar, content_time_picker, content_time_pickers, content_radio_group;
    private SeekBar seek;
    private TextView myTextView;
    public int num;
    private int max;
    private NumberPicker numberpicker1, numberpicker2, numberpicker3, numberpicker4, numberpicker5, numberpicker6;
    private RadioButton rd1, rd2;
    private String result;
    private RadioGroup rg;
    private String mTag="DialogTwoButton";


    public DialogTwoButton(Context context, int a,DataModel model, OnViewClickListener listener) {//a=0为文字，1为bar，2为日期，3为时间
        super();
        this.mContext = context;
        this.listener = listener;
        this.xuanxiang = a;
        this.model=model;
        init(xuanxiang);


    }

    public void setNum(int a, int max) {
        seek.setProgress(a);
        seek.setMax(max);
        seek.setOnSeekBarChangeListener(seekListener);
        myTextView.setText("当前值为:" + a);
        num = a;
    }

    public void init(int a) {

        dailogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_two_button, null);
        initcontent();
        initSeekBar();
        initTimePicker();
        initTimePickers();
        initRadioGroup( );
        confirm_bt_1 = (Button) dailogView.findViewById(R.id.positiveButton1);
        confirm_bt_1.setOnClickListener(this);
        confirm_bt_2 = (Button) dailogView.findViewById(R.id.positiveButton2);
        confirm_bt_2.setOnClickListener(this);
        centerImage = (ImageView) dailogView.findViewById(R.id.centre_image);

        dialog = new Dialog(mContext);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 去掉背景
        dialog.setContentView(dailogView);
        dialog.setOnDismissListener(this);
        dialog.setCanceledOnTouchOutside(false);
        setDialogUnableDismiss();
        content.setVisibility(View.GONE);
        content_bar.setVisibility(View.GONE);
        content_time_picker.setVisibility(View.GONE);
        content_radio_group.setVisibility(View.GONE);
        switch (a) {
            case 0:
                content.setVisibility(View.VISIBLE);
                break;
            case 1:
                content_bar.setVisibility(View.VISIBLE);
                break;
            case 2:
                content_time_picker.setVisibility(View.VISIBLE);

                break;
            case 3:
                content_time_pickers.setVisibility(View.VISIBLE);

                break;
            case 4:
                content_radio_group.setVisibility(View.VISIBLE);
                break;
        }

    }

    private void initcontent() {
        content = (LinearLayout) dailogView.findViewById(R.id.content);
        title_tv = (TextView) dailogView.findViewById(R.id.dialog_title);
        main_tv = (TextView) dailogView.findViewById(R.id.dialog_message);
    }

    private void initSeekBar() {
        content_bar = (LinearLayout) dailogView.findViewById(R.id.content_bar);
        myTextView = (TextView) dailogView.findViewById(R.id.myTextView);
        seek = (SeekBar) dailogView.findViewById(R.id.mySeekBar);
        setNum(num, max);
    }

    private void initTimePicker() {
        content_time_picker = (LinearLayout) dailogView.findViewById(R.id.content_time_picker);
        numberpicker1 = (NumberPicker) dailogView.findViewById(R.id.numberpicker1);
        numberpicker2 = (NumberPicker) dailogView.findViewById(R.id.numberpicker2);
        numberpicker3 = (NumberPicker) dailogView.findViewById(R.id.numberpicker3);
        numberpicker3.setMinValue(1);
        numberpicker3.setMaxValue(31);
        numberpicker3.setValue(day);
        numberpicker3.setFocusable(true);
        numberpicker3.setFocusableInTouchMode(true);
        numberpicker3.setOnValueChangedListener(dayChangedListener);
        numberpicker2.setMaxValue(12);
        numberpicker2.setMinValue(1);
        numberpicker2.setValue(month);
        numberpicker2.setFocusable(true);
        numberpicker2.setFocusableInTouchMode(true);
        numberpicker2.setOnValueChangedListener(monthChangedListener);
        numberpicker1.setMinValue(2017);
        numberpicker1.setMaxValue(2030);
        numberpicker1.setValue(year);
        numberpicker1.setFocusable(true);
        numberpicker1.setFocusableInTouchMode(true);
        numberpicker1.setOnValueChangedListener(yearChangedListener);

    }

    public void initTimePickers() {
        content_time_pickers = (LinearLayout) dailogView.findViewById(R.id.content_time_pickers);
        numberpicker4 = (NumberPicker) dailogView.findViewById(R.id.numberpicker4);
        numberpicker5 = (NumberPicker) dailogView.findViewById(R.id.numberpicker5);
        numberpicker6 = (NumberPicker) dailogView.findViewById(R.id.numberpicker6);


        numberpicker6.setMinValue(0);
        numberpicker6.setMaxValue(59);
        numberpicker6.setValue(second);
        numberpicker6.setFocusable(true);
        numberpicker6.setFocusableInTouchMode(true);
        numberpicker6.setOnValueChangedListener(secondChangedListener);
        numberpicker5.setMinValue(0);
        numberpicker5.setMaxValue(59);
        numberpicker5.setValue(minite);
        numberpicker5.setFocusable(true);
        numberpicker5.setFocusableInTouchMode(true);
        numberpicker5.setOnValueChangedListener(miniteChangedListener);
        numberpicker4.setMinValue(0);
        numberpicker4.setMaxValue(23);
        numberpicker4.setValue(hour);
        numberpicker4.setFocusable(true);
        numberpicker4.setFocusableInTouchMode(true);
        numberpicker4.setOnValueChangedListener(hourChangedListener);

    }

    private void initRadioGroup( ) {
        content_radio_group = (LinearLayout) dailogView.findViewById(R.id.content_radio_group);

        rg=(RadioGroup)dailogView.findViewById(R.id.rg);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton)rg.findViewById(rg.getCheckedRadioButtonId());
                result= radioButton.getText().toString();
                model.setValue(result);

            }
        });
        rd1 = (RadioButton) dailogView.findViewById(R.id.rd1);
        rd2 = (RadioButton) dailogView.findViewById(R.id.rd2);
        setRdText("", "");
        rg.setOnClickListener(this);

    }

    public void setRdText(String a, String b) {
        rd1.setText(a);
        rd2.setText(b);
    }

    private int year, month, day, hour, minite, second;
    //设置月份改变监听
    private NumberPicker.OnValueChangeListener monthChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            month = numberpicker2.getValue();
            switch (month) {
                case 1:
                case 3:
                case 5:
                case 7:
                case 8:
                case 10:
                case 12:
                    numberpicker3.setMaxValue(31);
                    break;
                case 2:
                    numberpicker3.setMaxValue(29);
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    numberpicker3.setMaxValue(30);
                    break;

                default:
                    break;
            }
        }

    };
    private NumberPicker.OnValueChangeListener dayChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            day = numberpicker3.getValue();

        }

    };
    //
    private NumberPicker.OnValueChangeListener yearChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            year = numberpicker1.getValue();
        }

    };

    private NumberPicker.OnValueChangeListener hourChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            hour = numberpicker4.getValue();

        }

    };
    private NumberPicker.OnValueChangeListener miniteChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            minite = numberpicker5.getValue();

        }

    };
    private NumberPicker.OnValueChangeListener secondChangedListener = new NumberPicker.OnValueChangeListener() {

        @Override
        public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            second = numberpicker6.getValue();

        }

    };


    public SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            result = progress + "";
            myTextView.setText("当前值为:" + progress);
            model.setValue(result);
        }
    };


    public void setButtonText(String titleText, String mainText) {
        main_tv.setText(mainText);
        title_tv.setText(titleText);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.positiveButton1://确定按钮
                if (listener != null) {
                    listener.onConfirmClick(v);
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;
            case R.id.positiveButton2://取消按钮
                if (listener != null) {
                    listener.onCancelClick(v);
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;

        }
    }

    /**
     * 设置谭框不能消失
     */
    public void setDialogUnableDismiss() {
        dialog.setCancelable(true);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    public interface OnViewClickListener {

        public void onConfirmClick(View v);

        public void onCancelClick(View v);

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

    public void someBodyCalling() {
        main_tv.setVisibility(View.GONE);
        centerImage.setVisibility(View.VISIBLE);
        centerImage.setImageResource(R.drawable.aimin_talk_other);
        AnimationDrawable animationDrawable1 = (AnimationDrawable) centerImage.getDrawable();
        animationDrawable1.start();
    }

    /***
     * 检查对话框 是否已经显示
     *
     * @return
     */
    public boolean isShowing() {
        if (dialog != null && dialog.isShowing()) {
            return true;
        } else {
            return false;
        }
    }

    /***
     * 取消对话框
     */
    public void cancel() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }

    public void setCancle(boolean isCancle) {
        this.isCancle = isCancle;
    }
}
