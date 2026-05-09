package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.SocketClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;

/**
 * Created by wzq on 2017/9/28.
 * 开机界面
 */

public class SignActivity  extends BaseActivity {
    private String mTag = "SignActivity";
    private Context mContext;
    private Thread thread;
     private WeakReference  reference;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sign;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        getThePermission();
        setgifs();
    }

    private void setgifs() {
        ImageView ivGif = (ImageView) findViewById(R.id.open);
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
         Glide.with(mContext).load(R.drawable.open_anim).apply(options).into(ivGif);


        thread= new Thread() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                try {
                    this.currentThread().sleep(400 );//开机动画的时间
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                reference = new WeakReference<>(mContext);

                if (endTime - startTime > 300 ) {
                    LogUtils.setLog(mTag,"跳主界面");

                    startActivity(new Intent((SignActivity)reference.get(),LoginActivity.class));
                }
            }
        };
       thread.start();
       //
    }

    //获取权限
    private void getThePermission() {
        String[] permissionStr = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_APN_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_APN_SETTINGS,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.DISABLE_KEYGUARD,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
                Manifest.permission.ACCESS_FINE_LOCATION,

        };
        ActivityCompat.requestPermissions(this, permissionStr, 6);
        for (int i = 0; i < permissionStr.length; i++) {
            boolean flag = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            LogUtils.setLog(permissionStr[i] + "is have" + flag);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.setLog(mTag,"onPause"+"销毁thread");
        if(thread!=null){
            thread=null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.setLog(mTag,"onDestroy");
        finish();
        if(thread!=null){
            thread=null;
        }
    }
}
