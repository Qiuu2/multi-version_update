package com.htgd.radiocontrol.aeroradiocontrol.httptask;

import android.content.Context;

import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;

import java.util.HashMap;


/**
 * Created by zongwei on 2017-08-01.
 */
public class MyRequestBuilder extends BaseModel {
    private String url;
    private boolean needToken = false;
    private HashMap<String, String> bodyMap;
    private Context mContext;
    public MyRequestBuilder(Context  context){
        this.mContext = context ;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if(!Constant.serveraddress.contains("//")) {
            Constant.serveraddress = PreferencesUtil.getInstance().getField(Constring.serverAddress, mContext);
        }
        this.url =     Constant.serveraddress + url;
    }
    public void setUrl(String url,String s) {
        this.url =    url;
    }

    public boolean isNeedToken() {
        return needToken;
    }

    public void setNeedToken(boolean needToken) {
        this.needToken = needToken;
    }

    public HashMap<String, String> getBodyMap() {
        return bodyMap;
    }

    public void setBodyMap(HashMap<String, String> bodyMap) {
        this.bodyMap = bodyMap;
    }
}
