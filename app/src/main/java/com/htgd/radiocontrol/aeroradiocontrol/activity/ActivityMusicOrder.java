package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MusicChooseAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.MusicTreeViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.MachineListModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.Node;
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
import java.util.List;



/**
 * Created by wzq on 2017-08-10.
 */
public class ActivityMusicOrder extends BaseActivity implements View.OnClickListener {
    private Context mContext;
    private final String mTag = "ActivityMusicOrder";
    private ViewHolder viewHolder = new ViewHolder();
    private ArrayList<MusicInfoModel> musicInfoList;
    private ArrayList<ArrayList<MusicInfoModel>> iData = new ArrayList<>();
    private MusicChooseAdapter musicListAdapter;
    private MachineListModel machineInfo;

    private ArrayList<MusicFolderInfoModel> folderList;
    private int c = 0;
    private ArrayList<MusicInfoModel> mDatas = new ArrayList<MusicInfoModel>();
    private MusicTreeViewAdapter<MusicInfoModel> mAdapter;
    private List<Node> musicCheckedList;
    private ArrayList<MusicFolderInfoModel> fList;
    private String Tag = "ActivityMusicOrder";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_music_list;
    }

    @Override
    protected void initSubViews() {

        initView();
        try {
            getFolderFromServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_go_back:
                htIntf.stopondemand();
                finish();
                break;
            case R.id.top_button_next:
                orderMusic();
                break;
            case R.id.top_button_cannel:
                cleanTheChoose();
                viewHolder.topTwoButton.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    private class ViewHolder {
        private Button title_left;
        private ImageView center_image;
        private TextView title_tv, center_text;
        private ListView music_lv;
        private View topTwoButton;
        private Button cannel_bt, next_bt;
        private ProgressBar wait;
    }
     //获取音乐
    private void getMusicFromServer(int folderid) throws IOException {

        RequestManger.getInstance().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getMusicInfo + folderid, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicInfosRsp.class);
                LogUtils.setLog("fanhui", c + "daxiao" + responseData.getData().size() + "");
                //    if (responseData != null && responseData.getData() != null && responseData.getData().size() >= 0) {
                musicInfoList = responseData.getData();
                if (responseData.getData().get(0).getAll() != 0) {
                    for (int i = 0; i < musicInfoList.size(); i++) {
                        MusicInfoModel mModel = new MusicInfoModel(musicInfoList.get(i).getMediaid(), musicInfoList.get(i).getFolderid(), musicInfoList.get(i).getName(), "false");
                        mDatas.add(mModel);
                        //musicDao.addDate(mDatas.get(i));
                    }
                }
                LogUtils.setLog(Tag, "媒体文件数量" + mDatas.size());
                if (c < fList.size()) {
                    try {
                        LogUtils.setLog(mTag, "dijici" + c);
                        getMusicFromServer(fList.get(c).getFolderid());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    c++;
                } else {
                  //  PreferencesUtil.getInstance().keepField(Constant.hasMusic, "true", mContext);
                    updateUI();
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag, "response failed");

            }
        });
    }
  //获取文件
    private void getFolderFromServer() throws IOException {
        RequestManger.getInstance().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getFolderInfo, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MusicFolderInfosRsp responseData = JsonUtil.getInstance().deSerializeString(response, MusicFolderInfosRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    folderList = responseData.getData();
                    fList = new ArrayList<MusicFolderInfoModel>();
                    mDatas.add(new MusicInfoModel(3, 0, "点播媒体库", "true"));
                    for (int i = 0; i < folderList.size(); i++) {
                        if (folderList.get(i).getParentid() == 3) {
                            mDatas.add(new MusicInfoModel(folderList.get(i).getFolderid(), folderList.get(i).getParentid(), folderList.get(i).getName(), "true"));
                            fList.add(folderList.get(i));
                        }
                    }
                    LogUtils.setLog(Tag, "媒体文件夹数量" + fList.size());
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
                if (ErrorCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }


    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewHolder.wait.setVisibility(View.GONE);
                try {
                    mAdapter = new MusicTreeViewAdapter<MusicInfoModel>(mDatas, ActivityMusicOrder.this, viewHolder.music_lv, 3);
                    viewHolder.music_lv.setAdapter(mAdapter);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                mAdapter.setOnTreeNodeCheckBoxClickListener(new MusicTreeViewAdapter.OnTreeNodeCheckBoxClickListener() {
                    @Override
                    public void onCheckChange(Node node, int position, List<Node> checkedNodes) {
                        musicCheckedList = checkedNodes;
                        LogUtils.setLog("xzl" + musicCheckedList.size() + "");
                        viewHolder.topTwoButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

      public  void onStart(){
           super.onStart();
           try {
               getFolderFromServer();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getFolderFromServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed() {
                    showToast(R.string.get_token_failed);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //发起音乐点播
    private void orderMusic() {
        LogUtils.setLog(mTag, "start to orderMusic");
        for (int i = 0; i < musicInfoList.size(); i++) {
            Cons.chooseProgList.add(musicInfoList.get(i) );
        }
        htIntf.newondemandlist();
        if (machineInfo != null && machineInfo.getMachineInfos().size() > 0) {
            ArrayList<MachineInfo> list = machineInfo.getMachineInfos();
            for (MachineInfo info : list) {
                htIntf.setondemandterminal(info.getId());
            }
        } else {
            LogUtils.setLog(mTag, "the list is  null");
        }
        if (musicCheckedList != null && musicCheckedList.size() > 0) {
            for (Node node : musicCheckedList) {
                if (node.ischecked()) {
                    LogUtils.setLog(mTag, "the music id " + node.getId() + " is add");
                    htIntf.setondemandmedia(node.getId());

                }
            }
        }

        int state = htIntf.startondemand();

    }

    // 取消所有选中状态
    private void cleanTheChoose() {
        LogUtils.setLog("xuanzhong", musicCheckedList.size() + "");
        if (musicCheckedList != null && musicCheckedList.size() > 0) {
            for (int i = 0; i < musicCheckedList.size(); i++) {
                musicCheckedList.get(i).setIschecked(false);
            }
        }
    }

    private void initView() {
        Bundle bundle = this.getIntent().getExtras();
        machineInfo = (MachineListModel) bundle.getSerializable(Constant.BUNDLE_KEY_MACHINE_ID_LIST);
        LogUtils.setLog(mTag, "the get bundle machineInfo is " + JsonUtil.getInstance().serializeObject(machineInfo));
        mContext = this;
        viewHolder.title_tv = (TextView) findViewById(R.id.title_text);
        viewHolder.title_tv.setText("点播音乐");
        viewHolder.title_left = (Button) findViewById(R.id.title_go_back);
        viewHolder.title_left.setVisibility(View.VISIBLE);
        viewHolder.title_left.setOnClickListener(this);
        viewHolder.music_lv = (ListView) findViewById(R.id.listview_music);
        viewHolder.topTwoButton = findViewById(R.id.top_next_button);
        viewHolder.cannel_bt = (Button) findViewById(R.id.top_button_cannel);
        viewHolder.cannel_bt.setOnClickListener(this);
        viewHolder.next_bt = (Button) findViewById(R.id.top_button_next);
        viewHolder.next_bt.setOnClickListener(this);
        viewHolder.topTwoButton.setVisibility(View.GONE);
        viewHolder.wait = (ProgressBar) findViewById(R.id.wait);
        viewHolder.wait.setVisibility(View.VISIBLE);
        viewHolder.music_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                viewHolder.topTwoButton.setVisibility(View.VISIBLE);
                if (musicInfoList.get(position).isChoose()) {
                    musicInfoList.get(position).setChoose(false);
                    LogUtils.setLog(mTag, "the music " + musicInfoList.get(position).getMediaid() + "is choose");
                } else {
                    musicInfoList.get(position).setChoose(true);
                    LogUtils.setLog(mTag, "the music " + musicInfoList.get(position).getMediaid() + "delete choose");
                }

            }
        });


    }
}
