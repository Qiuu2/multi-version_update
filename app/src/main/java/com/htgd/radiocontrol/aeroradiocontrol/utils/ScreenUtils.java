package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.content.Context;

/**
 * 作者：wzq
 * 时间：2021/7/20:15:56
 * 邮箱：535708929
 * 说明：
 */
public class ScreenUtils {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale+0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    /**
     * 获取屏幕高度(px)
     */
    public static int getScreenPixelheight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    /**
     * 获取屏幕宽度(px)
     */
    public static int getScreenPixelWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

}
