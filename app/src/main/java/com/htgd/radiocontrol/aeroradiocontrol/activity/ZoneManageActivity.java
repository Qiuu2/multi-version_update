package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.ZoneMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;

public class ZoneManageActivity extends BaseActivity implements View.OnClickListener {
    private ZoneManageActivity mContext;
    private Button title_left;
    private TextView title_tv;
    private Button title_right;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private LRecyclerViewAdapter LAdapter;
    private ArrayList<ZoneModel> zoneList = new ArrayList<>();
    private CommonAdapter<ZoneModel> mAdapter;
    private String mTag = "ZoneManageActivity";
    private int currentPosition = -1;
    private TextView change;
    private TextView delete;
    private TextView detail;
    private ZoneMethod zoneMethod;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_task;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        zoneMethod = new ZoneMethod(mContext);
        initTitle();
        initListView();
        initThreeButton();
    }

    private void initThreeButton() {
        change = (TextView) findViewById(R.id.all);
        change.setBackgroundResource(R.drawable.change_style);
        change.setOnClickListener(this);
        delete = (TextView) findViewById(R.id.start);
        delete.setOnClickListener(this);
        delete.setText("");
        delete.setBackgroundResource(R.drawable.delete_style);
        detail = (TextView) findViewById(R.id.cancle);
        detail.setOnClickListener(this);
        detail.setText("");
        detail.setBackgroundResource(R.drawable.detail_style);

    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtils.setLog(mTag, "activity onstop返回数据" + zoneList.size());
        Intent intent = new Intent();
        setResult(RESULT_OK, intent.putExtra(CacheConstants.ACTIVITY_RESULT, zoneList.size() + ""));
        String result = intent.getExtras().getString(CacheConstants.ACTIVITY_RESULT);
        LogUtils.setLog(mTag, "activity onstop返回数据" + result);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.setLog(mTag, "activity onpause返回数据" + zoneList.size());
        Intent intent = new Intent();
        setResult(RESULT_OK, intent.putExtra(CacheConstants.ACTIVITY_RESULT, zoneList.size() + ""));
        String result = intent.getExtras().getString(CacheConstants.ACTIVITY_RESULT);
        LogUtils.setLog(mTag, "activity onpause返回数据" + result);
    }


    @Override
    public void onResume() {
        super.onResume();
        try {
            getZone();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initTitle() {
        title_left = (Button) findViewById(R.id.title_go_back);
        title_left.setVisibility(View.VISIBLE);
        title_left.setOnClickListener(this);
        title_tv = (TextView) findViewById(R.id.title_text);
        title_tv.setText(getResources().getString(R.string.zonelist));
        title_right = (Button) findViewById(R.id.title_button);
        title_right.setVisibility(View.VISIBLE);
        title_right.setOnClickListener(this);

    }

    private void initListView() {
        list = new MyLRecycView(getApplicationContext(), R.color.transparent);
        content = (LinearLayout) findViewById(R.id.content);
        empty = (LinearLayout) findViewById(R.id.empty);
        list.setLayoutManager(new MyLinearLayoutManager(mContext));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, 0);
        list.setLayoutParams(lp);
        renderView();
        LAdapter = new LRecyclerViewAdapter(mAdapter);
        list.setAdapter(LAdapter);
        list.setEmptyView(empty);
        list.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    getZone();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //网络错误的时候调用
        list.setOnNetWorkErrorListener(new OnNetWorkErrorListener() {
            @Override
            public void reload() {
                try {
                    getZone();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    //初始化时获取分区数组
    private synchronized void getZone() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.SearchZone, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                ZoneRsp responseData = JsonUtil.getInstance().deSerializeString(response, ZoneRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    zoneList.clear();
                    if (responseData.getData().get(0).getName() != null) {
                        for (int i = 0; i < responseData.getData().size(); i++) {
                            zoneList.add(responseData.getData().get(i));
                        }
                    }
                    zoneList.add(0, new ZoneModel(zoneList.size() + "", 0, 0, 0, 0, "无", "全部", "全部终端"));
                    LogUtils.setLog(mTag, "zoneList sizes" + zoneList.size() + zoneList.get(0).getId());
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            content.removeViewAt(0);
                            content.addView(list, 0);
                            list.setAdapter(LAdapter);
                            updateUI();
                        }
                    });
                }
            }

            @Override
            public void onFailed(int code, String message) {
                showToast(ChinaConstants.zonegetfail);
            }
        });
    }

    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(zoneList.size());//停止刷新
            }
        });
    }

    private void renderView() {
        mAdapter = new CommonAdapter<ZoneModel>(mContext, R.layout.item_zone, zoneList) {

            @Override
            protected void convert(ViewHolder holder, ZoneModel zoneModel, final int position) {
                holder.setText(R.id.name, zoneModel.getName());

                holder.setText(R.id.build_time, zoneModel.getDatetime());
                if (zoneModel.getDescription() != null) {
                    holder.setText(R.id.descraption, zoneModel.getDescription());
                }
                holder.setOnClickListener(R.id.zone_part, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentPosition = position - 1;
                        notifyDataSetChanged();
                    }
                });
                LogUtils.setLog(mTag, "列表" + position);
                if (currentPosition == position - 1) {
                    LogUtils.setLog(mTag, currentPosition + "是选中" + position);
                    holder.setBackgroundRes(R.id.zone_part, R.drawable.et_back_sel);
                } else {
                    holder.setBackgroundRes(R.id.zone_part, R.drawable.et_back);
                }


            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_button:
                startActivity(new Intent(this, AddZoneActivity.class));
                break;
            case R.id.title_go_back:
                finish();
                break;
            case R.id.all:
                if (currentPosition != -1) {
                    changeZone();
                } else {
                    showToast(getResources().getString(R.string.please_select_zone) );
                }
                currentPosition = -1;
                break;
            case R.id.start:
                if (currentPosition != -1) {
                     deleteZone();
                } else {
                    showToast(getResources().getString(R.string.please_select_zone) );
                }
                zoneList.remove(currentPosition);
                updateUI();
                currentPosition = -1;
                break;
            case R.id.cancle:
                if (currentPosition != -1) {
                    Intent intent = new Intent(mContext,  ZoneDetailActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(CacheConstants.ZONE_MODEL, zoneList.get(currentPosition));
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    showToast(getResources().getString(R.string.please_select_zone));
                }
                currentPosition = -1;
                break;
        }
    }

    private void deleteZone() {
        try {
            zoneMethod.deleteZone(zoneList.get(currentPosition).getId(),false,zoneList.get(currentPosition),new ArrayList<MachineInfo>());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeZone() {
        Intent intent= new Intent(this,AddZoneActivity.class);
        Bundle bundle = new Bundle();
        if (currentPosition != -1) {
            bundle.putSerializable(CacheConstants.ZONE_MODEL, zoneList.get(currentPosition));
             intent.putExtras(bundle);
            startActivity(intent);
        }

    }
}
