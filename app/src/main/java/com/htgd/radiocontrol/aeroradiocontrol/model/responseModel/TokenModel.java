package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by zongwei on 2017-07-31.
 */
public class TokenModel extends BaseModel {
    private String token;
    private String expired_at;
    private String refresh_expired_at;
    private String priority;




    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpired_at() {
        return expired_at;
    }

    public void setExpired_at(String expired_at) {
        this.expired_at = expired_at;
    }

    public String getRefresh_expired_at() {
        return refresh_expired_at;
    }

    public void setRefresh_expired_at(String refresh_expired_at) {
        this.refresh_expired_at = refresh_expired_at;
    }
}
