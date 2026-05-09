package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzw on 2017/10/26.
 */

public class ShortCutKeyListRsp implements Serializable{
    private ArrayList<ShorCutKey> data;

    public ArrayList<ShorCutKey> getData() {
        return data;
    }

    public void setData(ArrayList<ShorCutKey> data) {
        this.data = data;
    }
}
