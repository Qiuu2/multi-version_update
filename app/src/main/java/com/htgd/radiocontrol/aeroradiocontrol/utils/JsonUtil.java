package com.htgd.radiocontrol.aeroradiocontrol.utils;

import com.google.gson.Gson;

import org.json.JSONObject;

/**
 * json工具類，將對象序列化成json字符串或者將字符串反序列化成對象
 *
 * @author wzq
 */
public class JsonUtil {

    private JsonUtil() {

    }

    private static volatile JsonUtil singleton;

    public static JsonUtil getInstance() {
        if (singleton == null) synchronized (JsonUtil.class) {
            if (singleton == null) singleton = new JsonUtil();
        }
        return singleton;
    }

    /**
     * 序列化對象為json字符�?	 *
     *
     * @param t 泛型對象
     * @return
     */
    public <T> String serializeObject(T t) {
        String str = "";
        if (t != null) {
            Gson gson = new Gson();
            try {
                if (t.getClass().getName().endsWith("String")) {
                    str = (String) t;
                } else {
                    str = gson.toJson(t);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return str;
    }

    /**
     * 对象转成json
     *
     * @param t
     * @return
     */
    public <T> JSONObject serializeObjectJson(T t) {
        JSONObject obj = null;
        if (t != null) {
            Gson gson = new Gson();
            try {
                String str = gson.toJson(t);
                obj = new JSONObject(str);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return obj;
    }

    /**
     * 反序列化字符串為對象
     *
     * @param jsonStr json字符�?	 * @param cls
     *                對應的數據bean
     * @return
     */
    public <T> T deSerializeString(String jsonStr, Class<T> cls) {
        T obj = null;
        if (! ValueUtil.isEmpty(jsonStr)) {
            Gson gson = new Gson();
            try {
                obj = gson.fromJson(jsonStr, cls);
            } catch (Exception e) {

            }
        }
        return obj;
    }

}
