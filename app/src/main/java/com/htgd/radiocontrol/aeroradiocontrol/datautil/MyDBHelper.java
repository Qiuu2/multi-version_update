package com.htgd.radiocontrol.aeroradiocontrol.datautil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

/**
 * Created by wzw on 2017/12/8.
 */

public class MyDBHelper extends SQLiteOpenHelper {
    private static final String name = "htgd"; //数据库名称
    private static final int version = 1; //数据库版本
    public static final String CREATE_MUSIC_BOOK = "create table musicbook (" +
            "_id integer primary key autoincrement, " +
            "mediaid text, " +
            "folderid text, " +
            "name text, "+
             "isfile text )";
    public static final String CREATE_BOOK = "create table temporarytask (" +
            "_id integer primary key autoincrement, " +
            "content text, " +
            "volume text, " +
            "speed text, " +
            "cycle text, " +
            "taskid text, " +
            "male text, " +
            "sort text, " +
            "taskname text, " +
            "terminal text, " +
            "createtime text, " +
            "timelength text, " +
            "timelengthtype text, " +
            "priority text, " +
            "state text, " +
            "number text, " +
            "username text, " +
            "zonename text, " +
            "mediaurl text, " +
            "tag text, " +
            "ip )";

    public static final String CREATE_Terminal_BOOK = "create  table terminalbook (" +
            "_id integer primary key autoincrement, " +
            "type text, " +
            "taskstate text, " +
            "devicestate text, " +
            "netstate text, " +
            "speechstate text, " +
            "volume text, " +
            "isinstancy text, " +
            "zone text, " +
            "name text, " +
            "ip text, " +
            "id text, " +
            "groupid text, " +
            "longitude text, " +
            "latitude text)";
    public static final String CREATE_TEST_BOOK= "create table testsdbook (" +
            "_id integer primary key autoincrement, " +
            "type text, " +
            "taskstate text, " +
            "id text )";
    private String mTag="MyDBHelper";


    public  MyDBHelper(Context context,int version) {
        /**
         * 参数说明：
         * 第一个参数： 上下文
         * 第二个参数：数据库的名称
         * 第三个参数：null代表的是默认的游标工厂
         * 第四个参数：是数据库的版本号  数据库只能升级,不能降级,版本号只能变大不能变小
         */
        super(context, "htgd", null, version);
    }


    /**
     * onCreate是在数据库创建的时候调用的，主要用来初始化数据表结构和插入数据初始化的记录
     * <p>
     * 当数据库第一次被创建的时候调用的方法,适合在这个方法里面把数据库的表结构定义出来.
     * 所以只有程序第一次运行的时候才会执行
     * 如果想再看到这个函数执行，必须写在程序然后重新安装这个app
     */

    @Override
    public void onCreate(SQLiteDatabase db) {
        LogUtils.setLog(mTag,"创建数据库表");
        db.execSQL(CREATE_BOOK);
        db.execSQL(CREATE_Terminal_BOOK);
    }


    /**
     * 当数据库更新的时候调用的方法
     * 这个要显示出来得在上面的super语句里面版本号发生改变时才会 打印  （super(context, "itheima.db", null, 2); ）
     * 注意，数据库的版本号只可以变大，不能变小，假设我们当前写的版本号是3，运行，然后又改成1，运行则报错。不能变小
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.setLog("更新数据库版本" + newVersion);
        for (int i = oldVersion; i < newVersion; i++) {
            switch (i) {
                case 1:
                  //  upToDbVersion2(db);
                    break;
                case 2:
                    //upToDbVersion3(db);
                    break;
                default:
                    break;
            }
           /* private void addUpgradeToVersion2(SQLiteDatabase db) {//注意添加两个字段时要分开写
                String sql1 = "ALTER TABLE " + DataBaseConfig.TABLE_TEST1 + " ADD COLUMN sex  varchar";
                String sql2 = "ALTER TABLE " + DataBaseConfig.TABLE_TEST1 + " ADD COLUMN address varchar";
                db.execSQL(sql1);
                db.execSQL(sql2);
            } */
           /* db.execSQL("alter table temporarytask add account varchar(20)");
            db.execSQL("alter table musicbook add account varchar(20)");
            db.execSQL("alter table machinebook add account varchar(20)");*/
            // db.execSQL(CREATE_TEST_BOOK);
        }


    }

    private void upToDbVersion2(SQLiteDatabase db) {
        db.execSQL(CREATE_TEST_BOOK);
    }


}


