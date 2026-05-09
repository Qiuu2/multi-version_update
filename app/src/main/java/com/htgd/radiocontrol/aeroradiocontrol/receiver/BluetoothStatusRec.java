package com.htgd.radiocontrol.aeroradiocontrol.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

public class BluetoothStatusRec extends BroadcastReceiver {
    private String TAG = "BluetoothStatusRec";
    public static AudioManager mAudioManager ;
    private TurnOffBtn mTurnOffBtn;
    public static boolean mIsScoEnable = false;
    public BluetoothStatusRec(TurnOffBtn ins){
        mTurnOffBtn = ins;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if(intent.getAction() == BluetoothDevice.ACTION_ACL_CONNECTED){
            Log.d(TAG,"链接成功");
            mTurnOffBtn.turnOn();
            mTurnOffBtn.changeBtn();
            context.registerReceiver(new BroadcastReceiver() {    //动态注册一个接受SCO状态改变的BroadcastReceiver
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                    Log.d(TAG, "Audio SCO state: " + state);
                    if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {  //SCO打开后(由手机输入变为蓝牙耳机输入)，进行录音
                        mAudioManager.setBluetoothScoOn(true);
                        context.unregisterReceiver(this);       //记得接收后解除注册
                        //startRecord();     //进行录音，此时声源来自蓝牙耳机
                        mTurnOffBtn.changeBtn();
                        mTurnOffBtn.turnOn();
                        Log.d("ww","链接sco成功");
                    }else{
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mAudioManager.startBluetoothSco();
                        mTurnOffBtn.turnOff();
                        mTurnOffBtn.changeBtnOn();
                    }
                }
            }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            mAudioManager.startBluetoothSco();
        }if(intent.getAction() == BluetoothDevice.ACTION_ACL_DISCONNECTED){
            mTurnOffBtn.turnOff();
            mTurnOffBtn.changeBtnOn();
        }
    }
    public interface TurnOffBtn{
        void turnOff();
        void turnOn();
        void changeBtn();//链接蓝牙后禁用上面的start,stop按钮
        void changeBtnOn();//链接蓝牙后启用上面的start,stop按钮
    }

}
