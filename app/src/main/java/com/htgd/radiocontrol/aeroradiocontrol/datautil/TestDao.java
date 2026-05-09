package com.htgd.radiocontrol.aeroradiocontrol.datautil;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

public class TestDao {
    private final MyDBHelper mMyDBHelper;

    public TestDao(Context context) {mMyDBHelper = new  MyDBHelper(context,1);
    }

    public void addData(MachineInfo model) {
        SQLiteDatabase sqLiteDatabase = mMyDBHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("taskname", model.getZone());
        long rowid = sqLiteDatabase.insert("testbook", null, contentValues);
        LogUtils.setLog("数据库添加test");
        sqLiteDatabase.close();
    }
}
