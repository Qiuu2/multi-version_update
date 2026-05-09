package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.util.Log;

/**
 * Created by wzq on 2017-07-12.
 */
public class LogUtils {
    public static LogUtils  instance;
    public static final String tag = "htgd_tech";
    public static LogUtils getInstance(){
        synchronized (LogUtils.class){
           if(instance==null) {
               instance = new LogUtils();
           }
        }
        return  instance;
    }

    public static void setLog(String msg){
        Log.e(tag,msg);
       /* LogToFile.e(tag,msg);*/
    }

    public static void setLog(String tag,String msg){
        Log.e(tag,msg);
        /*LogToFile.e(tag,msg);*/
    }

    public static void setLogError(String msg){
        Log.e(tag,msg);
      /*  LogToFile.e(tag,msg);*/
    }
}
