package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import java.util.ArrayList;


/**
 * Created by wzw on 2018/3/22.
 */

public class CommenModel<T> {
    private ArrayList<T> data;

    public ArrayList<T> getData() {
        return data;
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }
}
