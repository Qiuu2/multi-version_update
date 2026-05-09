package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;

import java.util.List;


/**
 * Created by wzw on 2018/3/17.
 */

public class HorizontalAdapter extends BaseAdapter{
    private List<ZoneModel> mList;
    private Context mContext;
    private int selectItem;

    public HorizontalAdapter(List<ZoneModel> mList, Context mContext) {
        super();
        this.mList = mList;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {

        if(mList != null){

            return mList.size();
        }else{

            return 0;
        }
    }

    @Override
    public Object getItem(int position) {

        if(mList != null){

            return mList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {

        return position;
    }
    public void setSelectItem(int selectItem) {
        this.selectItem = selectItem;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();

            convertView=LayoutInflater.from(mContext).inflate(R.layout.item_fen_name,null);
            holder.item_name =(TextView)convertView.findViewById(R.id.t_name);
            holder.bb=(RelativeLayout)convertView.findViewById(R.id.bb);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (mList != null && mList.get(position) != null) {


            holder.item_name.setText(mList.get(position).getName());
        }
        if(position==selectItem){
            holder.item_name.setTextColor(Color.parseColor("#3986f9") );
            holder.bb.setBackgroundResource(R.color.colorBuleDark);

        }else {
            holder.item_name.setTextColor(Color.parseColor("#000000") );
            holder.bb.setBackgroundResource(R.color.colorBarBottom);
        }

        return convertView;
    }

    class ViewHolder {

        TextView   item_name;
        RelativeLayout bb;
    }

}
