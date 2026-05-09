package com.htgd.radiocontrol.aeroradiocontrol.component;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import java.util.List;


/**
 * Created by wzw on 2017/12/13.
 */
public abstract class IRefreshAdapter extends BaseAdapter {

    protected List list = null;
    protected Context context;
    private IListView iListView = null;


    public List getList() {
        return list;
    }

    public IRefreshAdapter( Context context,List list) {
        this.list = list;
        this.context = context;
    }



    public void setiListView(IListView iListView) {
        this.iListView = iListView;
    }



    @Override
    public View getView(final int position, View contentView, ViewGroup arg2) {

        ViewHolder holder = new ViewHolder();
        View view02 = LayoutInflater.from(context).inflate(R.layout.listview_delete_item, null);
        holder.btn_delete = (Button) view02.findViewById(R.id.platform_component_listview_delete_item_id_item_btn);
        holder.btn_reuse = (Button) view02.findViewById(R.id.platform_component_listview_reuse_item_id_item_btn);
        holder.btn_change = (Button) view02.findViewById(R.id.platform_component_listview_change_item_id_item_btn);

        contentView = new  SwipeItemLayout(contentView, view02, null, null);

        holder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iListView.getDeleteItemListener().DeleteItem(position);
            }
        });
        holder.btn_reuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iListView.getReuseItemListener().ReuseItem(position);
            }
        });
        holder.btn_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iListView.getChangeItemListener().ChangeItem(position);
            }
        });

        return contentView;
    }

    class ViewHolder {
        Button btn_delete;
        Button btn_reuse;
        Button  btn_change;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (list != null)
            count = list.size();
        return count;
    }

    @Override
    public Object getItem(int arg0) {
        Object object = null;
        if (list != null)
            object = list.get(arg0);

        return object;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public void notifyDataSetChanged() {
        this.iListView.loadingFinish();
        super.notifyDataSetChanged();
    }
}
