package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.CheckBoxAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MyExpandableAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.RecyclerMusicAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.component.SwipableRecycleView.DefaultItemTouchHelpCallback;
import com.htgd.radiocontrol.aeroradiocontrol.component.SwipableRecycleView.DefaultItemTouchHelper;
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
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 作者：wzq
 * 时间：2019/1/24:15:32
 * 邮箱：535708929
 * 说明：选音乐    获取音乐文件分组获取音乐
 */
public class SelectMusicActivity extends BaseActivity/*extends BaseActivity implements View.OnClickListener*/ {
    @Override
    protected int getLayoutId() {
        return 0;
    }

    @Override
    protected void initSubViews() {

    }

    /*private ArrayList<MusicInfoModel> musicInfoList = new ArrayList<>();
    private Context mContext;
    private String mTag = "SelectMusicActivity";
    private ArrayList<MusicFolderInfoModel> folderList;
    private ArrayList<MusicFolderInfoModel> allFolderList;
    private ArrayList<MusicFolderInfoModel> mediaFolderList = new ArrayList<>();
    private ListView mediaList;
    private ArrayList<String> groupString = new ArrayList<String>();
    private ArrayList<ArrayList<String>> childString = new ArrayList<ArrayList<String>>();
    private MyExpandableAdapter myExpandableAdapter;
    private HashMap<Integer, String> groupmaps = new HashMap<>();
    private HashMap<String, Integer> groupmap = new HashMap<>();
    private HashMap<Integer, Boolean> pmaps = new HashMap<>();
    private CheckBoxAdapter checkBoxAdapter;
    private TextView start, all, has, cancle, selected_num;
    private ArrayList<MusicInfoModel> chooseProgList = new ArrayList<MusicInfoModel>();
    private ArrayList<MachineInfo> choosemachine;
    private PopupWindow popWindow;
    private TextView mediaRepertory;//点播媒体库
    private TextView terminalNum;
    private RecyclerMusicAdapter recyclerAdapter;
    private DefaultItemTouchHelper.PackTouchHelper itemTouchHelper;
    private ImageView noMedia;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_music_select;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        choosemachine = (ArrayList<MachineInfo>) getIntent().getExtras().get("choosemachine");
        getTitleUi();


        initlistview();
        initFourButton();

    }



    private void getTitleUi() {
        TextView title = (TextView) findViewById(R.id.title_text);
        TextView back = (TextView) findViewById(R.id.title_go_back);
        title.setText(getResources().getString(R.string.play));
        back.setOnClickListener(this);
    }

    private void initlistview() {

        try {
            getMusicFromServer(3);
        } catch (IOException e) {
            e.printStackTrace();
        }


        checkBoxAdapter = new CheckBoxAdapter(mContext, pmaps, musicInfoList, chooseProgList);
        mediaList.setAdapter(checkBoxAdapter);

        checkBoxAdapter.setListener(new CheckBoxAdapter.AddNumListener() {
            @Override
            public void addNum() {
                LogUtils.setLog(mTag, "监听触发" + chooseProgList.size());
                selected_num.setText("" + chooseProgList.size());
            }
        });

    }

    public void getfolderlist() {
        for (int i = 0; i < folderList.size(); i++) {
            groupmaps.put(folderList.get(i).getFolderid(), folderList.get(i).getName());
            groupmap.put(folderList.get(i).getName(), folderList.get(i).getFolderid());
            if (folderList.get(i).getParentid() == 3) {
                groupString.add(folderList.get(i).getName());
                childString.add(new ArrayList<String>());

            }
        }
        for (int i = 0; i < folderList.size(); i++) {
            for (int j = 0; j < groupString.size(); j++) {
                LogUtils.setLog(mTag, "ziwenjian" + folderList.size() + groupString.get(j));
                if (groupString.get(j) == groupmaps.get(folderList.get(i).getParentid())) {
                    childString.get(j).add(folderList.get(i).getName());
                    LogUtils.setLog(mTag, "ziwenjian" + folderList.get(i).getName());
                }
            }
        }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myExpandableAdapter.notifyDataSetChanged();
            }
        });
    }

    //获取文件
    private void getFolderFromServer() throws IOException {
        RequestManger.getInsatcne().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getFolderInfo, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicFolderInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicFolderInfosRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    folderList = responseData.getData();
                    allFolderList = new ArrayList<MusicFolderInfoModel>();
                    for (int i = 0; i < folderList.size(); i++) {
                        if (folderList.get(i).getParentid() == 3) {
                            allFolderList.add(folderList.get(i));
                            mediaFolderList.add(folderList.get(i));
                        }
                    }
                    getfolderlist();
                    LogUtils.setLog(mTag, "媒体文件夹数量" + allFolderList.size());
                    try {
                        getMusicFromServer(3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.setLog(mTag, "the date from server is error");
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "response failed");

            }
        });
    }

    private void getMusicFromServer(int folderid) throws IOException {

        RequestManger.getInsatcne().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getMusicInfo + folderid, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                musicInfoList.clear();
                for (int i = 0; i < responseData.getData().size(); i++) {
                    musicInfoList.add(responseData.getData().get(i));
                }
                pmaps.clear();
                if (responseData.getData().get(0).getAll() != 0 && responseData.getData().get(0).getName() != null) {
                    LogUtils.setLog(mTag, "diyishouge" + responseData.getData().get(0).getName());
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mediaList.setVisibility(View.VISIBLE);
                            noMedia.setVisibility(View.GONE);
                        }
                    });
                    for (int i = 0; i < musicInfoList.size(); i++) {
                        pmaps.put(i, false);//获取
                        for (int j = 0; j < chooseProgList.size(); j++) {
                            if (chooseProgList.get(j).getName().equals(musicInfoList.get(i).getName())) {//复原已选音乐
                                pmaps.put(i, true);
                                LogUtils.setLog(mTag, musicInfoList.get(i).getName());
                            }
                        }
                    }
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
                LogUtils.setLog(mTag,"刷新了已选界面");
                selected_num.setText(chooseProgList.size() + "");
                checkBoxAdapter.setList(musicInfoList);
                checkBoxAdapter.setIsSelected(pmaps);
                checkBoxAdapter.notifyDataSetChanged();
            }
        });
    }

    //初始化四个按钮
    private void initFourButton() {
        start = (TextView) findViewById(R.id.start);
        start.setOnClickListener(this);
        all = (TextView) findViewById(R.id.all);
        all.setOnClickListener(this);
        cancle = (TextView) findViewById(R.id.cancle);
        cancle.setOnClickListener(this);
        has = (TextView) findViewById(R.id.has);
        has.setOnClickListener(this);
        selected_num = (TextView) findViewById(R.id.select_num);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_media_repertory:
                try {
                    getMusicFromServer(3);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.start:
                LogUtils.setLog(mTag, "选择的音乐" + chooseProgList.size());
                if (chooseProgList.size() > 0) {
                    MainMethod.startplay(choosemachine, chooseProgList);

                }
                for (int i = 0; i < choosemachine.size(); i++) {
                    Cons.chooseMachine.add(choosemachine.get(i).getName());
                }
                for (int i = 0; i < chooseProgList.size(); i++) {
                    Cons.chooseProgList.add(chooseProgList.get(i).getName());
                }
                finish();
                break;
            case R.id.all:

                selectAll();
                break;
            case R.id.cancle:
                selectNull();
                break;
            case R.id.has:
              MusicDialog musicDialog = new MusicDialog(mContext, musicInfoList, chooseProgList,pmaps, new MusicDialog.UpdataListener() {
                  @Override
                  public void updataUI() {
                    LogUtils.setLog(mTag,"刷新了已选界面"+"lis");
                      updateUI();
                  }
              });
              musicDialog.show();

                break;
            case R.id.title_go_back:
                Cons.chooseMachine.clear();
                finish();
                break;
        }
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

    private void selectAll() {
        for (Integer key : pmaps.keySet()) {
            if (pmaps.get(key) == false) {
                chooseProgList.add(musicInfoList.get(key));
            }
            pmaps.put(key, true);
        }
        updateUI();
    }

    //显示已选的popwindow
    private void showPopwind() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.music_dialog, null);
        ListView mRecyclerViews = (ListView) view.findViewById(R.id.media_list);
        mRecyclerViews.setVisibility(View.GONE);
        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.medias_list);
        final ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < chooseProgList.size(); i++) {
            strings.add(chooseProgList.get(i).getName());
        }
        LogUtils.setLog(mTag, "yixuanshuliang" + strings.size());
        mRecyclerView.setLayoutManager(new MyLinearLayoutManager(mContext ));
        recyclerAdapter = new RecyclerMusicAdapter(mContext, chooseProgList);
        mRecyclerView.setAdapter(recyclerAdapter);
        itemTouchHelper = new DefaultItemTouchHelper.PackTouchHelper(listeners);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        itemTouchHelper.setDragEnable(true);
        itemTouchHelper.setSwipeEnable(true);
        recyclerAdapter.setListener(new RecyclerMusicAdapter.DeleteListener() {
            @Override
            public void delete(int i) {
                for (int j = 0; j < chooseProgList.size(); j++) {
                    if (chooseProgList.get(j).getName().equals(strings.get(i))) {
                        chooseProgList.remove(j);
                        break;
                    }
                }
                for (int j = 0; j < musicInfoList.size(); j++) {
                    if (musicInfoList.get(j).getName().equals(strings.get(i))) {
                        pmaps.put(j, false);
                    }
                }
                strings.remove(i);
                recyclerAdapter.notifyDataSetChanged();
                updateUI();
            }
        });
        popWindow = new PopupWindow(view, 700, 600, true);
        popWindow.setBackgroundDrawable(new BitmapDrawable());//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(true);//设置是否点击PopupWindow外退出PopupWindow
        popWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    // 拖动/滑动 监听器 更新数据
    private final DefaultItemTouchHelpCallback.OnItemTouchCallbackListener listeners = new DefaultItemTouchHelpCallback.OnItemTouchCallbackListener() {
        @Override
        public boolean onMove(int srcPosition, int targetPosition) {
            Collections.swap(chooseProgList, srcPosition, targetPosition);
            recyclerAdapter.notifyItemMoved(srcPosition, targetPosition);
            for (int i = 0; i < chooseProgList.size(); i++) {
                LogUtils.setLog(mTag, chooseProgList.get(i).getName());
            }
            recyclerAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onSwiped(int adapterPosition) {

        }
    };*/
}
