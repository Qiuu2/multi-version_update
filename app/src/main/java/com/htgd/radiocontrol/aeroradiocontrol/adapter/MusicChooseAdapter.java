package com.htgd.radiocontrol.aeroradiocontrol.adapter;




/**
 * Created by wzq on 2017/10/18.
 */

public class MusicChooseAdapter/* extends BaseExpandableListAdapter*/ {
    /*private   ArrayList<MusicFolderInfoModel> gData;
    private Context context;
    private ArrayList<ArrayList<MusicInfoModel>> iData;
    public MusicChooseAdapter(Context context , ArrayList<ArrayList<MusicInfoModel>> musicInfoModels , ArrayList<MusicFolderInfoModel> gData){
        this.context = context;
        this.iData = musicInfoModels;
        this.gData=gData;

    }
    @Override
    public int getGroupCount() {
        return gData.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return iData.get(groupPosition).size();
    }

    @Override
    public MusicFolderInfoModel getGroup(int groupPosition) {
        return gData.get(groupPosition);
    }

    @Override
    public MusicInfoModel getChild(int groupPosition, int childPosition) {
        return iData.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    //取得用于显示给定分组的视图. 这个方法仅返回分组的视图对象
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        ViewHolderGroup groupHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.item_exlist_group, parent, false);
            groupHolder = new ViewHolderGroup();
            groupHolder.tv_group_name = (TextView) convertView.findViewById(R.id.tv_group_name);
            convertView.setTag(groupHolder);
        }else{
            groupHolder = (ViewHolderGroup) convertView.getTag();
        }
        groupHolder.tv_group_name.setText(gData.get(groupPosition).getName());
        return convertView;
    }

    //取得显示给定分组给定子位置的数据用的视图
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolderItem itemHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music_choose,null);
            itemHolder=new ViewHolderItem();
            itemHolder.itemALl_rl = (RelativeLayout)convertView.findViewById(R.id.item_all);
            itemHolder.musicName_tv = (TextView)convertView.findViewById(R.id.item_music_name);
            itemHolder.misic_box =(CheckBox)convertView.findViewById(R.id.music_choose_box);
            convertView.setTag(itemHolder);
        }else{
            itemHolder = (ViewHolderItem) convertView.getTag();
        }
        if(iData.get(groupPosition).get(childPosition)!=null){
            itemHolder.musicName_tv.setText(iData.get(groupPosition).get(childPosition).getName());
            if(iData.get(groupPosition).get(childPosition).isChoose()){
                itemHolder.misic_box.setChecked(true);
            }else{
                itemHolder.misic_box.setChecked(false);
            }
        }


        itemHolder.musicName_tv.setText(iData.get(groupPosition).get(childPosition).getName());
        return convertView;
    }

    //设置子列表是否可选中
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    private static class ViewHolderGroup{
        private TextView tv_group_name;
    }

    private static class ViewHolderItem{
        RelativeLayout itemALl_rl;
        TextView musicName_tv,music_style;
        CheckBox misic_box;
    }
   *//* public View getView(int position, View convertView, ViewGroup parent) {
     ViewHolder viewHolder;
        final int index = position;
        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music_choose,null);
            viewHolder.itemALl_rl = (RelativeLayout)convertView.findViewById(R.id.item_all);
            viewHolder.musicName_tv = (TextView)convertView.findViewById(R.id.item_music_name);
            viewHolder.misic_box =(CheckBox)convertView.findViewById(R.id.music_choose_box);
            convertView.setTag(viewHolder);

        }else{
            viewHolder =(ViewHolder)convertView.getTag();
        }
        viewHolder.misic_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    musicLists.get(index).setChoose(true);
                }else{
                    musicLists.get(index).setChoose(false);
                }
            }
        });
        if(musicLists.get(position)!=null){
            viewHolder.musicName_tv.setText(musicLists.get(position).getName());
            if(musicLists.get(position).isChoose()){
                viewHolder.misic_box.setChecked(true);
            }else{
                viewHolder.misic_box.setChecked(false);
            }
        }
        return convertView;
    }*/


}
