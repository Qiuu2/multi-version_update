package com.htgd.radiocontrol.aeroradiocontrol.model;

/**
 * Created by wzw on 2017/12/4.
 */

public class DataModel   {

    private  String name;
    private  String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DataModel(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
