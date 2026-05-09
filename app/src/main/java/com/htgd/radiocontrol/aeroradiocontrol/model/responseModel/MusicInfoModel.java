package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;

import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeId;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeIsfile;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeName;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodePid;
import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;

/**
 * Created by wzq on 2017-08-07.
 */
public class MusicInfoModel extends BaseModel {
    @TreeNodePid
    private int folderid;
    @TreeNodeId
    private int mediaid;
    private int size;
    private String format;
    private int bitrate;
    @TreeNodeIsfile
    private String isFile;
    private int length;
    @TreeNodeName
    private String name;
    private boolean choose =false;

    public String getIsFile() {
        return isFile;
    }

    public void setIsFile(String isFile) {
        this.isFile = isFile;
    }

    public MusicInfoModel(){

    }
    public MusicInfoModel( int mediaid,int folderid, String name,String isFile) {
        this.folderid = folderid;
        this.mediaid = mediaid;
        this.name = name;
        this.isFile=isFile;
    }


    public int getFolderid() {
        return folderid;
    }

    public void setFolderid(int folderid) {
        this.folderid = folderid;
    }

    public int getMediaid() {
        return mediaid;
    }

    public void setMediaid(int mediaid) {
        this.mediaid = mediaid;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChoose() {
        return choose;
    }

    public void setChoose(boolean choose) {
        this.choose = choose;
    }
}
