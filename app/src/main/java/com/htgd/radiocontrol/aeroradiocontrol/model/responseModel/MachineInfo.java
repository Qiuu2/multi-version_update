package com.htgd.radiocontrol.aeroradiocontrol.model.responseModel;



import static com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication.getContext;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.model.LatLng;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.BaseModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.map.view.ClusterItem;
import java.io.Serializable;

/**
 * Created by wzq on 2019-08-01.
 */
public class MachineInfo extends BaseModel  implements Serializable  , ClusterItem {


    private int type;//
    private int taskstate;//任务状态
    private int devicestate;//设备状态
    private int netstate;//网络状态
    private int speechstate;//
    private int volume;//
    private int isinstancy;//
    private int zone;//
    private String name;//名称
    private String ip;//
    private int id;//
    private boolean isChoose;
    private int groupid;
    private String longitude;
    private String latitude;
    private LatLng mPosition;

    public MachineInfo(int type, int taskstate, int devicestate, int netstate, int speechstate, int volume, int isinstancy, int zone, String name, String ip, int id) {
        this.type = type;
        this.taskstate = taskstate;
        this.devicestate = devicestate;
        this.netstate = netstate;
        this.speechstate = speechstate;
        this.volume = volume;
        this.isinstancy = isinstancy;
        this.zone = zone;
        this.name = name;
        this.ip = ip;
        this.id = id;
        this. mPosition = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
    }
    public MachineInfo(int type, int taskstate, int devicestate, int netstate, int speechstate, int volume, int isinstancy, int zone, String name, String ip, int id,String longitude,String latitude) {
        this.type = type;
        this.taskstate = taskstate;
        this.devicestate = devicestate;
        this.netstate = netstate;
        this.speechstate = speechstate;
        this.volume = volume;
        this.isinstancy = isinstancy;
        this.zone = zone;
        this.name = name;
        this.ip = ip;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;

       this. mPosition = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
        Log.e("创建了经纬度",mPosition.latitude+"");
    }
    public MachineInfo(int type, int taskstate, int devicestate, int netstate, int speechstate, int volume, int isinstancy,   String name, String ip, int id,int groupid, String longitude, String latitude) {
        this.type = type;
        this.taskstate = taskstate;
        this.devicestate = devicestate;
        this.netstate = netstate;
        this.speechstate = speechstate;
        this.volume = volume;
        this.isinstancy = isinstancy;
        this.name = name;
        this.ip = ip;
        this.id = id;
        this.groupid = groupid;
        this.latitude = latitude;
        this.longitude = longitude;
        //this. mPosition = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
        // Log.e("创建了经纬度",mPosition.latitude+"");
    }
    public MachineInfo( String name, String longitude,String latitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;

        //this. mPosition = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
        //Log.e("创建了经纬度",mPosition.latitude+"");
    }
    public MachineInfo(LatLng latlng){
        this. mPosition = latlng;
    }

    public MachineInfo(int zone) {
        this.zone = zone;
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTaskstate() {
        return taskstate;
    }

    public void setTaskstate(int taskstate) {
        this.taskstate = taskstate;
    }

    public int getDevicestate() {
        return devicestate;
    }

    public void setDevicestate(int devicestate) {
        this.devicestate = devicestate;
    }

    public int getNetstate() {
        return netstate;
    }

    public void setNetstate(int netstate) {
        this.netstate = netstate;
    }

    public int getSpeechstate() {
        return speechstate;
    }

    public void setSpeechstate(int speechstate) {
        this.speechstate = speechstate;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getIsinstancy() {
        return isinstancy;
    }

    public void setIsinstancy(int isinstancy) {
        this.isinstancy = isinstancy;
    }

    public int getZone() {
        return zone;
    }

    public void setZone(int zone) {
        this.zone = zone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isChoose() {
        return isChoose;
    }

    public void setChoose(boolean choose) {
        isChoose = choose;
    }

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setmPosition(LatLng latLng){
       this. mPosition = latLng;
    }
    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public BitmapDescriptor getBitmapDescriptor() {
     /*   Log.e("machineinfo","getBitmapDescriptor"+name+isChoose+mPosition.latitude+"纬度"+mPosition.longitude+"---"+latitude+"longtitude");*/
        Bitmap viewBitmap = getViewBitmap();
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(viewBitmap);
        if(viewBitmap != null && !viewBitmap.isRecycled()){
            // 回收并且置为null
            viewBitmap.recycle();
            viewBitmap = null;
            LogUtils.setLog("回收了bitmap");
        }
        System.gc();

        return bitmapDescriptor;

    }

    private Bitmap getViewBitmap( ) {
       View addViewContent = View.inflate(getContext(),R.layout.map_bitmap, null);
        ImageView map_icon = (ImageView)addViewContent.findViewById(R.id.map_icon);
        TextView terminal_name = (TextView)addViewContent.findViewById(R.id.terminal_name);
        if(type==41){
            terminal_name.setText("分控终端： "+name);
        }else if(type==28){
            terminal_name.setText("寻呼话筒： "+name);
        }else if(type==34){
            terminal_name.setText("彩屏网络前置： "+name);
        }else if(type==0){
            terminal_name.setText("服务器： "+name);
        }else if(type==1){
            terminal_name.setText("音箱： "+name);
        }else if(type==2){
            terminal_name.setText("话筒： "+name);
        }else if(type==3){
            terminal_name.setText("双向寻呼终端： "+name);
        }else if(type==4){
            terminal_name.setText("IP前置： "+name);
        }else if(type==5){
            terminal_name.setText("IP功放： "+name);
        }else if(type==6){
            terminal_name.setText("电源管理器： "+name);
        }else if(type==7){
            terminal_name.setText("报警主机： "+name);
        }else if(type==8){
            terminal_name.setText("采样终端： "+name);
        }else if(type==9){
            terminal_name.setText("普通电脑： "+name);
        }else if(type==10){
            terminal_name.setText("MP3： "+name);
        }else if(type==11){
            terminal_name.setText("一体化音箱： "+name);
        }else if(type==12){
            terminal_name.setText("分控软件： "+name);
        }else if(type==13){
            terminal_name.setText("一键寻呼终端： "+name);
        }else if(type==14){
            terminal_name.setText("分控前置： "+name);
        }else if(type==15){
            terminal_name.setText("背景音乐： "+name);
        }else if(type==16){
            terminal_name.setText("实话接口： "+name);
        }else if(type==17){
            terminal_name.setText("手机终端： "+name);
        }else if(type==18){
            terminal_name.setText("9970分控工作站： "+name);
        }else if(type==19){
            terminal_name.setText("透传终端： "+name);
        }else if(type==20){
            terminal_name.setText("普通IP终端： "+name);
        }else if(type==21){
            terminal_name.setText("监控主机： "+name);
        }else if(type==22){
            terminal_name.setText("TTS主机： "+name);
        }else if(type==23){
            terminal_name.setText("离线终端： "+name);
        }else if(type==24){
            terminal_name.setText("简版网络功放： "+name);
        }else if(type==25){
            terminal_name.setText("简版采样终端： "+name);
        }else if(type==26){
            terminal_name.setText("网络调音台： "+name);
        }

       // Log.e("machineinfo","getBitmapDescriptor"+name+isChoose+mPosition.latitude+"纬度"+mPosition.longitude);
        if(isChoose){
            map_icon.setBackgroundResource(R.mipmap.selected);
        }else{
            map_icon.setBackgroundResource(R.mipmap.anull);
        }
        if(devicestate==1&&netstate==1){
            if(taskstate==0){
                map_icon.setImageResource(R.mipmap.map_online);
            }else{
                map_icon.setImageResource(R.mipmap.map_busyline);
            }
        }else{
            map_icon.setImageResource(R.mipmap.map_offline);
        }
        addViewContent.setDrawingCacheEnabled(true);
        addViewContent.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        addViewContent.layout(0, 0,
                addViewContent.getMeasuredWidth(),
                addViewContent.getMeasuredHeight());
        addViewContent.buildDrawingCache();

        Bitmap cacheBitmap = addViewContent.getDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        return bitmap;
    }

}
