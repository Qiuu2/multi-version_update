package com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constant.key_terminalName;
import static com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant.allMachineList;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MyLinearLayoutManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.map.MarkerManager;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.MusicPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.TipDialog;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class ZonePop {
    private   Consumer consumer;
    private Context mContext;
    private PopupWindow popWindow;
    private String mTag = "ZonePop";
    private Button title_left;
    private TextView title_tv, title_right;
    private View view;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private LRecyclerViewAdapter LAdapter;
    private CommonAdapter<ZoneModel> mAdapter;
    private int currentPosition = 0;
    private   ArrayList<ZoneModel> zoneList = new ArrayList<>();
    private ArrayList<MachineInfo> zoneMachineList = new ArrayList<>();
    private TipDialog tipCallDialog, tipPlayDialog;

    public ZonePop(Context context,Consumer s) {
        this.mContext = context;
        this.consumer=s;
        showPopWindow();
        initTitle();
        initListView();
    }

    private void initTitle() {
        title_left = (Button) view.findViewById(R.id.title_go_back);
        title_left.setVisibility(View.GONE);
        title_tv = (TextView) view.findViewById(R.id.title_text);
        title_tv.setText(mContext.getResources().getString(R.string.zonelist));
        title_right = (Button) view.findViewById(R.id.title_button);
        title_right.setVisibility(View.GONE);
    }


    private void showPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        view = inflater.inflate(R.layout.activity_task, null);
        RelativeLayout three = (RelativeLayout) view.findViewById(R.id.two);
        three.setVisibility(View.GONE);
        popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        popWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        popWindow.setAnimationStyle(R.style.mypopwindow_anim_style);


    }

    private void initListView() {
        list = new MyLRecycView(mContext, R.color.transparent);
        content = (LinearLayout) view.findViewById(R.id.content);
        empty = (LinearLayout) view.findViewById(R.id.empty);
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
        try {
            getZone();//首页
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void renderView() {
        mAdapter = new CommonAdapter<ZoneModel>(mContext, R.layout.item_zone_on_map, zoneList) {

            @Override
            protected void convert(ViewHolder holder, final ZoneModel zoneModel, final int position) {
                holder.setText(R.id.name, zoneModel.getName());
                holder.setText(R.id.online, zoneModel.getOnline() + "");
                holder.setText(R.id.offline, zoneModel.getOffline() + "");
                holder.setText(R.id.busyinline, zoneModel.getBusyline() + "");
                holder.setOnClickListener(R.id.call, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtils.setLog(mTag, "点击了寻呼"+zoneList.get(position-1).getMachineInfoArrayList().size());
                        tipCallDialog = new TipDialog(mContext, "确定发起寻呼", new TipDialog.OnViewClickListener() {
                            @Override

                            public void onConfirmClick(View v) {
                                for (int i = 0; i < zoneList.get(position-1).getMachineInfoArrayList().size(); i++) {
                                    VariableConstant.chooseMachine.add(zoneList.get(position-1).getMachineInfoArrayList().get(i));
                                }
                                if (VariableConstant.chooseMachine.size() > 0) {
                                    MainMethod.startCall(zoneList.get(position-1).getMachineInfoArrayList());
                                } else {
                                    Toast.makeText(mContext, "请先选择终端", Toast.LENGTH_LONG);
                                }
                                tipCallDialog.dismiss();
                            }

                            @Override
                            public void onCancelClick(View v) {

                            }
                        });
                        tipCallDialog.show();
                    }
                });
                holder.setOnClickListener(R.id.terminal, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                         new ZoneTerminalPop(mContext,zoneModel);

                    }
                });
                holder.setOnClickListener(R.id.onmap, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VariableConstant.zonename=new StringBuffer(zoneList.get(position-1).getName());
                        LogUtils.setLog(mTag,"分区终端数量"+ zoneList.get(position-1).getMachineInfoArrayList().size());

                        Observable.just(zoneList.get(position-1).getMachineInfoArrayList() ).subscribe(consumer);
                        popWindow.dismiss();
                    }
                });
                holder.setOnClickListener(R.id.play, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tipPlayDialog = new TipDialog(mContext, "确定发起点播", new TipDialog.OnViewClickListener() {
                            @Override

                            public void onConfirmClick(View v) {
                                tipPlayDialog.dismiss();
                                if (zoneList.get(position-1).getMachineInfoArrayList().size() > 0) {//在选音乐里面添加到静态变量里面

                                    new MusicPop(mContext, "", new ArrayList<MusicInfoModel>(), zoneList.get(position-1).getMachineInfoArrayList() );
                                } else {
                                    Toast.makeText(mContext, "请先选择终端", Toast.LENGTH_LONG);
                                }
                            }

                            @Override
                            public void onCancelClick(View v) {

                            }
                        });
                        tipPlayDialog.show();
                    }
                });
                LogUtils.setLog(mTag, "列表" + position);

            }

        };
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

    public void getZoneTerminal(final int i) throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getGroupTerminal + "/" + zoneList.get(i).getId(), new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                zoneMachineList.clear();

                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> list = responseData.getData();
                    int pp=-1;
                    if(list.get(0).getName()!=null) {
                        for (int i = 0; i < list.size(); i++) {//去除自身
                            if (list.get(i).getName().equals(PreferencesUtil.getInstance().getField(key_terminalName, mContext))) {
                                LogUtils.setLog(mTag, "去除自身" + list.get(i).getName());
                                pp = i;
                                break;
                            }
                        }
                    }
                    if(pp!=-1) {
                        list.remove(list.get(i));
                    }
                    int inline = 0;
                    int busy = 0;
                    int offline = 0;
                    if (zoneList.get(i).getId() == 0) {

                        for (int i = 0; i < allMachineList.size(); i++) {
                            zoneMachineList.add(allMachineList.get(i));
                            if (zoneMachineList.get(i).getDevicestate() == 1 && zoneMachineList.get(i).getNetstate() == 1) {
                                if (zoneMachineList.get(i).getTaskstate() == 0) {
                                    inline++;
                                } else {
                                    busy++;
                                }
                            } else {
                                offline++;
                            }
                        }
                        zoneList.get(i).setMachineInfoArrayList(allMachineList);
                    } else {
                        for (int j = 0; j < list.size(); j++) {
                            zoneMachineList.add(list.get(j));
                            if (zoneMachineList.get(j).getDevicestate() == 1 && zoneMachineList.get(j).getNetstate() == 1) {
                                if (zoneMachineList.get(j).getTaskstate() == 0) {
                                    inline++;
                                } else {
                                    busy++;
                                }
                            } else {
                                offline++;
                            }
                        }
                        zoneList.get(i).setMachineInfoArrayList(list);
                    }
                    zoneList.get(i).setOffline(offline);
                    zoneList.get(i).setOnline(inline);
                    zoneList.get(i).setBusyline(busy);
                }
                currentPosition++;
                if (currentPosition < zoneList.size()) {
                    try {
                        getZoneTerminal(currentPosition);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            content.removeViewAt(0);
                            content.addView(list, 0);
                            list.setAdapter(LAdapter);
                            updateUI();
                        }
                    });
                    currentPosition = 0;
                }
            }
            @Override
            public void onFailed(int i, String s) {
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
                    zoneList.add(0, new ZoneModel(zoneList.size() + "", 0, 0, 0, 0, "无", "全部终端", "全部终端"));
                    LogUtils.setLog(mTag, "zoneList sizes" + zoneList.size() + zoneList.get(0).getId());
                    VariableConstant.zoneListAll=zoneList;
                    try {
                        getZoneTerminal(currentPosition);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, ChinaConstants.zonegetfail, Toast.LENGTH_LONG);
                    }
                });
            }
        });
    }
}
