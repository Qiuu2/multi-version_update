package com.htgd.radiocontrol.aeroradiocontrol.adapter;

/**
 * 作者：wzq
 * 时间：2021/1/25:15:18
 * 邮箱：535708929
 * 说明：选择终端
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;



public class CheckBoxMachineAdapter extends BaseAdapter {
    private  ArrayList<MachineInfo> zoneMachineList;
    private  ArrayList<MachineInfo> chooseMachineList;
    private HashMap<Integer, Boolean> isSelected;//key为int型类似arraylist
    private Context mContext;
    private String mTag = "CheckBoxMachineAdapter";
    public void setListener(AddNumListener listener) {
        LogUtils.setLog(mTag,"回调选择监听");
        this.listener = listener;
    }

    private AddNumListener listener;


  public CheckBoxMachineAdapter(Context context, HashMap maps, ArrayList<MachineInfo> zoneMachineList, ArrayList<MachineInfo> chooseMachineList ) {
      this.mContext = context;
      this.isSelected = maps;
      this.chooseMachineList=   chooseMachineList;
      this.zoneMachineList=zoneMachineList;
    }

    @Override
    public int getCount() {
        return zoneMachineList.size();
    }
    @Override
    public Object getItem(int i) {
        return zoneMachineList.get(i);
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



        if(isSelected!=null&&isSelected.size()>0) {
            if (isSelected.get(i)) {
                viewHolder.name.setChecked(true);
            } else {
                viewHolder.name.setChecked(false);
            }

            // 监听checkBox并根据原来的状态来设置新的状态
            final ViewHolder finalViewHolder = viewHolder;
            viewHolder.name.setText(zoneMachineList.get(i).getName());
            if(zoneMachineList.get(i).getNetstate()==1){
                viewHolder.timesize.setText("在线");
            }else{
                viewHolder.timesize.setText("离线");
            }

            finalViewHolder.cBox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LogUtils.setLog(mTag, isSelected.size() + "mapsize" + i + isSelected.get(i));
                    if (isSelected.get(i)) {
                        isSelected.put(i, false);

                        finalViewHolder.name.setChecked(false);
                        for (int j = 0; j < chooseMachineList.size(); j++) {
                            if(chooseMachineList.get(j).getName().equals(zoneMachineList.get(i).getName())){
                                LogUtils.setLog(mTag,chooseMachineList.get(j).getName()+"删选了");
                                chooseMachineList.remove(j);
                                listener.addNum();
                            }
                        }
                    } else {
                        isSelected.put(i, true);
                        finalViewHolder.name.setChecked(true);
                        chooseMachineList.add(zoneMachineList.get(i));
                        LogUtils.setLog(mTag, zoneMachineList.get(i) .getName()+"增选了");
                        listener.addNum();
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

    public void setList(ArrayList<MachineInfo> list) {
        this.zoneMachineList=list;
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
