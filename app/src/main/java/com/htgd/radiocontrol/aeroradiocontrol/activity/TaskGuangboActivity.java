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
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.TaskMainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfosRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by zongwei on 2017-08-04.
 */
public class TaskGuangboActivity extends BaseActivity implements View.OnClickListener {
    /* private Context mContext;
     private final String Tag = "TaskGuangboActivity";
     private GridView taskGuangboList;
     private ArrayList<TaskGuangboModel> taskList = new ArrayList<>();
     private TaskGuangboAdapter adapter;
     private Button title_left;
     private TextView title_tv;
     private int flag; // 2.文件广播， 3.采播管理，5.终端功放 ,17 文字语音


     @Override
     protected int getLayoutId() {
         mContext = this;
         return R.layout.activity_tasks_info;
     }

     @Override
     protected void initSubViews() {
         initFlag();

         taskGuangboList = (GridView) findViewById(R.id.task_zuoxi_list);
         adapter = new TaskGuangboAdapter(mContext, taskList, flag);
         title_tv = (TextView) findViewById(R.id.title_text);
         switch (flag) {
             case Constant.keyGuangbo:
                 title_tv.setText("广播任务");
                 break;
             case Constant.keyCaibo:
                 title_tv.setText("采播管理");
                 break;
             case Constant.keyGongfang:
                 title_tv.setText("终端功放");
                 break;
             case Constant.keyWenZiYuYin:
                 title_tv.setText("文字语音");
                 break;
         }
         try {

             getTaskListFromSever();
         } catch (IOException e) {
             e.printStackTrace();
         }
         title_left = (Button) findViewById(R.id.title_go_back);
         title_left.setVisibility(View.VISIBLE);
         title_left.setOnClickListener(this);
         taskGuangboList.setAdapter(adapter);
         taskGuangboList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
             @Override
             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                 TaskGuangboModel taskGuangboModel = taskList.get(position);
                 if (taskGuangboModel != null) {
                     startTaskDetailActvity(taskGuangboModel, flag);
                 }
             }
         });

     }

     private synchronized void getTaskListFromSever() throws IOException {
         taskList.clear();
         RequestManger.getInstance().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getTaskList + "/" + flag, new onRequestLister() {
             @Override
             public void onSucess(int code, String response) {
                 TaskGuangboListRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskGuangboListRsp.class);
                 if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                     LogUtils.setLog(Tag, "the request is sucess");
                       taskList = responseData.getData();
                     adapter.setTasklist(taskList);
                     updateUI();
                 } else {
                     showToast("数据请求失败");
                 }
             }

             @Override
             public void onFailed(int code, String message) {
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
                 taskGuangboList.setAdapter(adapter);
                 adapter.notifyDataSetChanged();
             }
         });
     }

     @Override
     public void onClick(View v) {
         switch (v.getId()) {
             case R.id.title_go_back:
                 finish();
                 break;

         }
     }

     // token 失效时重新验证，验证成功后重新请求服务器
     private void reTry() {
         try {
             updateToken(mContext, new updatelister() {
                 @Override
                 public void onSucess() {
                     try {
                         getTaskListFromSever();
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

     public void initFlag() {
         Intent intent = getIntent();
         Bundle bundle = intent.getExtras();
         flag = bundle.getInt(Constant.BUNDLE_KEY_TYPE);
     }

     @Override
     public void onResume() {
         super.onResume();
         LogUtils.setLog(Tag, "onResume is start");
         try {
             getTaskListFromSever();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }

     private void startTaskDetailActvity(TaskGuangboModel taskGuangboModel, int flag) {
         Intent intent = new Intent();
         Bundle bundle = new Bundle();
         bundle.putInt(Constant.BUNDLE_KEY_TYPE, flag);
         bundle.putSerializable(Constant.BUNDLE_KEY_MODEL, taskGuangboModel);
         intent.putExtras(bundle);
         intent.setClass(mContext,  TaskGuangboDetailActivity.class);
         startActivity(intent);

     }*/

    private Context mContext;
    private final String mTag = "TaskGuangboActivity";
    private ArrayList<TaskGuangboModel> taskList = new ArrayList<>();
    private ArrayList<TaskGuangboModel> serveletTaskList = new ArrayList<>();
    private ArrayList<TaskGuangboModel> manualist = new ArrayList<>();
    private Button title_left;
    private TextView title_tv;
    private int flag; // 2.文件广播， 3.采播管理，5.终端功放 ,17 文字语音

    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private CommonAdapter<TaskGuangboModel> mAdapter;
    private LRecyclerViewAdapter LAdapter;
    private TaskManageUtils taskManageUtils;
    private Button title_right;
    private TextView change,delete,detail;
    private int currentPosition = -1;

    @Override
    protected int getLayoutId() {
        mContext = this;
        return R.layout.activity_task;
    }

    @Override
    protected void initSubViews() {
        initFlag();

        initTitle();

        switch (flag) {
            case IntConstans.keyGuangbo:
                title_tv.setText("广播任务");
                break;
            case IntConstans.keyCaibo:
                title_tv.setText("采播管理");
                break;
            case IntConstans.keyGongfang:
                title_tv.setText("终端功放");
                break;
            case IntConstans.keyWenZiYuYin:
                title_tv.setText("文字语音");
                break;
        }
        initListView();
        change=(TextView)findViewById(R.id.all);
        change.setBackgroundResource(R.drawable.change_style);
        change.setOnClickListener(this);
        delete=(TextView)findViewById(R.id.start);
        delete.setOnClickListener(this);
        delete.setText("");
        delete.setBackgroundResource(R.drawable.delete_style);
        detail=(TextView)findViewById(R.id.cancle);
        detail.setOnClickListener(this);
        detail.setText("");
        detail.setBackgroundResource(R.drawable.detail_style);
        taskManageUtils = new TaskManageUtils(mContext);
    }

    private void initTitle() {
        title_left = (Button) findViewById(R.id.title_go_back);
        title_left.setVisibility(View.VISIBLE);
        title_left.setOnClickListener(this);
        title_tv = (TextView) findViewById(R.id.title_text);
        title_right = (Button) findViewById(R.id.title_button);
        title_right.setVisibility(View.VISIBLE);
        title_right.setOnClickListener(this);
    }

    private void initListView() {
        list = new MyLRecycView(getApplicationContext(), R.color.transparent);
        content = (LinearLayout) findViewById(R.id.content);
        empty = (LinearLayout) findViewById(R.id.empty);
        list.setLayoutManager(new MyLinearLayoutManager(mContext , LinearLayoutManager.VERTICAL,false));
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
        mAdapter = new CommonAdapter<TaskGuangboModel>(mContext, R.layout.item_zuoxi_task, taskList) {
            @Override
            protected void convert(final ViewHolder holder, final TaskGuangboModel model, final int position) {
                if (model != null) {
                    holder.setText(R.id.name, model.getTaskname());
                    holder.setText(R.id.task_start_time, model.getStartdate());
                    holder.setText(R.id.task_end_time, model.getEnddate());
                    holder.setText(R.id.timelength_type, TaskMainMethod.setWeekDay(model.getExecmode()));
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
                    if (flag == IntConstans.keyGuangbo) {//时长
                        holder.setText(R.id.time_length, TaskMainMethod.setTimeLength(Integer.parseInt(model.getTimelength()), Integer.parseInt(model.getTimelengthtype())));
                    } else if (flag == IntConstans.keyWenZiYuYin) {
                        holder.setText(R.id.time_length, TaskMainMethod.setTimeLength(Integer.parseInt(model.getTimelength()), 2));
                    } else {
                        holder.setText(R.id.time_length, TaskMainMethod.setTimeLength(Integer.parseInt(model.getTimelength()), 1));
                    }
                    if (flag == IntConstans.keyGuangbo || flag == IntConstans.keyWenZiYuYin) {//设置启停
                        holder.setVisible(R.id.lv_on_or_off, true);
                        holder.setVisible(R.id.lv_starttimr_inhour, false);


                        switch (model.getEnablestate()) {//任务启停
                            case 0:
                                holder.setChecked(R.id.on_or_off, false);
                                holder.setText(R.id.task_state, getResources().getString(R.string.off));
                                break;
                            case 1:
                                holder.setChecked(R.id.on_or_off, true);
                                holder.setText(R.id.task_state, getResources().getString(R.string.on));
                                break;
                        }

                        holder.setOnClickListener(R.id.on_or_off, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                switch (taskList.get(position - 1).getEnablestate()) {
                                    case 0://零为停用
                                        try {
                                            LogUtils.setLog(mTag, "qiyong" + 1);
                                            startOrStopTask(model, 1, holder);//1为启用
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    case 1:
                                        try {
                                            LogUtils.setLog(mTag, "tingyong" + 0);
                                            startOrStopTask(model, 0, holder);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                }

                            }
                        });
                    } else {
                        holder.setVisible(R.id.lv_on_or_off, false);
                        holder.setVisible(R.id.lv_starttimr_inhour, true);
                        holder.setText(R.id.starttime_inhour, model.getStarttime());
                    }

                    //  LogUtils.setLog(mTag,"每项终端数量"+zoneMachineList.size());//终端数量
                    //  holder.setText(R.id.rv_terminal,zoneMachineList.size()+"");

                }else{
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };
    }

    private void showPopWindow(View v, final int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_pop_window, null);
        final PopupWindow popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new BitmapDrawable());//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(true);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        v.getLocationOnScreen(a);
        popWindow.showAtLocation(list, Gravity.CENTER | Gravity.BOTTOM, 0, Utils.getScreenHeight(mContext) - a[1]);
        Button btnEdit = (Button) view.findViewById(R.id.listen);
        btnEdit.setText(ChinaConstants.modify);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//修改
                LogUtils.setLog(mTag, "修改任务名称" + taskList.get(position).getTaskname());
                Intent intent = null;
                switch (flag) {
                    case IntConstans.keyGuangbo:
                        intent = new Intent(mContext, AddFileBroadActivity.class);
                        break;
                    case IntConstans.keyCaibo:
                        intent = new Intent(mContext, AddCaiboActivity.class);
                        break;
                    case IntConstans.keyGongfang:
                        intent = new Intent(mContext, AddTerminalAmplifierActivity.class);
                        break;
                    case IntConstans.keyWenZiYuYin:
                        intent = new Intent(mContext, AddWenziyuyinActivity.class);
                        break;


                }
                Bundle bundle = new Bundle();
                LogUtils.setLog(mTag, "修改任务名称" + taskList.get(position).getTaskname());
                bundle.putSerializable(CacheConstants.NETWORK_MODEL, taskList.get(position));

                intent.putExtras(bundle);
                startActivity(intent);
                popWindow.dismiss();

            }
        });
        Button btnDelete = (Button) view.findViewById(R.id.tag);
        btnDelete.setText(ChinaConstants.delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {//删除
            @Override
            public void onClick(View v) {

                try {
                    TaskMainMethod TaskMainMethod = new TaskMainMethod(mContext);
                    TaskMainMethod.deleteTask(Integer.parseInt(taskList.get(position).getTaskid()));

                } catch (IOException e) {
                    e.printStackTrace();
                }
                taskList.remove(position);
                updateUI();
                popWindow.dismiss();
            }
        });

    }


    // 启用或停止任务
    private synchronized void startOrStopTask(final TaskGuangboModel model, final int state, final ViewHolder holder) throws IOException {
        taskManageUtils.useOrStopTask(model.getTaskid(), state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                LogUtils.setLog(mTag, "启停成功后" + state);

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        model.setEnablestate(state);
                        if (state == 0) {
                            holder.setChecked(R.id.on_or_off, false);
                            holder.setText(R.id.task_state, getResources().getString(R.string.off));
                        } else {
                            holder.setChecked(R.id.on_or_off, true);
                            holder.setText(R.id.task_state, getResources().getString(R.string.on));
                        }
                        LogUtils.setLog(mTag, "第一个方案启停" + taskList.get(0).getProjectstate());
                    }
                });
            }

            @Override
            public void onTheSameStatu() {
                model.setEnablestate(state);

            }

            @Override
            public void onRetry() {
            }
        });
    }

    //执行或停止方案
    private synchronized void runOrStopTask(final TaskGuangboModel model, final int state) throws IOException {
        taskManageUtils.runOrStopTask(model.getTaskid(), state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                model.setTaskstate(state);
                LogUtils.setLog(mTag, "点击后" + model.getTaskstate());
            }

            @Override
            public void onTheSameStatu() {
                showToast(getResources().getString(R.string.taskcantstart));
                model.setTaskstate(state);
            }

            @Override
            public void onRetry() {

            }
        });
    }



    private synchronized void getTaskListFromSever() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getTaskList + "/" + flag, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskGuangboListRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskGuangboListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<TaskGuangboModel> lists = responseData.getData();
                    taskList.clear();
                    if (lists.get(0).getTaskname() != null) {
                        for (int i = 0; i < lists.size(); i++) {
                            if (lists.get(i).getTaskstate() == 1) {//
                                taskList.add(lists.get(i));
                            }
                        }
                        for (int i = 0; i < lists.size(); i++) {
                            //筛选掉紧急任务

                            if (lists.get(i).getTaskstate() != 1 && lists.get(i).getIsinstancy() != 7) {
                                taskList.add(lists.get(i));
                            }
                        }

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

                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(getApplicationContext(), new BaseActivity.updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getTaskListFromSever();
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

    private void changeTask() {

        Intent intent = null;
        switch (flag) {
            case IntConstans.keyGuangbo:
                intent = new Intent(mContext, AddFileBroadActivity.class);
                break;
            case IntConstans.keyCaibo:
                intent = new Intent(mContext, AddCaiboActivity.class);
                break;
            case IntConstans.keyGongfang:
                intent = new Intent(mContext, AddTerminalAmplifierActivity.class);
                break;
            case IntConstans.keyWenZiYuYin:
                intent = new Intent(mContext, AddWenziyuyinActivity.class);
                break;


        }
        Bundle bundle = new Bundle();
        if (currentPosition != -1) {
            LogUtils.setLog(mTag, "修改任务名称" + taskList.get(currentPosition).getTaskname());
            bundle.putSerializable(CacheConstants.NETWORK_MODEL, taskList.get(currentPosition));

            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    private void deleteTask() {
        if (currentPosition != -1) {
            try {
                TaskMainMethod TaskMainMethod = new TaskMainMethod(mContext);
                TaskMainMethod.deleteTask(Integer.parseInt(taskList.get(currentPosition).getTaskid()));

            } catch (IOException e) {
                e.printStackTrace();
            }
            taskList.remove(currentPosition);
            updateUI();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_go_back:
                finish();
                break;
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
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constant.BUNDLE_KEY_TYPE, flag);
                    bundle.putSerializable(Constant.BUNDLE_KEY_MODEL,taskList.get(currentPosition) );
                    intent.putExtras(bundle);
                    intent.setClass(mContext,  TaskGuangboDetailActivity.class);
                    startActivity(intent);
                } else {
                    showToast("请先选择任务");
                }
                currentPosition = -1;
                break;
            case R.id.title_button:
                switch (flag) {
                    case IntConstans.keyGuangbo:
                        Intent intent2 = new Intent(this, AddFileBroadActivity.class);
                        startActivity(intent2);
                        break;
                    case IntConstans.keyCaibo:
                        Intent intent1 = new Intent(this, AddCaiboActivity.class);
                        startActivity(intent1);
                        break;
                    case IntConstans.keyGongfang:
                        Intent intent3 = new Intent(this, AddTerminalAmplifierActivity.class);
                        startActivity(intent3);
                        break;
                    case IntConstans.keyWenZiYuYin:
                        Intent intent4 = new Intent(this, AddWenziyuyinActivity.class);
                        startActivity(intent4);
                        break;
                }
                break;

        }
    }


    public void initFlag() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        flag = bundle.getInt(Constant.BUNDLE_KEY_TYPE);
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




}
