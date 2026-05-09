package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;

import java.util.ArrayList;


/**
 * Created by zongwei on 2017-08-23.
 */
public class GVShortTaskAdapter extends BaseAdapter{

    private ArrayList<TaskGuangboModel> taskList;
    private Context mContext;
    private ViewHolder viewHolder = new ViewHolder();
    public GVShortTaskAdapter(Context context,ArrayList<TaskGuangboModel> list){
        this.mContext = context;
        this.taskList = list;
    }

    @Override
    public int getCount() {
        if(taskList!=null && taskList.size()>0 && 0!=taskList.get(0).getAll()){
            return  taskList.size();
        }else{
            return  0;
        }
    }

    @Override
    public TaskGuangboModel getItem(int position) {
        return taskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_gridview_click,null);
            viewHolder.allItembg_rl = (LinearLayout)convertView.findViewById(R.id.item_gridview_click);
            viewHolder.taskImage_iv = (ImageView)convertView.findViewById(R.id.item_gridview_image);
            viewHolder.taskName_tv = (TextView)convertView.findViewById(R.id.item_gridview_text);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.taskName_tv.setText(taskList.get(position).getName());
        return convertView;
    }

    private class ViewHolder{
        LinearLayout allItembg_rl;
        ImageView taskImage_iv;
        TextView taskName_tv;
    }


    public void setTaskList(ArrayList<TaskGuangboModel> taskList) {
        this.taskList = taskList;
    }


    public void adapteNotifyChange(){
        this.notifyDataSetChanged();
    }

}
