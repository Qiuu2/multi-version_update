package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;

import java.util.ArrayList;


/**
 * Created by wzq on 2017-07-19.
 */
public class GridViewAdapter extends BaseAdapter {
    private ArrayList<MachineInfo> machineArrayList;
    private Context mContext;
    public GridViewAdapter(Context context, ArrayList<MachineInfo> signMachines) {
        this.machineArrayList = signMachines;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        if (machineArrayList != null && machineArrayList.size() > 0 && 0 != machineArrayList.get(0).getAll()) {
            return machineArrayList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return machineArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_gridview_click, null);
            initView(convertView, viewHolder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (machineArrayList.get(position).isChoose()) {
            viewHolder.itemImage.setImageResource(R.mipmap.machine_all_click_image);
            viewHolder.itemText.setTextColor(mContext.getResources().getColor(R.color.colorBlue));
        } else {
            if (machineArrayList.get(position).getTaskstate() == 0) {
                viewHolder.itemImage.setImageResource(R.mipmap.machine_all_image);
                viewHolder.itemText.setTextColor(mContext.getResources().getColor(R.color.LightBlack));
            } else {
                viewHolder.itemImage.setImageResource(R.mipmap.machine_all_image);
                viewHolder.itemText.setTextColor(mContext.getResources().getColor(R.color.colorRed));
            }
        }
        viewHolder.itemText.setText(machineArrayList.get(position).getName());
        return convertView;
    }
    private class ViewHolder {
        LinearLayout itemAll;
        ImageView itemImage;
        TextView itemText;
    }
    private void initView(View view, ViewHolder viewHolder) {
        viewHolder.itemAll = (LinearLayout) view.findViewById(R.id.item_gridview_click);
        viewHolder.itemImage = (ImageView) view.findViewById(R.id.item_gridview_image);
        viewHolder.itemText = (TextView) view.findViewById(R.id.item_gridview_text);
    }
    public void setMachineArrayList(ArrayList<MachineInfo> machineArrayList) {
        this.machineArrayList = machineArrayList;
    }
    public void refrech(ArrayList<MachineInfo> list) {
        this.machineArrayList = list;
        this.notifyDataSetChanged();
    }
}
