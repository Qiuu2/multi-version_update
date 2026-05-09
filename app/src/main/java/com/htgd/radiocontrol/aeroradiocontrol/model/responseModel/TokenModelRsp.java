package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by zongwei on 2017-08-01.
 */
public class TokenModelRsp implements Serializable {
    private ArrayList<TokenModel> data;

    public ArrayList<TokenModel> getData() {
        return data;
    }

    public void setData(ArrayList<TokenModel> data) {
        this.data = data;
    }
}
