package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;


/**
 * 作者：wzq
 * 时间：2021/1/20:9:55
 * 邮箱：535708929
 * 说明：分区列表适配器
 */
public class ZoneAdapter extends BaseAdapter {
    private Context mContext;
    private String mTag = "ZoneAdapter";
    private ArrayList<ZoneModel> list;
    private int currentposition;

    public void setListener(GetZoneTerminalListener listener) {
        this.listener = listener;
    }

    public interface GetZoneTerminalListener {
        public void getZoneTerminals(int i);
    }

    private GetZoneTerminalListener listener;

    public ZoneAdapter(Context context, ArrayList<ZoneModel> zonelist) {
        this.mContext = context;
        this.list = zonelist;

    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_text, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.s_name);
            viewHolder.menu = (LinearLayout) view.findViewById(R.id.menu_item);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.name.setText(list.get(position).getName());
        if (currentposition == position) {

            viewHolder.menu.setBackgroundColor(mContext.getResources().getColor(R.color.side_side_text));
        } else {
            viewHolder.menu.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
        }
        ViewHolder finalViewHolder = viewHolder;
        viewHolder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.setLog(mTag, "获取分区终端监听触发" + list.get(position).getId());
                currentposition = position;
                LogUtils.setLog(mTag, "当前选择的是第" + position + "项");
                notifyDataSetChanged();

                // finalViewHolder.menu.setBackgroundColor(mContext.getResources().getColor( R.color.side_side_text));
                listener.getZoneTerminals(list.get(position).getId());
            }
        });

        return view;
    }


    private class ViewHolder {
        public LinearLayout menu;
        private TextView name;
    }
}
