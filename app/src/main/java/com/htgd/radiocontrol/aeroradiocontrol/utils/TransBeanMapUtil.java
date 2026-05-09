package com.htgd.radiocontrol.aeroradiocontrol.utils;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by zongwei on 2017-07-29.
 */
public class TransBeanMapUtil {
    private final String TAG = "TransBeanMapUtil";

    /**
     *   map 实例化成bean
     * @param t
     * @param map
     * @param <T>
     */
    public static <T extends Object>  void transMapBean(T t,HashMap<String,String> map){
         if(t != null && map!=null){
             Class  clazz = t.getClass();
             Field[] fields = clazz.getDeclaredFields();
             for(;clazz!=Object.class;clazz = clazz.getSuperclass()){
                 for(int i= 0;i<fields.length;i++){
                     String name =fields[i].getName();
                     Object value  = map.get(name);
                     fields[i].setAccessible(true);
                     try {
                         fields[i].set(t,value);
                     } catch (IllegalAccessException e) {
                         e.printStackTrace();
                     }
                 }
             }
         }
    }

    /**
     *   将实体装换成map
     * @param b
     * @param <B>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <B extends  Object> HashMap<String,String> transBeanToMap(B b){
        HashMap<String,String> map = new HashMap<>();
        Class<B> clazz = (Class<B>) b.getClass();
        if(clazz!=null){
            while (clazz!= Object.class){
                Field[] fields = clazz.getDeclaredFields();
                for(int i=0; i<fields.length ; i++){
                    fields[i].setAccessible(true);
                    try {
                        if(null!=fields[i].get(b)){
                            map.put(fields[i].getName(),fields[i].get(b).toString());
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e){
                        e.printStackTrace();
                    }

                }
                clazz = (Class<B>) clazz.getSuperclass();
            }
        }
        return  map;
    }


}
