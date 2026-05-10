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
import com.htgd.radiocontrol.aeroradiocontrol.utils.AndroidVersion;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.SocketClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Build the runtime-permission request list dynamically.
     *
     * Only "dangerous" permissions need a runtime request — install-time perms
     * (INTERNET, WAKE_LOCK, ACCESS_WIFI_STATE etc.) are granted automatically
     * once they appear in the manifest. Signature/system perms (WRITE_APN_SETTINGS,
     * BIND_ACCESSIBILITY_SERVICE, MOUNT_UNMOUNT_FILESYSTEMS) can never be
     * granted to a normal app and are silently ignored if requested.
     *
     * Storage and Bluetooth permissions changed shape across Android versions, so
     * we branch on the running OS to ask for the right ones.
     */
    private void getThePermission() {
        List<String> permissions = new ArrayList<>();

        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (AndroidVersion.requiresGranularMediaPermissions()) {
            // Android 13+: READ_EXTERNAL_STORAGE no longer grants media access;
            // ask for the per-media-type permissions instead. WRITE_EXTERNAL_STORAGE
            // is a no-op on API 30+ so we skip it on this branch.
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (AndroidVersion.requiresNewBluetoothPermissions()) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        if (AndroidVersion.requiresNotificationPermission()) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        String[] permissionArray = permissions.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissionArray, 6);

        for (String permission : permissionArray) {
            boolean granted = ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED;
            LogUtils.setLog(mTag, permission + " granted=" + granted);
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
