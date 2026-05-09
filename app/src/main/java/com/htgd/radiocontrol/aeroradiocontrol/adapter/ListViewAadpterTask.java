package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by zongwei on 2017-07-18.
 *  任务列表 适配器
 */
public class ListViewAadpterTask extends BaseAdapter {

    private Context mContext;
    private Viewholder viewholder = new Viewholder();
    private int clickTemp = -1;//标识被选择的item
    private int itemLength =0;
    private int[] clickedList;//这个数组用来存放item的点击状态
    private int seletedPostion;

    private ArrayList<MachineInfo> machienList;
    public ListViewAadpterTask(Context context,ArrayList<MachineInfo> machienList){
        this.mContext  =  context;
        this.machienList = machienList;
        if(machienList!=null){
            itemLength = machienList.size();
            clickedList=new int[itemLength];
            LogUtils.setLog("the itemLengeth is" + itemLength);
        }else{
            clickedList=new int[0];
        }
        for (int i =0;i<itemLength;i++){
            clickedList[i]=0;      //初始化item点击状态的数组
        }
    }
    @Override
    public int getCount() {
        if(machienList!=null && machienList.size()>0 && 0!=machienList.get(0).getAll()){
            return  machienList.size();
        }else{
            return  0;
        }
    }

    public void updateListViewData(ArrayList<MachineInfo> list) {
        this.machienList = list;
        notifyDataSetChanged();
    }
    public void setSeclection(int posiTion) {
        clickTemp = posiTion;
    }

    @Override
    public MachineInfo getItem(int position) {

        return machienList==null?null:machienList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null&& mContext !=null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_ter_machine,null);
            initView(convertView);
            convertView.setTag(viewholder);
            LogUtils.setLog("the machienList size is" + machienList.size());
        }else{
            viewholder =(Viewholder)convertView.getTag();
            LogUtils.setLog("the conviewView is not null or the context is null");
        }
        if(machienList.get(position) != null){
            viewholder.itemText1.setText(machienList.get(position).getName());
            viewholder.itemText2.setText(machienList.get(position).getIp());
        }
        if(clickTemp==position){    //根据点击的Item当前状态设置背景
            if (clickedList[position]==0){
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.colorYellw));
                clickedList[position]=1;
            }
            else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
                clickedList[position]=0;
            }
        }
        return convertView;
    }

    private void initView(View view){
        viewholder.imgaeBig =(ImageView)view.findViewById(R.id.item_big_image);
        viewholder.item1 = (LinearLayout)view.findViewById(R.id.item_1);
        viewholder.item2 = (LinearLayout)view.findViewById(R.id.item_2);
        viewholder.item3 = (LinearLayout)view.findViewById(R.id.item_3);
        viewholder.item4 = (LinearLayout)view.findViewById(R.id.item_4);
        viewholder.itemImage1 = (ImageView)view.findViewById(R.id.item_little_image1);
        viewholder.itemImage2 = (ImageView)view.findViewById(R.id.item_little_image2);
        viewholder.itemImage3 = (ImageView)view.findViewById(R.id.item_little_image3);
        viewholder.itemImage4 = (ImageView)view.findViewById(R.id.item_little_image4);
        viewholder.itemText1 = (TextView)view.findViewById(R.id.item_little_text1);
        viewholder.itemText2 = (TextView)view.findViewById(R.id.item_little_text2);
        viewholder.itemText3 = (TextView)view.findViewById(R.id.item_little_text3);
        viewholder.itemText4 = (TextView)view.findViewById(R.id.item_little_text4);
        viewholder.item_all = (RelativeLayout)view.findViewById(R.id.item_all);
    }

    private class Viewholder{
        private ImageView itemImage1,itemImage2,itemImage3,itemImage4;
        private ImageView imgaeBig;
        private TextView itemText1,itemText2,itemText3,itemText4;
        private LinearLayout item1,item2,item3,item4;
        private RelativeLayout item_all;
    }

    public void setSelectedPosition(int position) {
        seletedPostion = position;
    }

    public ArrayList<MachineInfo> getMachienList() {
        return machienList;
    }

    public void setMachienList(ArrayList<MachineInfo> machienList) {
        this.machienList = machienList;
    }
}
