package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by wzq on 2016/5/20.
 */
public class ToastUtil {
    public static void showToastOnUIThread(final Activity activity, final CharSequence text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast t = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }
        });
    }

    public static void showToast(Activity activity, CharSequence text) {
        Toast t = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void showToast(Context context, String text) {
        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void showToast(Context context, String text, int gravity) {
        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.setGravity(gravity, 0, 0);
        t.show();
    }
}
