package com.htgd.radiocontrol.aeroradiocontrol.widget.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;


/**
 * 作者：wzq
 * 时间：2020/12/25:13:26
 * 邮箱：535708929
 * 说明：封装的顶部标题栏
 */
public class TitleLayout extends LinearLayout /*implements View.OnClickListener*/{
    private   String mTag="TitleLayout";
    private   Context mContext;
    private   View view;
    private Button title_left;
    private Button title_right;




    public TitleLayout(Context context, AttributeSet attrs ) {
        super(context, attrs);
        mContext=context;
        LogUtils.setLog(mTag,"渲染视图");
        view =  LayoutInflater.from(context).inflate(R.layout.title_bar, this);
        Button back =(Button) view.findViewById(R.id.title_go_back);
        back.setVisibility(VISIBLE);
        back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    public void settitle(String s){
        TextView title = (TextView)view.findViewById(R.id.title_text);
        title.setText(s);
    }
    public void setleftButton(final Listener listener){
          title_left = (Button)view.findViewById(R.id.title_go_back);
          title_left.setVisibility(VISIBLE);
          title_left.setOnClickListener(new OnClickListener() {
              @Override
              public void onClick(View v) {
                  listener.right();
              }
          });
    }
     public void setRightButtonVisible(Boolean b){
        if(b) {
            title_right.setVisibility(VISIBLE);
        }else{
            title_right.setVisibility(GONE);
        }
     }
    public void setRightButtons(  ) {
        title_right = (Button) view.findViewById(R.id.title_button);
    }
    public void setRightButton(String s , final Listener listener ){
        title_right = (Button)view.findViewById(R.id.title_button);
        title_right.setText(s);
        title_right.setBackgroundResource(R.drawable.login_bt_style);
        title_right.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.right();
            }
        });
        title_right.setVisibility(View.VISIBLE);

    }
    public void setRightButtonLongClick(String s , final Listener listener ){
        title_right = (Button)view.findViewById(R.id.title_button);
        title_right.setText(s);
        title_right.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.right();
                return false;
            }
        });
        title_right.setVisibility(View.VISIBLE);

    }



    public interface  Listener{
        public void right();
    }
}
