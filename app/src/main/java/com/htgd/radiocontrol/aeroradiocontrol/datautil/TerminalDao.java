package com.htgd.radiocontrol.aeroradiocontrol.datautil;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

public class TerminalDao {
    private  MyDBHelper mMyDBHelper;
    private Cursor c;
    private String mTag = "数据库TerminalDao";

    /**
     * dao类需要实例化数据库Help类,只有得到帮助类的对象我们才可以实例化 SQLiteDatabase
     *
     * @param context
     */
    public TerminalDao(Context context) {
        mMyDBHelper = new  MyDBHelper(context,1);
    }

    public void addDate(MachineInfo model) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
               LogUtils.setLog(mTag,"type "+model.getType()+"taskstate "+model.getTaskstate()+"devicestate "+model.getDevicestate()+
               "netstate "+model.getNetstate()+"speechstate "+model.getSpeechstate()+"volume "+model.getVolume()+
               "isinstancy "+model.getIsinstancy()+"zone "+model.getZone()+"name "+model.getName()+"ip "+model.getIp()+"id "+model.getId()
               +"longitude"+model.getLongitude()+"latitude"+model.getLatitude());
        contentValues.put("type", model.getType()+"");
        contentValues.put("taskstate", model.getTaskstate()+"");
        contentValues.put("devicestate", model.getDevicestate()+"");
        contentValues.put("netstate", model.getNetstate()+"");
        contentValues.put("speechstate", model.getSpeechstate()+"");
        contentValues.put("volume", model.getVolume()+"");
        contentValues.put("isinstancy", model.getIsinstancy()+"");
        contentValues.put("zone", model.getZone()+"");
        contentValues.put("name", model.getName());
        contentValues.put("ip", model.getIp());
        contentValues.put("id", model.getId()+"");
        contentValues.put("longitude", model.getLongitude() );
        contentValues.put("latitude", model.getLatitude() );
        long machinebook = sqLiteDatabase.insert("machinebook", null, contentValues);
        LogUtils.setLog(mTag,"插入成功"+machinebook  );
        sqLiteDatabase.close();
    }

    // 删除的方法，返回值是int
    public int deleteDate(String id) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        int deleteResult = sqLiteDatabase.delete("machinebook", "id=?", new String[]{id});
        sqLiteDatabase.close();
        return deleteResult;
    }

    /**
     * 查询的方法
     *
     * @param
     * @return
     */
    public ArrayList<MachineInfo> getAllData() {
        ArrayList<MachineInfo> list = new ArrayList<MachineInfo>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();

        if (readableDatabase.isOpen()) {
            LogUtils.setLog("getread");
            Cursor cursors = readableDatabase.rawQuery("select * from machinebook", null);
            while (cursors.moveToNext()) {
                @SuppressLint("Range") String _id = cursors.getString(cursors.getColumnIndex("_id"));
                @SuppressLint("Range") String type = cursors.getString(cursors.getColumnIndex("type"));
                @SuppressLint("Range") String taskstate = cursors.getString(cursors.getColumnIndex("taskstate"));
                @SuppressLint("Range") String devicestate = cursors.getString(cursors.getColumnIndex("devicestate"));
                @SuppressLint("Range") String netstate = cursors.getString(cursors.getColumnIndex("netstate"));
                @SuppressLint("Range") String speechstate = cursors.getString(cursors.getColumnIndex("speechstate"));
                @SuppressLint("Range") String volume = cursors.getString(cursors.getColumnIndex("volume"));
                @SuppressLint("Range") String isinstancy = cursors.getString(cursors.getColumnIndex("isinstancy"));
                @SuppressLint("Range") String zone = cursors.getString(cursors.getColumnIndex("zone"));
                @SuppressLint("Range") String name = cursors.getString(cursors.getColumnIndex("name"));
                @SuppressLint("Range") String ip = cursors.getString(cursors.getColumnIndex("ip"));
                @SuppressLint("Range") String id = cursors.getString(cursors.getColumnIndex("id"));
                @SuppressLint("Range") String longitude = cursors.getString(cursors.getColumnIndex("longitude"));
                @SuppressLint("Range") String latitude = cursors.getString(cursors.getColumnIndex("latitude"));
                list.add( new MachineInfo(  Integer.parseInt( type),   Integer.parseInt(taskstate),   Integer.parseInt(devicestate),   Integer.parseInt(netstate),
                        Integer.parseInt(speechstate),   Integer.parseInt(volume),   Integer.parseInt(isinstancy),   Integer.parseInt(zone),   name,   ip,
                        Integer.parseInt(id), longitude,latitude));
            }
        }

        LogUtils.setLog(mTag+"shujuliang",list.size()+"");
        readableDatabase.close(); // 关闭数据库
        return list;
    }
    /**
     * 查询的方法
     *
     * @param
     * @return
     */
    public ArrayList<MachineInfo> getmachine(String  latitude,String longtitude) {
        ArrayList<MachineInfo> list = new ArrayList<MachineInfo>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();

        if (readableDatabase.isOpen()) {

            Cursor cursors = readableDatabase.rawQuery("select * from machinebook where latitude='" + latitude + "' order by id", null);
            while (cursors.moveToNext()) {
                @SuppressLint("Range") String _id = cursors.getString(cursors.getColumnIndex("_id"));
                @SuppressLint("Range") String type = cursors.getString(cursors.getColumnIndex("type"));
                @SuppressLint("Range") String taskstate = cursors.getString(cursors.getColumnIndex("taskstate"));
                @SuppressLint("Range") String devicestate = cursors.getString(cursors.getColumnIndex("devicestate"));
                @SuppressLint("Range") String netstate = cursors.getString(cursors.getColumnIndex("netstate"));
                @SuppressLint("Range") String speechstate = cursors.getString(cursors.getColumnIndex("speechstate"));
                @SuppressLint("Range") String volume = cursors.getString(cursors.getColumnIndex("volume"));
                @SuppressLint("Range") String isinstancy = cursors.getString(cursors.getColumnIndex("isinstancy"));
                @SuppressLint("Range") String zone = cursors.getString(cursors.getColumnIndex("zone"));
                @SuppressLint("Range") String name = cursors.getString(cursors.getColumnIndex("name"));
                @SuppressLint("Range") String ip = cursors.getString(cursors.getColumnIndex("ip"));
                @SuppressLint("Range") String id = cursors.getString(cursors.getColumnIndex("id"));

                 list.add(0,new MachineInfo(Integer.parseInt( type),   Integer.parseInt(taskstate),   Integer.parseInt(devicestate),   Integer.parseInt(netstate),   Integer.parseInt(speechstate),
                         Integer.parseInt(volume),   Integer.parseInt(isinstancy),   Integer.parseInt(zone),   name,   ip,   Integer.parseInt(id) ));
            LogUtils.setLog(mTag,"查询到数据库终端");
            }

        }
        readableDatabase.close(); // 关闭数据库
        return list;
    }

    public ArrayList<MachineInfo> getNearData(String time, String seewhat) {
        ArrayList<MachineInfo> list = new ArrayList<MachineInfo>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();
        if (readableDatabase.isOpen()) {

        }
        readableDatabase.close(); // 关闭数据库
        return list;
    }

    public int updateLatitudeAndLongtitude(String taskid, String latitude,String longtitude) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("latitude", latitude);
        contentValues.put("longtitude", longtitude);
        int updateResult = sqLiteDatabase.update("temporarytask", contentValues, "taskid=?", new String[]{taskid});
        sqLiteDatabase.close();
        return updateResult;
    }
}
