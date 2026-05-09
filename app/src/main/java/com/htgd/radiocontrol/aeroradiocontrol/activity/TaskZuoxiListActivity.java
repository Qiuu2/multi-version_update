package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;

import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.zhy.adapter.recyclerview.CommonAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wzq on 2017-08-14.
 */
public class TaskZuoxiListActivity extends BaseActivity implements View.OnClickListener {
    private Context mContexts;
    private ArrayList<TaskGuangboModel> taskDataList = new ArrayList<>();//作息任务数据
    private String mTag = "TaskZuoxiListActivity";
    private ViewHolder viewHolder = new ViewHolder();
    private TaskGuangboModel taskZuoxiModel;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private LRecyclerViewAdapter LAdapter;
    private CommonAdapter<TaskGuangboModel> mAdapter;
    private ArrayList<TaskGuangboModel> lists;
    private int currentPosition = -1;
    private TextView change,delete,detail;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_task;
    }

    @Override
    protected void initSubViews() {
        mContexts = this;
        viewHolder.title_tv = (TextView) findViewById(R.id.title_text);
        viewHolder.title_tv.setText("作息任务表");
        viewHolder.title_left = (Button) findViewById(R.id.title_go_back);
        viewHolder.title_left.setVisibility(View.VISIBLE);
        viewHolder.title_left.setOnClickListener(this);
        viewHolder.title_right = (Button) findViewById(R.id.title_button);
        viewHolder.title_right.setVisibility(View.VISIBLE);
        viewHolder.title_right.setOnClickListener(this);
        initModel();
        initListView();
        initTwoButton();
    }

    private void initTwoButton() {
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

    private void initListView() {
        list = new MyLRecycView(getApplicationContext(), R.color.transparent);
        content = (LinearLayout) findViewById(R.id.content);
        empty = (LinearLayout) findViewById(R.id.empty);
        list.setLayoutManager(new MyLinearLayoutManager(mContexts));
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
                    getDataFormSever();//首页
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
                    getDataFormSever();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private void renderView() {
        mAdapter = new CommonAdapter<TaskGuangboModel>(mContexts, R.layout.item_task_zuoxi_list, taskDataList) {
            public boolean same;

            @Override
            protected void convert(final com.zhy.adapter.recyclerview.base.ViewHolder holder, final TaskGuangboModel model, final int position) {
                if(model!=null) {
                    holder.setText(R.id.name, model.getName());
                    holder.setText(R.id.task_start_time, model.getStartdate());
                    holder.setText(R.id.task_end_time, model.getEnddate());
                    holder.setText(R.id.starttime_inhour, model.getStarttime());
                    holder.setText(R.id.timelength_type, TaskMainMethod.setWeekDay(model.getExecmode()));
                    holder.setOnClickListener(R.id.task_part, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            currentPosition = position - 1;
                            notifyDataSetChanged();
                        }
                    });
                    holder.setText(R.id.task_music, model.getMedianame());
                    if (currentPosition == position - 1) {
                        LogUtils.setLog(mTag, currentPosition + "是选中" + position);
                        holder.setBackgroundRes(R.id.task_part, R.drawable.et_back_sel);
                    } else {
                        holder.setBackgroundRes(R.id.task_part, R.drawable.et_back);
                    }
                    holder.setOnClickListener(R.id.task_part, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            currentPosition = position - 1;
                            notifyDataSetChanged();
                        }
                    });
                }else{
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.all:
                if (currentPosition != -1) {
                    changeTask();
                } else {
                    showToast("请先选择任务");
                }
                currentPosition = -1;
                break;
            case R.id.start:
                if (currentPosition != -1) {
                    deleteTask();
                } else {
                    showToast("请先选择任务");
                }
                currentPosition = -1;
                break;
            case R.id.cancle:
                if (currentPosition != -1) {
                    LogUtils.setLog("the model is no empty");
                    Intent intent = new Intent();
                    intent.setClass(mContexts,TaskZuoxiDetailActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Constant.BUNDLE_KEY_MODEL,taskZuoxiModel);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    showToast("请先选择任务");
                }
                currentPosition = -1;
                break;
            case R.id.title_go_back:
                finish();
                break;
            case R.id.title_button:
                if (taskDataList.size() > 0) {
                    Intent intent = new Intent(mContexts, AddSchemeTaskActivity.class);
                    Bundle bundle = new Bundle();
                    taskDataList.get(0).setSechename(taskZuoxiModel.getSechename());
                    LogUtils.setLog(mTag, "方案任务列表处点添加" + taskZuoxiModel.getSechename());
                    bundle.putSerializable(CacheConstants.add_scheme_task_from_list, taskDataList.get(0));
                    bundle.putSerializable(CacheConstants.add_scheme_task_from_list_size, taskDataList.size());
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    showToast(getResources().getString(R.string.no_scheme));
                }
                break;
        }
    }

    private void deleteTask() {
        try {
            TaskMainMethod TaskMainMethod = new TaskMainMethod(mContexts);
            TaskMainMethod.deleteTask(Integer.parseInt(taskDataList.get(currentPosition).getTaskid()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        taskDataList.remove(currentPosition);
        updateUI();
    }

    private void changeTask() {
        Intent intent = new Intent(mContexts, AddSchemeTaskActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(CacheConstants.change_schemetask, taskDataList.get(currentPosition));
        LogUtils.setLog(mTag, "点击前" + taskDataList.get(currentPosition).getTaskname());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private class ViewHolder {
        Button title_left, title_right;
        TextView title_tv;

    }

    private void getDataFormSever() throws IOException {
        if (taskZuoxiModel != null && taskZuoxiModel.getSechename() != null) {
            MyRequestBuilder request = new MyRequestBuilder(mContexts);
            request.setNeedToken(true);
            request.setUrl(Constant.postSchemeInfo);
            request.setBodyMap(new HashMap<String, String>() {
                {
                    LogUtils.setLog(mTag, "方案名称" + taskZuoxiModel.getTaskname() + "-name-" + taskZuoxiModel.getName());
                    put("name", taskZuoxiModel.getSechename());
                }
            });
            RequestManger.getInstance().postHashMap(request, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    LogUtils.setLog(mTag, "code is" + code + "response is:" + response);
                    TaskGuangboListRsp reponseData = JsonUtil.getInstance().deSerializeString(response, TaskGuangboListRsp.class);
                    if (reponseData != null && reponseData.getData() != null && reponseData.getData().size() > 0) {
                        lists = reponseData.getData();
                        LogUtils.setLog(mTag, "任务数量" + lists.size() + lists.get(0).getName());
                        taskDataList.clear();
                        if (lists.get(0).getName() != null) {
                            for (int i = 0; i < lists.size(); i++) {
                                if (lists.get(i).getTaskstate() == 1) {
                                    taskDataList.add(lists.get(i));
                                }
                            }

                            for (int i = 0; i < lists.size(); i++) {
                                if (lists.get(i).getTaskstate() != 1) {
                                    taskDataList.add(lists.get(i));
                                }
                            }

                            ((Activity) mContexts).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    content.removeViewAt(0);
                                    content.addView(list, 0);
                                    list.setAdapter(LAdapter);
                                    updateUI();
                                }
                            });

                        } else {
                            ((Activity) mContexts).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    content.removeViewAt(0);
                                    content.addView(list, 0);
                                    list.refreshComplete(taskDataList.size());//停止刷新
                                    list.setEmptyView(empty);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailed(int code, String message) {
                    reTry();
                    showToast("请求失败，正在重试");
                }
            });
        } else {
            LogUtils.setLog(mTag, "" + "model is empty");
        }
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(getApplicationContext(), new BaseActivity.updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getDataFormSever();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed() {

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            LogUtils.setLog(mTag, "qingqius");
            getDataFormSever();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //初始化模型
    private void initModel() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        taskZuoxiModel = (TaskGuangboModel) bundle.getSerializable(CacheConstants.NETWORK_MODEL);
        LogUtils.setLog(mTag, "方案名称" + taskZuoxiModel.getSechename() + "-name-"/*+taskZuoxiModel.getName()*/);
    }


    private void updateUI() {
        ((Activity) mContexts).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(taskDataList.size());
            }
        });
    }


}
