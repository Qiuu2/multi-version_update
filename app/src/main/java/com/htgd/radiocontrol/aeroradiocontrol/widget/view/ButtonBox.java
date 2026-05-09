package com.htgd.radiocontrol.aeroradiocontrol.widget.view;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;

import com.htgd.radiocontrol.aeroradiocontrol.R;


/**
 * 作者：wzq
 * 时间：2021/1/14:14:25
 * 邮箱：535708929
 * 说明：按钮
 */
public class ButtonBox extends LinearLayout {
    private Context mContext;
    private View view;
    private Button button;
    public String result = "1";
    private String mTag = "ButtonBox";
    private Spinner spinner;

    public ButtonBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        view = LayoutInflater.from(context).inflate(R.layout.box_button, this);
        button = (Button) view.findViewById(R.id.button);
    }

    public void setName(String name) {
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setText(name);
    }

    public void setNameAndColor(String name, int i) {
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setTextColor(i);
        textView.setText(name);
    }

    public void setButton(final OnClickListener listener) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
            }
        });
    }

    public void setBtnShowOrHide(boolean t) {
        Button btt = (Button) view.findViewById(R.id.btn_to_tv);
        if (t) {
            btt.setVisibility(VISIBLE);
        } else {
            btt.setVisibility(GONE);
        }
    }

    public Button getButton() {
        return button;
    }

    public void setButtonText(String s) {
        button.setText(s);
    }

    public void setButtonTextHint(String s) {
        button.setHint(s);
    }
}
