package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;

/**
 * Created by wzq on 2017-07-31.
 */
public class BaseResponse implements Serializable {

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
