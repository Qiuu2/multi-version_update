package com.htgd.radiocontrol.aeroradiocontrol.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Created by wzw on 2017/9/27.
 */

public class TalkService extends Service{

    private final String TAG ="TalkService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "in onCreate");}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "in onStartCommand");
        Log.w(TAG, "MyService:" + this);
        String name = intent.getStringExtra("name");
        Log.w(TAG, "name:" + name);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "in onDestroy");
        TalkAsytask asytask = new TalkAsytask();
        asytask.execute();
    }

    public class TalkAsytask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }
        @Override
        public void onPostExecute(Object result) {
            super.onPostExecute(result);
        }
    }



}
