package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;


import com.htgd.radiocontrol.aeroradiocontrol.model.Node;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TreeHelper;

import java.util.List;

/**
 * Created by Administrator on 2016/8/14.
 */
public abstract class BaseTreeViewAdapter<T> extends BaseAdapter {


    private final boolean[] checks;
    protected List<Node> mAllNodeList;
    protected List<Node> mVisibleNodeList;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected ListView mTreeView;


    /**
     * 设置点击回调接口
     */
    public interface OnTreeNodeClickListener {
        void onTreeNodeClick(Node node, int position);

    }

    private OnTreeNodeClickListener mListener;

    public void setOnTreeNodeClickListener(OnTreeNodeClickListener l) {
        mListener = l;
    }
    public BaseTreeViewAdapter(List<T> datas, Context context, ListView tree, int defaultExpandLevel ) throws IllegalAccessException {
         checks = new boolean[datas.size()];
        mAllNodeList = TreeHelper.sortedNode(datas, defaultExpandLevel);
        mVisibleNodeList = TreeHelper.filterVisibleNode(mAllNodeList);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mTreeView = tree;
        if (mTreeView != null) {
            mTreeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    expandOrCollapse(position);
                    if (mListener != null) {
                        mListener.onTreeNodeClick(mVisibleNodeList.get(position), position);
                        LogUtils.setLog("shauishaui");
                    }
                }
            });
        }
        BaseTreeViewAdapter.this.notifyDataSetChanged();

    }

    /**
     * 点击收缩或展开
     * @param position
     */
    private void expandOrCollapse(int position) {
        Node node = mVisibleNodeList.get(position);
        if (node != null) {
            //叶子节点，返回
            if (node.isLeaf()) {
                return;
            }
            //根据当前状态决定是展开还是收缩
            node.setExpand(!node.isExpand());
            //集合中的node的可见性发生变化，重新过滤得到可见node的集合
            mVisibleNodeList = TreeHelper.filterVisibleNode(mAllNodeList);
            //通知adapter数据发生改变
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mVisibleNodeList.size();
    }

    @Override
    public Object getItem(int position) {
        return mVisibleNodeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(  int position, View convertView, ViewGroup parent) {

        final Node node = mVisibleNodeList.get(position);
        convertView = getConvertView(node, position, convertView, parent,checks);
        //设置左边距，视觉上形成层级关系
        convertView.setPadding(node.getLevel() * 30, 3, 3, 3);
        RelativeLayout myView = (RelativeLayout) convertView;
        //父布局下的CheckBox
       /* CheckBox cb = (CheckBox) myView.getChildAt(1);
        final int pos  = position; //pos必须声明为final
        cb.setOnCheckedChangeListener(new  CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                TreeHelper.setNodeChecked(node, isChecked);
                checks[pos] = isChecked;
                List<Node> checkedNodes = new ArrayList<Node>();
                for(Node n:mAllNodeList){
                    if(n.ischecked()&&n.isLeaf()){
                        checkedNodes.add(n);
                    }
                }
                mListener.onCheckChange(node,pos,checkedNodes);

            }

        });*/
        return convertView;
    }

    public abstract View getConvertView(Node node, int position, View convertView, ViewGroup parent,boolean[] checks);
}
