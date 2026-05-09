package com.htgd.radiocontrol.aeroradiocontrol.model.requestModel;


import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by zongwei on 2017-07-29.
 */
public class GetTokenModel extends BaseModel {
    private String username;
    private String userpwd;
    public GetTokenModel(String username ,String userpwd){
        this.username  = username ;
        this.userpwd = userpwd;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserpwd() {
        return userpwd;
    }

    public void setUserpwd(String userpwd) {
        this.userpwd = userpwd;
    }
}
