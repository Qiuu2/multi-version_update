package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * @author Kevin
 * @email kevin.guo@snowballtech.com
 * @Description TODO
 * @date 2015年1月22日 上午11:34:28
 */
public class PreferencesUtil {

    private static PreferencesUtil instance;

    private PreferencesUtil() {

    }

    public static PreferencesUtil getInstance() {
        if (instance == null) {
            instance = new PreferencesUtil();
        }
        return instance;
    }

    private SharedPreferences getSharedPreferences(String name, Context context) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * @param
     * @email kevin.guo@snowballtech.com
     * @Description 保存单个字段
     * @date 2015年1月22日 上午11:46:28
     * @author Kevin
     */
    public void keepField(String key, String value, Context context) {
        Editor editor = getSharedPreferences(key, context).edit();
        editor.putString(key, value);
        editor.commit();
    }


    public String getField(String key, Context context) {
        return getSharedPreferences(key, context).getString(key, "");
    }

    /**
     * @param
     * @email kevin.guo@snowballtech.com
     * @Description 保存实体
     * @date 2015年1月22日 上午11:46:52
     * @author Kevin
     */
    public <T> void keepEntity(T t, Context context) {
        String key = t.getClass().getName();
        String value = JsonUtil.getInstance().serializeObject(t);
        keepField(key, value, context);
    }

    /**
     * @param
     * @return
     * @description 返回存储的bean类
     * @creator travel
     * @create_email chentangqijava@163.com
     * @create_date Feb 17, 2015 12:55:18 PM
     * @modifier travel
     * @modify_date Feb 17, 2015 time12:55:18 PM
     * @modify_email chentangqijava@163.com
     */
    public <T> T getEntity(Class<T> clazz, Context context) {
        String key = clazz.getName();
        String value = getField(key,context);
        return JsonUtil.getInstance().deSerializeString(value, clazz);
    }

    /**
     * @param
     * @email kevin.guo@snowballtech.com
     * @Description 保存实体
     * @date 2015年1月22日 上午11:46:52
     * @author Kevin
     */
    public <T> void keepEntity(String key, T t, Context context) {
        String value = JsonUtil.getInstance().serializeObject(t);
        keepField(key, value, context);
    }

    /**
     * @param
     * @return
     * @description 返回存储的bean类
     * @creator travel
     * @create_email chentangqijava@163.com
     * @create_date Feb 17, 2015 12:55:18 PM
     * @modifier travel
     * @modify_date Feb 17, 2015 time12:55:18 PM
     * @modify_email chentangqijava@163.com
     */
    public <T> T getEntity(String key, Class<T> clazz, Context context) {
        String value = getField(key, context);
//        LogUtil.log(" the  frist list is =  1024  " + value);
        return JsonUtil.getInstance().deSerializeString(value, clazz);
    }

}
