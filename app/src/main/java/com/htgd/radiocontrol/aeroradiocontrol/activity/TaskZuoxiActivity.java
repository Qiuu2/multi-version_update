package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ErrorCode;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskZuoxiModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskZuoxiRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ArryListUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.TipDialog;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wzq on 2017-08-03.
 */
public class TaskZuoxiActivity extends BaseActivity implements View.OnClickListener {


    private Context mContext;
    private String mTag = "TaskZuoxiActivity";

    private ArrayList<TaskGuangboModel> taskList = new ArrayList<>();
    private Button title_left;
    private TextView title_tv;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private LRecyclerViewAdapter LAdapter;
    private CommonAdapter<TaskGuangboModel> mAdapter;
    private TaskManageUtils taskManageUtils;
    private Button title_right;
    private ArrayList<TaskGuangboModel> allList=new ArrayList<>();

    private TextView change,delete,detail;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_task;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        taskManageUtils = new TaskManageUtils(mContext);

        initTitle();
        initListView();
         initTwoButton();
    }
    private void initTitle() {
        title_tv = (TextView) findViewById(R.id.title_text);
        title_tv.setText("作息方案");
        title_left = (Button) findViewById(R.id.title_go_back);
        title_left.setVisibility(View.VISIBLE);
        title_left.setOnClickListener(this);
        title_right = (Button) findViewById(R.id.title_button);
        title_right.setVisibility(View.VISIBLE);
        title_right.setOnClickListener(this);
    }
    private void initTwoButton() {
        change=(TextView)findViewById(R.id.all);
        change.setVisibility(View.GONE);
        delete=(TextView)findViewById(R.id.start);
        delete.setVisibility(View.GONE);
        detail=(TextView)findViewById(R.id.cancle);
        detail.setVisibility(View.GONE);
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
                    getTaskListFromSever();//首页
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
                    getTaskListFromSever();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void renderView() {
        mAdapter = new CommonAdapter<TaskGuangboModel>(mContext, R.layout.item_scheme_task, taskList) {
            @Override
            protected void convert(final ViewHolder holder, final TaskGuangboModel model, final int position) {
                if(model!=null) {

                    holder.setText(R.id.sche_name, model.getSechename());
                    holder.setText(R.id.task_start_time, model.getStartdate());
                    holder.setText(R.id.task_end_time, model.getEnddate());
                    holder.setText(R.id.task_order, model.getTaskcount() + "");
                    holder.setOnClickListener(R.id.delete, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new TipDialog(mContext, "是否删除该方案", new TipDialog.OnViewClickListener() {
                                @Override
                                public void onConfirmClick(View v) {
                                    TaskMainMethod taskMethod = new TaskMainMethod(mContext);
                                    for (int i = 0; i < allList.size(); i++) {
                                        if (allList.get(i).getSechename().equals(taskList.get(position - 1).getSechename())) {
                                            try {
                                                taskMethod.deleteTask(Integer.parseInt(allList.get(i).getTaskid()));
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    taskList.remove(position - 1);
                                    updateUI();
                                }

                                @Override
                                public void onCancelClick(View v) {

                                }
                            }).show();

                        }
                    });

                    holder.setOnClickListener(R.id.scheme_part, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TaskGuangboModel taskZuoxiModel = taskList.get(position - 1);
                            LogUtils.setLog(mTag, "单击了作息方案 " + taskZuoxiModel.getSechename() + ",getVolume:" + taskZuoxiModel.getVolume() + "priority:" + taskZuoxiModel.getPriority());

                            if (taskZuoxiModel != null) {
                                Intent intent = new Intent();
                                intent.setClass(mContext, TaskZuoxiListActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(CacheConstants.NETWORK_MODEL, taskZuoxiModel);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        }
                    });
                    LogUtils.setLog(mTag, "初始化数据列表时projectstate" + model.getProjectstate());
                    switch (taskList.get(position - 1).getProjectstate()) {
                        case 0:
                            holder.setChecked(R.id.on_or_off, true);
                            holder.setText(R.id.task_state, getResources().getString(R.string.on));
                            break;
                        case 1:
                            holder.setChecked(R.id.on_or_off, false);
                            holder.setText(R.id.task_state, getResources().getString(R.string.off));
                            break;
                    }
                    holder.setOnClickListener(R.id.on_or_off, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            LogUtils.setLog(mTag, "dianjil " + model.getProjectstate());
                            switch (taskList.get(position - 1).getProjectstate()) {
                                case 0:

                                    try {
                                        startOrStopTask(model, 1, holder);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 1:
                                    try {
                                        startOrStopTask(model, 0, holder);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }

                    });
                }else {
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };
    }

    // 启用或停止任务
    private synchronized void startOrStopTask(final TaskGuangboModel model, final int state, final ViewHolder holder) throws IOException {

        taskManageUtils.startOrStopProject(model.getSechename(), state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(mTag,"更改状态成功"+state);

                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        model.setProjectstate(state);
                       if(state==0) {
                           holder.setChecked(R.id.on_or_off, true);
                           holder.setText(R.id.task_state, getResources().getString(R.string.on));
                       } else {
                           holder.setChecked(R.id.on_or_off, false);
                           holder.setText(R.id.task_state, getResources().getString(R.string.off));
                       }
                        LogUtils.setLog(mTag,"第一个方案启停"+taskList.get(0).getProjectstate());
                    }
                });

            }

            @Override
            public void onTheSameStatu() {
            }

            @Override
            public void onRetry() {
            }
        });
    }





    @Override
    public void onResume() {
        super.onResume();
        try {
            getTaskListFromSever();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getTaskListFromSever() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getsecheList, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskGuangboListRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskGuangboListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    LogUtils.setLog(mTag,"作息方案数量"+responseData.getData().size()+responseData.getData().get(0).getSechename());
                    ArrayList<TaskGuangboModel>  lists = responseData.getData();

                    allList.clear();
                    for (int i = 0; i < responseData.getData().size(); i++) {
                        allList.add(responseData.getData().get(i));
                    }
                    ArrayList<TaskGuangboModel> sameStrings = new ArrayList<>();
                    if (lists.get(0).getSechename() != null) {
                        for (int i = 0; i < lists.size(); i++) {//选出重复的内容
                            for (int j = 0; j < lists.size(); j++) {
                                if (lists.get(i).getSechename().equals(lists.get(j).getSechename()) && i != j) {
                                    if (sameStrings.size() == 0) {
                                        sameStrings.add(lists.get(i));
                                    } else {
                                        boolean same = true;
                                        for (int k = 0; k < sameStrings.size(); k++) {
                                            if (!lists.get(i).getSechename().equals(sameStrings.get(k).getSechename())) {
                                                same = false;
                                            } else {
                                                same = true;
                                                break;
                                            }
                                        }
                                        if (same == false) {
                                            sameStrings.add(lists.get(i));
                                        }
                                    }
                                }
                            }
                        }

                        if (sameStrings.size() > 0) {
                            HashMap<Integer, TaskGuangboModel> map = new HashMap<>();
                            int length = lists.size();
                            for (int i = 0; i < length; i++) {
                                map.put(i, lists.get(i));
                            }

                            for (int i = 0; i < length; i++) {//去除重复的内容
                                for (int j = 0; j < sameStrings.size(); j++) {
                                    if (lists.get(i).getSechename().equals(sameStrings.get(j).getSechename())) {
                                        map.remove(i);

                                    }
                                }
                            }
                            lists.clear();
                            for (Integer in : map.keySet()) {

                                TaskGuangboModel str = map.get(in);//得到每个key多对用value的值  
                                lists.add(str);
                            }
                            lists.addAll(sameStrings);
                        }
                        LogUtils.setLog(mTag,"最后服务器获取的数量"+responseData.getData().size());
                        sameStrings.clear();
                        taskList.clear();
                        for (int i = 0; i < lists.size(); i++) {
                            taskList.add(responseData.getData().get(i));
                        }
                        LogUtils.setLog(mTag,"最后方案数量"+taskList.size());
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                content.removeViewAt(0);
                                content.addView(list, 0);
                                list.setAdapter(LAdapter);
                                updateUI();
                            }
                        });

                    } else {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju" + taskList.size());
                                content.removeViewAt(0);
                                content.addView(list, 0);
                                list.refreshComplete(taskList.size());//停止刷新
                                list.setEmptyView(empty);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {


            }
        });
    }



    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(taskList.size());//停止刷新
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.title_go_back:
                finish();
                break;
            case R.id.title_button:
                Intent intent1 = new Intent(this, AddSchemeActivity.class);
                startActivity(intent1);
                break;
        }
    }





    
}
