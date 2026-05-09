package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wzw on 2017/12/29.
 */

public class CommonRsp  implements Serializable {
    private ArrayList<TempTTSModel> data;

    public ArrayList<TempTTSModel> getData() {
        return data;
    }

    public void setData(ArrayList<TempTTSModel> data) {
        this.data = data;
    }
}
