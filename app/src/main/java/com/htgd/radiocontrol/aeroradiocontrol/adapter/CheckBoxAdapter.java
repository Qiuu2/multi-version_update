package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 作者：wzq
 * 时间：2018/8/18:11:59
 * 邮箱：535708929
 * 说明：带复选框的列表适配器
 */
public class CheckBoxAdapter extends BaseAdapter {
    private  ArrayList<MusicInfoModel> musicList;
    private  ArrayList<MusicInfoModel> chooseProgList;
    private HashMap<Integer, Boolean> isSelected;//key为int型类似arraylist
    private Context mContext;
    private String mTag = "CheckBoxAdapter";



    public void setListener(AddNumListener listener) {
        LogUtils.setLog(mTag,"回调选择监听");
        this.listener = listener;
    }

    private AddNumListener listener;


    public CheckBoxAdapter(Context context, HashMap maps, ArrayList<MusicInfoModel> musicList, ArrayList<MusicInfoModel> choosePogList) {
        this.mContext = context;
        this.isSelected = maps;
        this.chooseProgList=choosePogList;

        this.musicList=musicList;
        LogUtils.setLog(mTag,choosePogList.toString()+musicList.size());
    }
    @Override
    public int getCount() {
        return musicList.size();
    }
    @Override
    public Object getItem(int i) {
        return musicList.get(i);
    }
    @Override
    public long getItemId(int i) {
        return i;
    }
    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.music_item, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (CheckBox) view.findViewById(R.id.s_name);
            viewHolder.timesize = (TextView) view.findViewById(R.id.time_size);
            viewHolder.timesize.setVisibility(View.GONE);
            viewHolder.cBox = (RelativeLayout) view.findViewById(R.id.c_box);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.name.setText(musicList.get(i).getName());
         if(isSelected!=null&&isSelected.size()>0) {
             if (isSelected.get(i)) {
                viewHolder.name.setChecked(true);
            } else {
                viewHolder.name.setChecked(false);
            }

        // 监听checkBox并根据原来的状态来设置新的状态
        final ViewHolder finalViewHolder = viewHolder;
            finalViewHolder.cBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LogUtils.setLog(mTag, isSelected.size() + "mapsize" + i + isSelected.get(i));
                if (isSelected.get(i)) {
                    isSelected.put(i, false);

                    finalViewHolder.name.setChecked(false);
                    for (int j = 0; j < chooseProgList.size(); j++) {
                        if(chooseProgList.get(j).getName().equals(musicList.get(i).getName())){
                            LogUtils.setLog(mTag,chooseProgList.get(j).getName()+"删选了");
                            chooseProgList.remove(j);
                           // listener.addNum();
                        }
                    }
                } else {
                    isSelected.put(i, true);
                    finalViewHolder.name.setChecked(true);
                    chooseProgList.add(musicList.get(i));
                    LogUtils.setLog(mTag, musicList.get(i) .getName()+"增选了");
                    //listener.addNum();
                }

            }
        });
  }
      /*  // 根据isSelected来设置checkbox的选中状况
        if (isSelected != null && isSelected.get(i) != null) {
            viewHolder.name.setChecked(isSelected.get(i));
        }*/
        return view;
    }
    public void setIsSelected(HashMap<Integer, Boolean> s) {
        this.isSelected = s;
    }

    public void setList(ArrayList<MusicInfoModel> list) {
        this.musicList=list;
    }

    private class ViewHolder {
        private CheckBox name;
        private TextView timesize;
        private RelativeLayout cBox;

    }
    public interface AddNumListener {
        public void addNum();
    }
}
