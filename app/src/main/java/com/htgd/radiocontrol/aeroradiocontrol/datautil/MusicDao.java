package com.htgd.radiocontrol.aeroradiocontrol.datautil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;


/**
 * Created by wzw on 2018/4/11.
 */

public class MusicDao {

    private  MyDBHelper mMyDBHelper;
    private Cursor c;
    private String mTag = "数据库";

    /**
     * dao类需要实例化数据库Help类,只有得到帮助类的对象我们才可以实例化 SQLiteDatabase
     *
     * @param context
     */
    public MusicDao(Context context) {
        mMyDBHelper = new  MyDBHelper(context,1);
    }

    // 增加的方法吗，返回的的是一个long值
    public void addDate(MusicInfoModel model) {
        // 增删改查每一个方法都要得到数据库，然后操作完成后一定要关闭
        // getWritableDatabase(); 执行后数据库文件才会生成
        // 数据库文件利用DDMS可以查看，在 data/data/包名/databases 目录下即可查看
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", model.getName());
        contentValues.put("folderid", model.getFolderid());
        contentValues.put("mediaid", model.getMediaid());
        contentValues.put("isfile", model.getIsFile());

        long rowid = sqLiteDatabase.insert("musicbook", null, contentValues);

        sqLiteDatabase.close();
        //  return rowid;
    }
    /**
     * 查询的方法
     *
     * @param
     * @return
     */
    public ArrayList<MusicInfoModel> getAllData() {
        ArrayList<MusicInfoModel> list = new ArrayList<MusicInfoModel>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();

        if (readableDatabase.isOpen()) {
           LogUtils.setLog("getread");
            Cursor cursors = readableDatabase.rawQuery("select * from musicbook", null);
            while (cursors.moveToNext()) {
                String _id = cursors.getString(cursors.getColumnIndex("_id"));
                String folderid = cursors.getString(cursors.getColumnIndex("folderid"));
                String mediaid = cursors.getString(cursors.getColumnIndex("mediaid"));
                String name = cursors.getString(cursors.getColumnIndex("name"));
                String isfile = cursors.getString(cursors.getColumnIndex("isfile"));
                list.add( new MusicInfoModel( Integer.parseInt(folderid),Integer.parseInt(mediaid),name,isfile ));
            }

        }

        LogUtils.setLog("shujuliang",list.size()+"");
        readableDatabase.close(); // 关闭数据库
        return list;
    }
    /**
     * 查询的方法
     *
     * @param
     * @return
     */
    public ArrayList<MusicInfoModel> getfolderData(int fid) {
        ArrayList<MusicInfoModel> list = new ArrayList<MusicInfoModel>();
        SQLiteDatabase readableDatabase = mMyDBHelper.getReadableDatabase();

        if (readableDatabase.isOpen()) {

            Cursor cursors = readableDatabase.rawQuery("select * from musicbook where folderid='" + fid + "' order by mediaid", null);
            while (cursors.moveToNext()) {
                String _id = cursors.getString(cursors.getColumnIndex("_id"));
                String folderid = cursors.getString(cursors.getColumnIndex("folderid"));
                String mediaid = cursors.getString(cursors.getColumnIndex("mediaid"));
                String name = cursors.getString(cursors.getColumnIndex("name"));
                String isfile = cursors.getString(cursors.getColumnIndex("isfile"));
                list.add(0,new MusicInfoModel( Integer.parseInt(folderid),Integer.parseInt(mediaid),name ,isfile));
            }

        }
        readableDatabase.close(); // 关闭数据库
        return list;
    }

}
