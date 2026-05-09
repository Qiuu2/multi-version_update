package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * 作者：wzq
 * 时间：2021/7/6:18:26
 * 邮箱：535708929
 * 说明：
 */
public class CustomScrollViewPager extends ViewPager {
    private boolean scrollable = false;

    public CustomScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomScrollViewPager(Context context) {
        super(context);
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        if (scrollable)
            return false;
        else
            return super.onTouchEvent(arg0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        if (scrollable)
            return false;
        else
            return super.onInterceptTouchEvent(arg0);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
    }
}
