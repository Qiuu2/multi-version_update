package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;

import java.util.ArrayList;


/**
 * 作者：wzq
 * 时间：2019/2/23:14:13
 * 邮箱：535708929
 * 说明：可展开显示子节点的列表
 */
public class MyExpandableAdapter extends BaseExpandableListAdapter {


   ArrayList<String> groupString =  new ArrayList<String>(); 
   ArrayList<ArrayList<String>> childString =  new ArrayList<ArrayList<String>>(); 

    public MyExpandableAdapter(Context mcontext, ArrayList<String> groupString, ArrayList<ArrayList<String>> childString) {

        this.groupString = groupString;
        this.childString = childString;
    }

    public MyExpandableAdapter(Context mcontext) {

        for (int i = 0; i <5; i++) {
            groupString.add("sheshi"+i);
            childString.add(new ArrayList<String>());
            for (int j = 0; j < 3; j++) {
                childString.get(i).add(groupString.get(i)+"haha"+j);
            }
        }

    }

    @Override
    // 获取分组的个数
    public int getGroupCount() {
        return groupString.size();
    }

    //获取指定分组中的子选项的个数
    @Override
    public int getChildrenCount(int groupPosition) {
        return childString.get(groupPosition).size();
    }

    //        获取指定的分组数据
    @Override
    public Object getGroup(int groupPosition) {
        return groupString.get(groupPosition);
    }

    //获取指定分组中的指定子选项数据
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childString.get(groupPosition).get(childPosition);
    }

    //获取指定分组的ID, 这个ID必须是唯一的
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    //获取子选项的ID, 这个ID必须是唯一的
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    //分组和子选项是否持有稳定的ID, 就是说底层数据的改变会不会影响到它们
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * 获取显示指定组的视图对象
     *
     * @param groupPosition 组位置
     * @param isExpanded    该组是展开状态还是伸缩状态
     * @param convertView   重用已有的视图对象
     * @param parent        返回的视图对象始终依附于的视图组
     */
// 获取显示指定分组的视图
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder groupViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.partent_item, parent, false);
            groupViewHolder = new GroupViewHolder();
            groupViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.label_group_normal);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }
        groupViewHolder.tvTitle.setText(groupString.get(groupPosition));
        return convertView;
    }

    /**
     * 获取一个视图对象，显示指定组中的指定子元素数据。
     *
     * @param groupPosition 组位置
     * @param childPosition 子元素位置
     * @param isLastChild   子元素是否处于组中的最后一个
     * @param convertView   重用已有的视图(View)对象
     * @param parent        返回的视图(View)对象始终依附于的视图组
     * @return
     * @see android.widget.ExpandableListAdapter#getChildView(int, int, boolean, View,
     * ViewGroup)
     */

    //取得显示给定分组给定子位置的数据用的视图
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder childViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_item, parent, false);
            childViewHolder = new ChildViewHolder();
            childViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.expand_child);
            convertView.setTag(childViewHolder);

        } else {
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }
        childViewHolder.tvTitle.setText(childString.get(groupPosition).get(childPosition));
        return convertView;
    }

    //指定位置上的子元素是否可选中
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public static class GroupViewHolder {
        TextView tvTitle;
    }

    public static class ChildViewHolder {
        TextView tvTitle;

    }
}


