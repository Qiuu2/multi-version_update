package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;


/**
 * Created by zongwei on 2017-08-07.
 */
public class GVMachineAdapter extends BaseAdapter{
    private Context context;
    private ArrayList<MachineInfo> machineInfos;
    private LinearLayout allItem_ll;
    private TextView  machineName_tv;
    public GVMachineAdapter(Context context, ArrayList<MachineInfo> machineInfos){
        this.context = context;
        this.machineInfos = machineInfos;
    }
    @Override
    public int getCount() {
        if(machineInfos!=null && machineInfos.size()>0 && 0!=machineInfos.get(0).getAll()){
            return  machineInfos.size();
        }else{
            return  0;
        }
    }

    @Override
    public MachineInfo getItem(int position) {
        return machineInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            LogUtils.setLog("the gridview size is" +getCount());
            convertView = LayoutInflater.from(context).inflate(R.layout.item_only_one_text,null);
            allItem_ll  = (LinearLayout)convertView.findViewById(R.id.item_all);
            machineName_tv = (TextView)convertView.findViewById(R.id.item_gridview_text);
            machineName_tv.setText(machineInfos.get(position).getName());
        }
        return convertView;
    }

    public void setMachineInfos(ArrayList<MachineInfo> machineInfos) {
        this.machineInfos = machineInfos;
    }
}
