package com.htgd.radiocontrol.aeroradiocontrol.widget.dialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.CheckBoxAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MyExpandableAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicFolderInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicFolderInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * 作者：wzq
 * 时间：2021/3/2:16:33
 * 邮箱：535708929
 * 说明：选音乐
 */
public class MusicPop implements View.OnClickListener {
    private String currentActivity;
    private ArrayList<MachineInfo> chooseMachineList;
    private View view;
    private ArrayList<MusicInfoModel> musicInfoList = new ArrayList<>();//音乐列表
    private Context mContext;
    private ImageView noMedia;
    private String mTag = "MusicPop";
    private CheckBoxAdapter checkBoxAdapter;
    private ListView mediaList;//音乐列表视图
    private HashMap<Integer, Boolean> pmaps = new HashMap<>();
    private ArrayList<MusicInfoModel> chooseProgList;
    private PopupWindow popWindow;
    private Consumer<String> consumer;

    public MusicPop(Context context, String ss, ArrayList<MusicInfoModel> chooseProgList, ArrayList<MachineInfo> chooseMachineList, Consumer s) {
        this.mContext = context;
        this.chooseProgList = chooseProgList;
        this.chooseMachineList = chooseMachineList;
        this.consumer = s;
        showPopWindow();
        currentActivity = ss;
        initView();
        initlistview();
        initFourButton(view);
    }

    public MusicPop(Context context, String ss, ArrayList<MusicInfoModel> chooseProgList, ArrayList<MachineInfo> chooseMachineList) {
        this.mContext = context;
        this.chooseProgList = chooseProgList;
        this.chooseMachineList = chooseMachineList;

        showPopWindow();
        currentActivity = ss;
        initView();
        initlistview();
        initFourButton(view);
    }

    private void showPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        view = inflater.inflate(R.layout.music_order, null);
        popWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        popWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        popWindow.setAnimationStyle(R.style.mypopwindow_anim_style);

    }

    //初始化四个按钮
    private void initFourButton(View view) {
        TextView start = (TextView) view.findViewById(R.id.start);
        start.setOnClickListener(this);
        start.setText(mContext.getResources().getString(R.string.confirm));
        TextView all = (TextView) view.findViewById(R.id.all);
        all.setOnClickListener(this);
        TextView cancle = (TextView) view.findViewById(R.id.cancle);
        cancle.setText(mContext.getResources().getString(R.string.selectnull));
        cancle.setOnClickListener(this);


    }

    private void initView() {

        noMedia = (ImageView) view.findViewById(R.id.no_media);
    }


    private void initlistview() {

        try {
            getMusicFromServer(3);
        } catch (IOException e) {
            e.printStackTrace();
        }


        mediaList = (ListView) view.findViewById(R.id.media_list);
        checkBoxAdapter = new CheckBoxAdapter(mContext, pmaps, musicInfoList, chooseProgList);
        mediaList.setAdapter(checkBoxAdapter);


    }

    private void getMusicFromServer(int folderid) throws IOException {
        LogUtils.setLog(mTag, "地址的内存地址" + Constant.serveraddress);
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMusicInfo + folderid, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                musicInfoList.clear();
                pmaps.clear();
                for (int i = 0; i < responseData.getData().size(); i++) {
                    musicInfoList.add(responseData.getData().get(i));
                }

                if (responseData.getData().get(0).getAll() != 0 && responseData.getData().get(0).getName() != null) {
                    LogUtils.setLog(mTag, "diyishouge" + responseData.getData().get(0).getName());
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mediaList.setVisibility(View.VISIBLE);
                            noMedia.setVisibility(View.GONE);
                        }
                    });
                    setMap();

                } else {

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mediaList.setVisibility(View.GONE);
                            noMedia.setVisibility(View.VISIBLE);
                        }
                    });
                    //无歌曲
                }
                updateUI();
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "response failed");
            }
        });
    }

    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtils.setLog(mTag, "刷新了已选界面");

                checkBoxAdapter.setList(musicInfoList);
                checkBoxAdapter.setIsSelected(pmaps);
                checkBoxAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setMap() {//
        for (int i = 0; i < musicInfoList.size(); i++) {
            pmaps.put(i, false);//获取
            LogUtils.setLog(mTag, musicInfoList.size() + "yixuan" + musicInfoList.get(i).getName());

            for (int j = 0; j < chooseProgList.size(); j++) {
                LogUtils.setLog(mTag, chooseProgList.size() + "yixuan--liebiao" + chooseProgList.get(j).getName());
                if (chooseProgList.get(j).getName().equals(musicInfoList.get(i).getName())) {//复原已选音乐
                    pmaps.put(i, true);
                    LogUtils.setLog(mTag, musicInfoList.get(i).getName());
                }
            }
        }
    }

    private void selectAll() {
        for (Integer key : pmaps.keySet()) {
            if (pmaps.get(key) == false) {
                chooseProgList.add(musicInfoList.get(key));
            }
            pmaps.put(key, true);
        }
        updateUI();
    }

    private void selectNull() {
        for (Integer key : pmaps.keySet()) {
            LogUtils.setLog(mTag, musicInfoList.get(key).getName() + pmaps.get(key));
            if (pmaps.get(key) == true) {
                for (int i = 0; i < chooseProgList.size(); i++) {
                    if (chooseProgList.get(i).getName().equals(musicInfoList.get(key).getName())) {
                        chooseProgList.remove(i);
                    }
                }
            }
            pmaps.put(key, false);
        }
        updateUI();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.start:
                LogUtils.setLog(mTag, "已选的音乐" + chooseProgList.size());
                if (consumer != null) {
                    LogUtils.setLog(mTag, "consumer" + "null");
                    if (chooseProgList.size() > 0 && !currentActivity.contains("Add")) {
                        LogUtils.setLog(mTag, "已选的音乐" + "startplay");
                        MainMethod.startplay(chooseMachineList, chooseProgList);
                    }else{
                        LogUtils.setLog(mTag, "dangqianwei  " + "add");
                    }
                    for (int i = 0; i < chooseMachineList.size(); i++) {
                        Cons.chooseMachine.add(chooseMachineList.get(i));
                    }
                    for (int i = 0; i < chooseProgList.size(); i++) {
                        Cons.chooseProgList.add(chooseProgList.get(i));
                    }
                } else {
                    LogUtils.setLog(mTag, "consumer" + " not null");
                    Observable.just(chooseProgList.size() + "").subscribe(consumer);
                }
                popWindow.dismiss();
                break;
            case R.id.all:
                selectAll();
                break;
            case R.id.cancle:
                selectNull();
                break;

        }

    }
}
