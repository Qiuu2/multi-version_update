package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.ProgressStyle;


/**
 * Author: wzq
 * Time: 2019/5/13 0013 16:55
 * Email: 1666755369@qq.com
 * Created by:
 */

public class MyLRecycView extends LRecyclerView {
    private int mColor = 0;

    public MyLRecycView(Context context) {
        super(context);
        init();
    }

    public MyLRecycView(Context context, int color) {
        super(context);
        this.mColor = color;
        init();
    }

    public MyLRecycView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyLRecycView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        if (mColor != 0) {
            setBackgroundColor(getResources().getColor(mColor));
        }
        //设置下拉刷新
        setRefreshProgressStyle(ProgressStyle.BallGridPulse);
        setLoadingMoreProgressStyle(ProgressStyle.BallGridPulse);
    }

//
    @Override
    public void setOnRefreshListener(OnRefreshListener listener) {
        super.setOnRefreshListener(listener);
/*
        list_recyclerview.clear();//recycleyView 的数据list

        //重写recyclerView的onTouch事件

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
           @Override

           public boolean onTouch(View v, MotionEvent event) {
                return true;//返回true

            }
    }*/
    }
}
