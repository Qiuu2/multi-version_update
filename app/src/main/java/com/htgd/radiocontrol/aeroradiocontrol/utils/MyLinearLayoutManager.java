package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 作者：wzq
 * 时间：2020/6/15:15:42
 * 邮箱：535708929
 * 说明：
 */
public class MyLinearLayoutManager extends LinearLayoutManager {
    public MyLinearLayoutManager(Context context) {
        super(context);
    }
    public MyLinearLayoutManager(Context context,int orientation,boolean reverseLayout){
        super(context,orientation,reverseLayout);
    }
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
