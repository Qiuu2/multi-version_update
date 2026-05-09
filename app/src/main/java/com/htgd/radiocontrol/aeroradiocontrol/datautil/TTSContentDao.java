package com.htgd.radiocontrol.aeroradiocontrol.datautil;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by wzw on 2017/12/8.
 */

public class TTSContentDao {
    private  MyDBHelper mMyDBHelper;
    private Cursor c;
    private String mTag = "数据库";

    /**
     * dao类需要实例化数据库Help类,只有得到帮助类的对象我们才可以实例化 SQLiteDatabase
     *
     * @param context
     */
    public TTSContentDao(Context context) {
        mMyDBHelper = new  MyDBHelper(context,1);
    }

    // 将数据库打开帮帮助类实例化，然后利用这个对象
    // 调用谷歌的api去进行增删改查

    // 增加的方法吗，返回的的是一个long值
    public void addDate(TempTTSModel model) {
        // 增删改查每一个方法都要得到数据库，然后操作完成后一定要关闭
        // getWritableDatabase(); 执行后数据库文件才会生成
        // 数据库文件利用DDMS可以查看，在 data/data/包名/databases 目录下即可查看
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("taskname", model.getTaskname());
        contentValues.put("content", model.getContent());
        contentValues.put("createtime", model.getCreatetime());
        contentValues.put("volume", model.getVolume());
        contentValues.put("speed", model.getSpeed());
        contentValues.put("male", model.getMale());
        contentValues.put("timelength", model.getTimelength());
        contentValues.put("timelengthtype", model.getTimelengthtype());
        contentValues.put("sort", model.getSort());
        contentValues.put("priority", model.getPriority());
        contentValues.put("terminal", model.getTerminal());
        contentValues.put("taskid", model.gettaskid());
         contentValues.put("state",model.getState());
        contentValues.put("number",model.getNumber());
        contentValues.put("username",model.getUsername());
        contentValues.put("zonename",model.getZonename());
        contentValues.put("mediaurl",model.getMediaurl());
        contentValues.put("tag",model.getTag());
        contentValues.put("ip",model.getip());
        LogUtils.setLog("被当前线程锁住" + sqLiteDatabase.isDbLockedByCurrentThread());
        LogUtils.setLog("被其它线程锁住" + sqLiteDatabase.isDbLockedByOtherThreads());
        long rowid = sqLiteDatabase.insert("temporarytask", null, contentValues);
        LogUtils.setLog(mTag,"数据库" + "插入成功"+rowid);
        sqLiteDatabase.close();
        //  return rowid;
    }


    // 删除的方法，返回值是int
    public int deleteDate(String time) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        int deleteResult = sqLiteDatabase.delete("temporarytask", "createtime=?", new String[]{time});
        sqLiteDatabase.close();
        return deleteResult;
    }

    public int updateSort(String taskid, String sort) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("sort", sort);
        int updateResult = sqLiteDatabase.update("temporarytask", contentValues, "taskid=?", new String[]{taskid});
        sqLiteDatabase.close();
        return updateResult;
    }
    public int updateState(String taskid, String newState) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("state", newState);
        int updateResult = sqLiteDatabase.update("temporarytask", contentValues, "taskid=?", new String[]{taskid});
        sqLiteDatabase.close();
        return updateResult;
    }
    public int updateTag(String taskid, String tag) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", tag);
        int updateResult = sqLiteDatabase.update("temporarytask", contentValues, "taskid=?", new String[]{taskid});
        sqLiteDatabase.close();
        return updateResult;
    }
    /**
     * 查询的方法
     *
     * @param
     * @return
     */
    @SuppressLint("Range")
    public TempTTSModel alterDate(String time) {
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();
        // 查询比较特别,涉及到 cursors
        Cursor cursors = readableDatabase.rawQuery("select * from temporarytask where  createtime = '" + time + "' order by createtime desc limit 2", null);
       // Cursor cursor = readableDatabase.query("temporarytask", new String[]{"time"}, "name=?", new String[]{name}, null, null, null);
        TempTTSModel model=new TempTTSModel();
        if (cursors.moveToNext()) {
            String _id = cursors.getString(cursors.getColumnIndex("_id"));
            LogUtils.setLog("当前取到第几" + _id + "行");
            @SuppressLint("Range") String content = cursors.getString(cursors.getColumnIndex("content"));
            String volume = cursors.getString(cursors.getColumnIndex("volume"));
            String speed = cursors.getString(cursors.getColumnIndex("speed"));
            String taskid = cursors.getString(cursors.getColumnIndex("taskid"));
            String male = cursors.getString(cursors.getColumnIndex("male"));
            String sort = cursors.getString(cursors.getColumnIndex("sort"));
            String taskname = cursors.getString(cursors.getColumnIndex("taskname"));
            String terminal = cursors.getString(cursors.getColumnIndex("terminal"));
            String createtime = cursors.getString(cursors.getColumnIndex("createtime"));
            LogUtils.setLog("使用历史记录的创建时间"+createtime);
            String timelength = cursors.getString(cursors.getColumnIndex("timelength"));
            String timelengthtype = cursors.getString(cursors.getColumnIndex("timelengthtype"));
            String priority = cursors.getString(cursors.getColumnIndex("priority"));
            String state = cursors.getString(cursors.getColumnIndex("state"));
            String number = cursors.getString(cursors.getColumnIndex("number"));
            String username = cursors.getString(cursors.getColumnIndex("username"));
            String zonename = cursors.getString(cursors.getColumnIndex("zonename"));
            String mediaurl = cursors.getString(cursors.getColumnIndex("mediaurl"));
            String tag = cursors.getString(cursors.getColumnIndex("tag"));
            String ip = cursors.getString(cursors.getColumnIndex("ip"));
            model=  new TempTTSModel(content, volume, speed,  taskid, male, sort, taskname, createtime, timelength, priority, timelengthtype, terminal,state,number,username,zonename,mediaurl,tag,ip);
        }

        cursors.close(); // 记得关闭 corsor
        readableDatabase.close(); // 关闭数据库
        return model;
    }
    @SuppressLint("Range")
    public ArrayList<TempTTSModel> getNearData(String time, String id, String ip) {
        ArrayList<TempTTSModel> list = new ArrayList<TempTTSModel>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();

       /* if (readableDatabase.isOpen()) {
            Cursor cursors = readableDatabase.rawQuery("select * from temporarytask where  createtime < '" + time + "' and  zonename  ='" + id + "' and ip ='" + ip + "' order by createtime desc limit 10", null);
             while (cursors.moveToNext()) {
                String _id = cursors.getString(cursors.getColumnIndex("_id"));
                 LogUtils.setLog(mTag,"当前取到第几" + _id + "行");
                String content = cursors.getString(cursors.getColumnIndex("content"));
                String volume = cursors.getString(cursors.getColumnIndex("volume"));
                String speed = cursors.getString(cursors.getColumnIndex("speed"));
                String taskid = cursors.getString(cursors.getColumnIndex("taskid"));
                String male = cursors.getString(cursors.getColumnIndex("male"));
                String sort = cursors.getString(cursors.getColumnIndex("sort"));
                String taskname = cursors.getString(cursors.getColumnIndex("taskname"));
                String terminal = cursors.getString(cursors.getColumnIndex("terminal"));
                String createtime = cursors.getString(cursors.getColumnIndex("createtime"));
                 LogUtils.setLog(mTag,time+"当前取到的时间" + createtime  );
                String timelength = cursors.getString(cursors.getColumnIndex("timelength"));
                String timelengthtype = cursors.getString(cursors.getColumnIndex("timelengthtype"));
                String priority = cursors.getString(cursors.getColumnIndex("priority"));
                 String state = cursors.getString(cursors.getColumnIndex("state"));
                 String number = cursors.getString(cursors.getColumnIndex("number"));
                 String username = cursors.getString(cursors.getColumnIndex("username"));
                 String zonename = cursors.getString(cursors.getColumnIndex("zonename"));
                 String mediaurl = cursors.getString(cursors.getColumnIndex("mediaurl"));
                 String tag = cursors.getString(cursors.getColumnIndex("tag"));
                 String ips = cursors.getString(cursors.getColumnIndex("ip"));
                list.add(new TempTTSModel(content, volume, speed,  taskid, male, sort, taskname, createtime, timelength, priority, timelengthtype, terminal,state,number,username,zonename,mediaurl,tag,ips));
           }
        }*/
        if (readableDatabase.isOpen()) {

            Cursor cursors = readableDatabase.rawQuery("select * from temporarytask where  createtime < '" + time + "' and  zonename  ='" + id + "' and ip ='" + ip + "' order by createtime desc limit 10", null);
            while (cursors.moveToNext()) {
                String _id = cursors.getString(cursors.getColumnIndex("_id"));
                LogUtils.setLog(mTag,"当前取到第几" + _id + "行");

                String content = cursors.getString(cursors.getColumnIndex("content"));
                String volume = cursors.getString(cursors.getColumnIndex("volume"));
                String speed = cursors.getString(cursors.getColumnIndex("speed"));
                String taskid = cursors.getString(cursors.getColumnIndex("taskid"));
                String male = cursors.getString(cursors.getColumnIndex("male"));
                String sort = cursors.getString(cursors.getColumnIndex("sort"));
                String taskname = cursors.getString(cursors.getColumnIndex("taskname"));
                String terminal = cursors.getString(cursors.getColumnIndex("terminal"));
                String createtime = cursors.getString(cursors.getColumnIndex("createtime"));
                LogUtils.setLog(mTag,time+"当前取到的时间" + createtime  );
                String timelength = cursors.getString(cursors.getColumnIndex("timelength"));
                String timelengthtype = cursors.getString(cursors.getColumnIndex("timelengthtype"));
                String priority = cursors.getString(cursors.getColumnIndex("priority"));
                String state = cursors.getString(cursors.getColumnIndex("state"));
                String number = cursors.getString(cursors.getColumnIndex("number"));
                String username = cursors.getString(cursors.getColumnIndex("username"));
                String zonename = cursors.getString(cursors.getColumnIndex("zonename"));
                String mediaurl = cursors.getString(cursors.getColumnIndex("mediaurl"));
                String tag = cursors.getString(cursors.getColumnIndex("tag"));
                String ips = cursors.getString(cursors.getColumnIndex("ip"));
                list.add(new TempTTSModel(content, volume, speed,  taskid, male, sort, taskname, createtime, timelength, priority, timelengthtype, terminal,state,number,username,zonename,mediaurl,tag,ips));
            }
        }
        readableDatabase.close(); // 关闭数据库
        return list;
    }

}
