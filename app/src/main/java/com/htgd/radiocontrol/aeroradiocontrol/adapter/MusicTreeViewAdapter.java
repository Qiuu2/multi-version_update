package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.Node;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2016/8/14.
 */
public class MusicTreeViewAdapter<T> extends  BaseTreeViewAdapter<T> {


    private OnTreeNodeCheckBoxClickListener mListener;

    public interface OnTreeNodeCheckBoxClickListener {
        void onCheckChange(Node node, int position, List<Node> checkedNodes);
    }

    public MusicTreeViewAdapter(List<T> datas, Context context, ListView tree, int defaultExpandLevel ) throws IllegalAccessException {
        super(datas, context, tree, defaultExpandLevel );


    }
    public void setOnTreeNodeCheckBoxClickListener(OnTreeNodeCheckBoxClickListener l) {
        mListener = l;
    }
    @Override
    public View getConvertView(final Node node, int position, View convertView, ViewGroup parent, boolean[] checks) {
      final  ViewHolder holder;
        if (convertView == null) {
           //  convertView = mInflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
           //  holder.icon = (ImageView) convertView.findViewById(R.id.iv_icon_list_item);
           //  holder.name = (TextView) convertView.findViewById(R.id.tv_name_list_item);
            holder.checkBox=(CheckBox)convertView.findViewById(R.id.checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final int pos  = position; //pos必须声明为final
        if (node.getIcon() == -1) {

            holder.icon.setVisibility(View.INVISIBLE);
        } else {
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageResource(node.getIcon());
        }

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            List<Node> checkedNodes = new ArrayList<Node>();
            @Override
            public void onClick(View v) {
                setNodeChecked(node,holder.checkBox.isChecked());
                for(Node n:mAllNodeList){
                    if(n.ischecked()&&n.isLeaf()){
                        checkedNodes.add(n);
                    }
                }
                mListener.onCheckChange(node,pos,checkedNodes);
            }
        });
        holder.checkBox.setOnCheckedChangeListener(new  CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });




        if (node.ischecked()){
            holder.checkBox.setChecked(true);
        }else {
            holder.checkBox.setChecked(false);
        }
           // holder.checkBox.setChecked(checks[pos]);

        if( node.isfile()=="true"){
            holder.checkBox.setVisibility(View.GONE);
        }else{
            holder.checkBox.setVisibility(View.VISIBLE);
        }
        holder.name.setText(node.getName());
        return convertView;
    }

    private void setNodeChecked(Node node, boolean checked) {
        node.setIschecked(checked);
    }

    class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkBox;
    }

    /**
     * 动态添加节点
     * @param name  要添加的节点的名称
     * @param position  被添加的位置
     */
   /* public void addNode( int position, String name) {
        Node node = mVisibleNodeList.get(position);
        //关键是找出原本的位置
        int indexOf = mAllNodeList.indexOf(node);
        Node newNode = new Node(-1, node.getId(), name);
        newNode.setParent(node);
        node.getChildrens().add(newNode);
        mAllNodeList.add(indexOf + 1, newNode);
        mVisibleNodeList = TreeHelper.filterVisibleNode(mAllNodeList);
        notifyDataSetChanged();
    }*/

}
