package com.htgd.radiocontrol.aeroradiocontrol.fragment;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.htapplib.HTIntf;
import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.adapter.GVShortTaskAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseFragment;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskGuangboModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.recyclerview.widget.GridLayoutManager;

/**
 * Created by wzq on 2017-07-12.
 */
public class FragmentShortcutTask extends BaseFragment {
    private Context mContext;
    private ArrayList<TaskGuangboModel> taskGuangbolist = new ArrayList<>();
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private CommonAdapter<TaskGuangboModel> mAdapter;
    private LRecyclerViewAdapter LAdapter;
    private String mTag="FragmentShortcutTask";

    public static FragmentShortcutTask newInstance(String text, Context mContext) {
        FragmentShortcutTask fragmentShortcutTask = new FragmentShortcutTask();
        Bundle bundle = new Bundle();
        fragmentShortcutTask.setArguments(bundle);
        return fragmentShortcutTask;
    }

    public FragmentShortcutTask() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = this.getActivity();
        View view = inflater.inflate(R.layout.common_list, container, false);
        initListview(view);


       /* shortTask.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                htIntf.newshortcuttaskitem();
                htIntf.setshortcuttaskitem(taskGuangbolist.get(position).getTaskid());
                htIntf.startshortcuttask();

                *//* htIntf.newshortcutpagingitem();
               LogUtils.setLog(mTAG,"the shortcutkey id is" + shorCutKeys.get(b).getId());
                htIntf.setshortcutpagingitem(shorCutKeys.get(position).getId());
                htIntf.startpaging();*//*

            }
        });*/
        try {
            getDataFromSever();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return view;
    }
    private void initListview(View v) {
        list = new MyLRecycView(getActivity(), R.color.transparent);
        content = (LinearLayout) v.findViewById(R.id.content);
        empty = (LinearLayout) v.findViewById(R.id.empty);
        list.setLayoutManager(new GridLayoutManager(mContext, 3, GridLayoutManager.VERTICAL, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 20, 0, 0);
        list.setLayoutParams(lp);
        renderView();
        LAdapter = new LRecyclerViewAdapter(mAdapter);
        LAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                htIntf.newshortcuttaskitem();
                htIntf.setshortcuttaskitem(Integer.parseInt(taskGuangbolist.get(position).getTaskid()));
                htIntf.startshortcuttask();
                Cons.shorttask=taskGuangbolist.get(position);
            }
        });
        list.setAdapter(LAdapter);
        list.setEmptyView(empty);
        list.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    getDataFromSever();//首页
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
                    getDataFromSever();//首页
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void renderView() {
        LogUtils.setLog(mTag,"快捷任务数量s"+taskGuangbolist.size());
        mAdapter = new CommonAdapter<TaskGuangboModel>(mContext, R.layout.item_machine, taskGuangbolist) {
            @Override
            protected void convert(ViewHolder viewHolder, final TaskGuangboModel s, int i) {
                if (s != null) {
                    viewHolder.setText(R.id.name, s.getName());//昵称

                    RelativeLayout IvUser = (RelativeLayout) viewHolder.getView(R.id.iv_user);
                    if (s.getPlaying() != 0) {
                        IvUser.setBackgroundResource(R.mipmap.machine_busy);
                    } else {
                        IvUser.setBackgroundResource(R.mipmap.anull);
                    }
                }else {
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };

    }
  //获取快捷任务
    private void getDataFromSever() throws IOException {
        MyRequestBuilder requestBuilder = new MyRequestBuilder(mContext);
        requestBuilder.setUrl(Constant.postGetShortcutTask);
        requestBuilder.setNeedToken(true);
        requestBuilder.setBodyMap(new HashMap<String, String>() {{
            put("id", "5");
        }});
        RequestManger.getInstance().postHashMap(requestBuilder, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskGuangboListRsp responeData = JsonUtil.getInstance().deSerializeString(response, TaskGuangboListRsp.class);
                if (responeData != null && responeData.getData() != null && responeData.getData().size() > 0) {
                    if(responeData.getData().get(0).getName()!=null) {
                        taskGuangbolist.clear();
                        for (int i = 0; i < responeData.getData().size(); i++) {
                            taskGuangbolist.add(responeData.getData().get(i));
                        }
                        LogUtils.setLog(mTag, "快捷任务数量" + taskGuangbolist.size());
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                content.removeViewAt(0);
                                content.addView(list, 0);
                                list.setAdapter(LAdapter);
                                updateUI();
                            }
                        });

                    }else{
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.setLog(mTag, "meishuju"+taskGuangbolist.size());
                                content.removeViewAt(0);
                                content.addView(list, 0);
                                list.refreshComplete(taskGuangbolist.size());//停止刷新
                                list.setEmptyView(empty);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog("get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }

    private void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
                LogUtils.setLog(mTag,"updateui"+taskGuangbolist.size());

                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(taskGuangbolist.size());//停止刷新
            }
        });
    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
                        getDataFromSever();
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

    private void runThetask() {

    }
}
