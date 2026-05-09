package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by zongwei on 2017-08-08.
 */
public class MusicListAdapter extends BaseAdapter{
    private Context context;
    private ArrayList<MusicInfoModel> musicLists;

    public MusicListAdapter(Context context , ArrayList<MusicInfoModel> musicInfoModels ){
        this.context = context;
        this.musicLists = musicInfoModels;
    }
    @Override
    public int getCount() {
        if(musicLists!=null && musicLists.size()>0 && 0!=musicLists.get(0).getAll()){
            return  musicLists.size();
        }else{
            return  0;
        }
    }

    @Override
    public MusicInfoModel getItem(int position) {
        return musicLists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        final int index = position;
        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music_info,null);
            viewHolder.itemALl_rl = (LinearLayout)convertView.findViewById(R.id.item_all);
            viewHolder.musicName_tv = (TextView)convertView.findViewById(R.id.item_music_name);
            convertView.setTag(viewHolder);

        }else{
            viewHolder =(ViewHolder)convertView.getTag();
        }
        if(musicLists.get(position)!=null){

            viewHolder.musicName_tv.setText(musicLists.get(position).getName());
        }
        return convertView;
    }
    public class ViewHolder{
        LinearLayout itemALl_rl;
        TextView musicName_tv;
    }

    public void setMusicLists(ArrayList<MusicInfoModel> musicLists) {
        this.musicLists = musicLists;
    }
    public void refrensh(){
        super.notifyDataSetChanged();
        LogUtils.setLog("the music list data is "+ JsonUtil.getInstance().serializeObject(musicLists));
    }
}
