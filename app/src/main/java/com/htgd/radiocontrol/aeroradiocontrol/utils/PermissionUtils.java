package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

/**
 * 作者：wzq
 * 时间：2021/7/20:10:17
 * 邮箱：535708929
 * 说明：请求权限的常用方法
 */
public class PermissionUtils {

    private   PackageManager pm;
    private   Context mContext;
    private String mTag="PermissionUtils";

    public PermissionUtils(Context context) {
        this.mContext=context;
          pm = mContext.getPackageManager();
    }

    public void requestPermission(String s){
        String[] permissionStr = new String[]{s};

        ActivityCompat.requestPermissions((Activity)mContext, permissionStr, 6);
    }
    public void judgePermission(String s){

        boolean permission = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission(s, "packageName"));
        if (!permission) {
            LogUtils.setLog(mTag,"请求权限");
             requestPermission(  s  /*Manifest.permission.WRITE_EXTERNAL_STORAGE*/);
        }
    }
}
