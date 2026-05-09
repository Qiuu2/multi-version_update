package com.htgd.radiocontrol.aeroradiocontrol.model;

import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by wzq on 2017-07-18.
 */
public class SignMachine extends BaseModel {
    private int SignMachineId;
    private int bigImageId;
    private int image1;
    private int image2;
    private int image3;
    private int image4;
    private String textMsg1;
    private String textMsg2;
    private String textMsg3;
    private String textMsg4;
    private boolean  isShow = true;
    private boolean  isSelect = false;

    public int getBigImageId() {
        return bigImageId;
    }

    public void setBigImageId(int bigImageId) {
        this.bigImageId = bigImageId;
    }

    public int getImage1() {
        return image1;
    }

    public void setImage1(int image1) {
        this.image1 = image1;
    }

    public int getImage2() {
        return image2;
    }

    public void setImage2(int image2) {
        this.image2 = image2;
    }

    public int getImage3() {
        return image3;
    }

    public void setImage3(int image3) {
        this.image3 = image3;
    }

    public int getImage4() {
        return image4;
    }

    public void setImage4(int image4) {
        this.image4 = image4;
    }

    public String getTextMsg1() {
        return textMsg1;
    }

    public void setTextMsg1(String textMsg1) {
        this.textMsg1 = textMsg1;
    }

    public String getTextMsg2() {
        return textMsg2;
    }

    public void setTextMsg2(String textMsg2) {
        this.textMsg2 = textMsg2;
    }

    public String getTextMsg3() {
        return textMsg3;
    }

    public void setTextMsg3(String textMsg3) {
        this.textMsg3 = textMsg3;
    }

    public String getTextMsg4() {
        return textMsg4;
    }

    public void setTextMsg4(String textMsg4) {
        this.textMsg4 = textMsg4;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setIsShow(boolean isShow) {
        this.isShow = isShow;
    }

    public int getSignMachineId() {
        return SignMachineId;
    }

    public void setSignMachineId(int signMachineId) {
        SignMachineId = signMachineId;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }
}
