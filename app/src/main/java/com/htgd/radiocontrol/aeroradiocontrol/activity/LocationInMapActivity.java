package com.htgd.radiocontrol.aeroradiocontrol.activity;


import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constant.key_terminalName;
import static com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans.terminaltype;
import static com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant.allMachineList;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polygon;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineListRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PermissionUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.map.ClusterManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.map.view.Cluster;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.MusicPop;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.TipDialog;
import com.htgd.radiocontrol.aeroradiocontrol.widget.popwindow.ZonePop;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Consumer;

/**
 * 激活页面中，可以在地图上mark一个地点，返回一个地址
 * Created by wzq on 2019/1/3.
 */

public class LocationInMapActivity extends BaseActivity implements View.OnClickListener, MKOfflineMapListener, BaiduMap.OnMapLoadedCallback {


    private MapView mMapView = null;//地图布局控件
    private BaiduMap mBaiduMap = null;//百度地图
    private GeoCoder mSearch = null;
    private String address = "";
    private TextView latlng, online, busyinline, outofline, chooseline, state, resets, zonename;
    private double latitude = 0;//地图取点的经纬度
    private double longitude = 0;
    private double mlatitude = 0;//定位的经纬度//缺省某台终端的经纬度
    private double mlongitude = 0;
    private double latitudep1 = 0;//矩形顶点1
    private double longitudep1 = 0;
    private double latitudep2 = 0;//矩形顶点2
    private double longitudep2 = 0;
    private Context mContext;
    private String mTag = "LocationInMapActivity";
    // 定位相关
    LocationClient mLocClient;
    private ArrayList<MachineInfo> machineInfoArrayList = new ArrayList<>();
    private MapStatus mMapStatus;
    private ClusterManager<MachineInfo> mClusterManager;
    private MarkerOptions ooc;
    private Button zone;
    private MachineInfo tempmachine = new MachineInfo("", "", "");
    private boolean isedit_lat = false;
    private LinearLayout fourstate;
    private androidx.appcompat.widget.SwitchCompat edit_lat;
    private Button select, edit, clear_select;
    private boolean isstate = true;
    private boolean isshoworhide = true;
    private Button call, play, speach, back;
    private ArrayList<MachineInfo> chooseMachine = new ArrayList<MachineInfo>();
    private ArrayList<OverlayOptions> MarkerOptionsList = new ArrayList<OverlayOptions>();
    private ArrayList<OverlayOptions> markerOptionsLists = new ArrayList<OverlayOptions>();
    private boolean makepolygon = false;
    private boolean hasp1 = false;//是否有第一个点
    private double maxla, minla, maxlo, minlo;
    private UiSettings mUiSettings;
    private Boolean guestenable = true;
    private ProgressBar upWait;
    public Timer timer = new Timer();
    private TimerTask timerTask;
    private MarkerOptions ooA;
    private List<Overlay> overlays;
    private RelativeLayout operation;
    private TimerTask timerTasks;
    private int busy = 0;
    private int offline = 0;
    private int inline = 0;
    private int choose_num = 0;
    private ArrayList<MachineInfo> machineInfos = new ArrayList<>();
    private boolean haschange = false;
    private Button confirm, cancle;
    private float locationx;
    private float locationy;
    private ArrayList<MachineInfo> kuangChoose = new ArrayList<>();
    private boolean isdestroy = false;
    private Notification mNotification;
    private LocationClient mClient;
    private MyLocationListener myLocationListener;
    private boolean isneedshow=false;
    private TextView showorhide;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isdestroy = true;
        stopRefreshMachine();
        guestenable = true;
        mBaiduMap.clear();
        mMapView.onDestroy();

        // 关闭前台定位服务
        mClient.disableLocInForeground(true);
        // 取消之前注册的 BDAbstractLocationListener 定位监听函数
        mClient.unRegisterLocationListener(myLocationListener);
        // 停止定位sdk
        mClient.stop();
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.setLog(mTag, "onStart");
    }


    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        LogUtils.setLog(mTag, "onpause");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_locationinmap;
    }

    @Override
    protected void initSubViews() {
        mContext = this;
        new PermissionUtils(mContext).judgePermission("android.permission.ACCESS_COARSE_LOCATION");
        new PermissionUtils(mContext).judgePermission("android.permission.ACCESS_FINE_LOCATION");
        initUI();
        initCluster();
        try {
            refreshMachineListfirst();
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshMachineStateInTime();
        createLocalLocation();

    }



    private void createLocalLocation() {
        // 创建定位客户端
        // Baidu Location SDK v9.6+ declares throws on the LocationClient constructor
        // to force the caller to handle the case where setAgreePrivacy has not been
        // called. We set agree privacy in MyApplication.initBaiduMap(), so this
        // catch is defensive — log and bail rather than crash if it ever throws.
        try {
            mClient = new LocationClient(this);
        } catch (Exception e) {
            LogUtils.setLog(mTag, "LocationClient init failed: " + e.getMessage());
            return;
        }
          myLocationListener = new MyLocationListener();
        // 注册定位监听
        mClient.registerLocationListener(myLocationListener);
        LocationClientOption mOption = new LocationClientOption();
         // 可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        mOption.setScanSpan(2000);
        // 可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        mOption.setCoorType("bd09ll");
        // 可选，默认false，设置是否开启Gps定位
        mOption.setOpenGps(true);
         // 设置定位参数
        mClient.setLocOption(mOption);
         // 启动定位
        mClient.start();
       // LogUtils.setLog(mTag, "dingwei kaiqi"  );
    }

    class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
          //  LogUtils.setLog(mTag, "dingwei" + bdLocation.getLocationDescribe());
            //   此处获取后台定位数据
        }
    }

    //定时刷新终端列表
    public void refreshMachineStateInTime() {

        LogUtils.setLog(mTag, "KAISHISHUAXIN");
        if (timer == null) {
            timer = new Timer();
        }
        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        refreshMachineList();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            };
            timer.schedule(timerTask, 6000, IntConstans.RefreshPeriodTimeInMap);//
        }
    }

    public void stopRefreshMachine() {
        LogUtils.setLog(mTag, "TINGZHISHUAXIN");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timerTasks != null) {
            timerTasks.cancel();
            timerTasks = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.setLog(mTag, "onResume");

        mMapView.onResume();
        if (edit != null) {
            LogUtils.setLog(mTag, "editbuweikong");
        }
        if (timerTask != null) {
            LogUtils.setLog(mTag, "定时刷新未结束");
        }
    }



    // 刷新设备信息
    private synchronized void refreshMachineList() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelistAll, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                LogUtils.setLog(mTag + "refreshMachineList" + code + "message is" + responseData.getData().size());
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    machineInfoArrayList.clear();
                    if (lists.size() > 0) {
                        for (int i = 0; i < lists.size(); i++) {
                            if (lists.get(i).getName().equals(PreferencesUtil.getInstance().getField(key_terminalName, mContext))) {
                            } else {
                                machineInfoArrayList.add(lists.get(i));
                                for (int j = 0; j < machineInfos.size(); j++) {
                                    if (machineInfos.get(j).getId() == lists.get(i).getId()) {
                                        if (machineInfos.get(j).getDevicestate() != lists.get(i).getDevicestate() || machineInfos.get(j).getNetstate() != lists.get(i).getNetstate()
                                                ||machineInfos.get(j).getTaskstate()!=lists.get(i).getTaskstate()) {
                                            haschange = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (haschange && !isdestroy) {
                            mClusterManager.clearItems();
                            for (int k = 0; k < machineInfoArrayList.size(); k++) {
                                for (int d = 0; d < machineInfos.size(); d++) {
                                    if (machineInfoArrayList.get(k).getId() == machineInfos.get(d).getId()) {
                                        machineInfos.get(d).setDevicestate(machineInfoArrayList.get(k).getDevicestate());
                                        machineInfos.get(d).setNetstate(machineInfoArrayList.get(k).getNetstate());
                                        tempmachine = machineInfos.get(d);
                                        MachineInfo machineInfoss = new MachineInfo(tempmachine.getType(), tempmachine.getTaskstate(), tempmachine.getDevicestate(), tempmachine.getNetstate(), tempmachine.getSpeechstate(), tempmachine.getVolume(), tempmachine.getIsinstancy(), tempmachine.getName(), tempmachine.getIp(), tempmachine.getId(), tempmachine.getGroupid(), tempmachine.getLongitude(), tempmachine.getLatitude());
                                        machineInfoss.setmPosition(new LatLng(tempmachine.getPosition().latitude, tempmachine.getPosition().longitude));
                                        machineInfoss.getBitmapDescriptor();
                                        LogUtils.setLog(mTag, "改变了状态的终端" + tempmachine.getName());
                                        mClusterManager.addItem(machineInfoss);
                                    }
                                }
                            }
                            mClusterManager.cluster();
                            haschange = false;
                        } else {
                            LogUtils.setLog(mTag, "无终端改变状态" + machineInfoArrayList.size());
                        }
                    }
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshText();


                    }
                });


            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag + "get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }

    private void refreshText() {
        busy = 0;
        offline = 0;
        inline = 0;
        for (int i = 0; i < machineInfos.size(); i++) {
            if (machineInfos.get(i).getDevicestate() == 1 && machineInfos.get(i).getNetstate() == 1) {
                if (machineInfos.get(i).getTaskstate() == 0) {
                    inline++;
                } else {
                    busy++;
                }
            } else {
                offline++;
            }
        }
        busyinline.setText("忙碌终端：  " + busy);
        outofline.setText("离线终端：  " + offline);
        online.setText("空闲终端：  " + inline);
    }

    private synchronized void refreshMachineListfirst() throws IOException {
        RequestManger.getInstance().get(Constant.serveraddress + Constant.getMahcinelistAll, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MachineListRsp responseData = JsonUtil.getInstance().deSerializeString(response, MachineListRsp.class);
                LogUtils.setLog(mTag + "refreshMachineList" + code + "message is" + responseData.getData().size());
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    ArrayList<MachineInfo> lists = responseData.getData();
                    if (lists.size() > 0) {
                        for (int i = 0; i < lists.size(); i++) {
                            LogUtils.setLog(mTag,"加一个扩声a"+PreferencesUtil.getInstance().getField(key_terminalName, mContext));
                            isneedshow=false;
                            for (int j = 0; j < terminaltype.length; j++) {
                                LogUtils.setLog(mTag,"加一个扩声"+lists.get(i).getName());
                                if (!lists.get(i).getName().equals(PreferencesUtil.getInstance().getField(key_terminalName, mContext))) {
                                    LogUtils.setLog(mTag,terminaltype[j]+"加一个扩声bb"+lists.get(i).getType());
                                    if(lists.get(i).getType()==terminaltype[j]){
                                        LogUtils.setLog(mTag,"加一个扩声");
                                        isneedshow=true;
                                        break;
                                    }
                                }
                            }
                            if(isneedshow){
                                if (lists.get(i).getLatitude().startsWith("0") && lists.get(i).getLongitude().startsWith("0")) {
                                    double a = new BigDecimal(mlatitude + (new Random().nextInt(100000)) * 0.000001).setScale(6, BigDecimal.ROUND_DOWN).doubleValue();
                                    double b = new BigDecimal(mlongitude + (new Random().nextInt(100000)) * 0.000001).setScale(6, BigDecimal.ROUND_DOWN).doubleValue();
                                    lists.get(i).setLatitude(a + "");
                                    lists.get(i).setLongitude(b + "");
                                }
                                LogUtils.setLog(mTag,"加一个扩声s");
                                machineInfos.add(lists.get(i));
                            }
                        }
                        for (int i = 0; i < machineInfos.size(); i++) {
                            if (machineInfos.get(i).getLatitude() != null && machineInfos.get(i).getLongitude() != null) {
                                machineInfos.get(i).setmPosition(new LatLng(Double.parseDouble(machineInfos.get(i).getLatitude()), Double.parseDouble(machineInfos.get(i).getLongitude())));
                                machineInfos.get(i).getBitmapDescriptor();
                                Log.e(i + "machineinfo", "getBitmapDescriptor  !!" + machineInfos.get(i).getName());
                            }
                        }
                        mClusterManager.addItems(machineInfos);
                        mMapStatus = new MapStatus.Builder().zoom(7).build();
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));
                        mClusterManager.cluster();
                    }
                    LogUtils.setLog(mTag, "第一次添加数据"+machineInfos.size());
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        busy = 0;
                        offline = 0;
                        inline = 0;
                        choose_num = 0;
                        for (int i = 0; i < machineInfos.size(); i++) {
                            if (machineInfos.get(i).getDevicestate() == 1 && machineInfos.get(i).getNetstate() == 1) {
                                if (machineInfos.get(i).getTaskstate() == 0) {
                                    inline++;
                                } else {
                                    busy++;
                                }
                            } else {
                                offline++;
                            }
                        }
                        busyinline.setText("忙碌终端：  " + busy);
                        outofline.setText("离线终端：  " + offline);
                        online.setText("空闲终端：  " + inline);
                        chooseline.setText("已选终端：  " + choose_num);
                        zonename.setText("分区名称： " + "全部终端");
                    }
                });


            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog(mTag + "get failed code is" + code + "message is" + message);
                if (EorroCode.TOKEN_EXPIRED == code) {
                    reTry();
                }
            }
        });
    }

    private void updateCluster() {

        mClusterManager.clearItems();
        for (int i = 0; i < machineInfos.size(); i++) {
            tempmachine = machineInfos.get(i);
            MachineInfo machineInfoss = new MachineInfo(tempmachine.getType(), tempmachine.getTaskstate(), tempmachine.getDevicestate(), tempmachine.getNetstate(), tempmachine.getSpeechstate(), tempmachine.getVolume(), tempmachine.getIsinstancy(), tempmachine.getName(), tempmachine.getIp(), tempmachine.getId(), tempmachine.getGroupid(), tempmachine.getLongitude(), tempmachine.getLatitude());
            machineInfoss.setChoose(machineInfos.get(i).isChoose());
            machineInfoss.setmPosition(new LatLng(machineInfos.get(i).getPosition().latitude, machineInfos.get(i).getPosition().longitude));
            machineInfoss.getBitmapDescriptor();
            mClusterManager.addItem(machineInfoss);
        }
        mClusterManager.cluster();
        int choose_nu = 0;
        for (int i = 0; i < machineInfos.size(); i++) {
            if (machineInfos.get(i).isChoose()) {
                choose_nu++;
            }
        }
        chooseline.setText("已选终端：  " + choose_nu);


    }

    // token 失效时重新验证，验证成功后重新请求服务器
    private void reTry() {
        try {
            updateToken(mContext, new BaseActivity.updatelister() {
                @Override
                public void onSucess() {

                    try {
                        refreshMachineList();
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

    public void hideProgress() {
        if (upWait != null) {
            upWait.setVisibility(View.GONE);
        }
    }

    public void showUpProgress() {//上传时的动态
        if (upWait != null) {
            upWait.setVisibility(View.VISIBLE);
        }
    }

    private void initUI() {
        //获取地图类
        latlng = findViewById(R.id.latlng);
        upWait = findViewById(R.id.up_wait);
        upWait.setVisibility(View.GONE);

        state = (TextView) findViewById(R.id.state);
        operation = findViewById(R.id.operation);
        state.setOnClickListener(this);
        showorhide = (TextView) findViewById(R.id.showorhide);
        showorhide.setOnClickListener(this);
        resets = (TextView) findViewById(R.id.resets);
        resets.setOnClickListener(this);
        edit_lat = findViewById(R.id.edit_lat);
        edit_lat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isedit_lat = b;
                if (isedit_lat) {
                    edit.setVisibility(View.VISIBLE);
                } else {
                    edit.setVisibility(View.GONE);
                    if (overlays != null) {
                        LogUtils.setLog(mTag, "清除overlays");
                        mBaiduMap.removeOverLays(overlays);
                    }
                }
            }
        });
        edit = findViewById(R.id.edit);
        edit.setOnClickListener(this);
        select = (Button) findViewById(R.id.select);
        select.setOnClickListener(this);
        clear_select = (Button) findViewById(R.id.clear_select);
        clear_select.setOnClickListener(this);
        call = (Button) findViewById(R.id.call);
        call.setOnClickListener(this);
        play = (Button) findViewById(R.id.play);
        play.setOnClickListener(this);
        back = (Button) findViewById(R.id.back);
        back.setOnClickListener(this);
        speach = (Button) findViewById(R.id.speach);
        speach.setOnClickListener(this);
        fourstate = findViewById(R.id.four_state);
        zone = (Button) findViewById(R.id.zone);
        zone.setOnClickListener(this);
        online = (TextView) findViewById(R.id.online);
        busyinline = (TextView) findViewById(R.id.busyinline);
        outofline = (TextView) findViewById(R.id.outofline);
        chooseline = (TextView) findViewById(R.id.choose);
        zonename = (TextView) findViewById(R.id.zonename);
        mMapView = findViewById(R.id.bmapViews);
        mBaiduMap = mMapView.getMap();
        mUiSettings = mBaiduMap.getUiSettings();
        mUiSettings.setAllGesturesEnabled(false);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mBaiduMap.setOnMapLoadedCallback(this);
        //获取定位
        getLocalLocation();
        getMapPointMessage();
    }

    private void initCluster() {
        // 定义点聚合管理类ClusterManager
        mClusterManager = new ClusterManager<MachineInfo>(this, mBaiduMap);
        // 设置地图监听，当地图状态发生改变时，进行点聚合运算
        mBaiduMap.setOnMapStatusChangeListener(mClusterManager);
        // 设置maker点击时的响应
        mBaiduMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MachineInfo>() {
            @Override
            public boolean onClusterClick(Cluster<MachineInfo> cluster) {
                List<MachineInfo> items = (List<MachineInfo>) cluster.getItems();
                LatLngBounds.Builder builder2 = new LatLngBounds.Builder();
                int i = 0;
                for (MachineInfo myItem : items) {
                    builder2 = builder2.include(myItem.getPosition());
                }
                LatLngBounds latlngBounds = builder2.build();
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngBounds(latlngBounds, mMapView.getWidth(), mMapView.getHeight());
                mBaiduMap.animateMapStatus(u);
                return false;
            }
        });


        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MachineInfo>() {
            @Override
            public boolean onClusterItemClick(MachineInfo item) {
                mClusterManager.clearItems();
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).getId() == item.getId()) {
                        LogUtils.setLog(mTag, "选中的终端" + item.getName());
                        MachineInfo s = machineInfos.get(i);
                        s.setChoose(!s.isChoose());
                        MachineInfo machineInfoss = new MachineInfo(s.getType(), s.getTaskstate(), s.getDevicestate(), s.getNetstate(), s.getSpeechstate(), s.getVolume(), s.getIsinstancy(), s.getName(), s.getIp(), s.getId(), s.getGroupid(), s.getLongitude(), s.getLatitude());
                        machineInfoss.setmPosition(new LatLng(s.getPosition().latitude, s.getPosition().longitude));
                        machineInfoss.setChoose(s.isChoose());
                        machineInfoss.getBitmapDescriptor();
                        LogUtils.setLog(mTag, "改变选中" + s.getName() + machineInfoss.isChoose());
                        mClusterManager.addItem(machineInfoss);
                        //changenewmachine(item);
                    } else {

                        mClusterManager.addItem(machineInfos.get(i));
                    }
                }
                mClusterManager.cluster();
                int choose_nu = 0;
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).isChoose()) {
                        choose_nu++;
                    }
                }
                chooseline.setText("已选终端：  " + choose_nu);
                return false;
            }
        });
    }

    @Override
    public void onMapLoaded() {
        // TODO Auto-generated method stub\


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                locationx = event.getX();
                locationy = event.getY();
                LogUtils.setLog(mTag, locationx + "触摸位置" + locationy);
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:


        }
        return super.onTouchEvent(event);
    }

    private void getMapPointMessage() {//点击地图上的点获取点位信息

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {


            @Override
            public void onMapPoiClick(MapPoi arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onMapClick(LatLng arg0) {
                // TODO Auto-generated method stub
                LogUtils.setLog(mTag, "点击过地图onMapClick");
                mBaiduMap.hideInfoWindow();
                latitude = arg0.latitude;
                longitude = arg0.longitude;
                // 实例化一个地理编码查询对象
                ReverseGeoCodeOption op = new ReverseGeoCodeOption();
                op.location(arg0);
                // 发起反地理编码请求(经纬度->地址信息)
                mSearch.reverseGeoCode(op);
                mSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {


                    @Override
                    public void onGetReverseGeoCodeResult(
                            ReverseGeoCodeResult arg0) {
                        // 获取点击的坐标地址
                        address = arg0.getAddress();
                        String strInfo;
                        strInfo = "当前位置:" + address;
                        LogUtils.setLog(mTag, "当前位置" + address + "经纬度" + latitude + "--" + longitude);
                        tempmachine = new MachineInfo(address, longitude + "", latitude + "");
                        if (isedit_lat) {
                            if (!makepolygon) {
                                BitmapDescriptor mbitmap = BitmapDescriptorFactory.fromResource(R.mipmap.s_map);
                                if (overlays != null) {
                                    LogUtils.setLog(mTag, "清除overlays");
                                    mBaiduMap.removeOverLays(overlays);
                                }
                                ooA = new MarkerOptions().position(new LatLng(latitude, longitude)).icon(mbitmap);
                                MarkerOptionsList.clear();
                                MarkerOptionsList.add(ooA);
                                overlays = mBaiduMap.addOverlays(MarkerOptionsList);
                           /* mClusterManager.cluster();
                            float zoom = mBaiduMap.getMapStatus().zoom;
                            LogUtils.setLog(mTag,"单击聚合点缩放等级"+zoom);
                            mMapStatus = new MapStatus.Builder().zoom(zoom-1).build();
                            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));
                            mMapStatus = new MapStatus.Builder().zoom(zoom ).build();
                            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));*/


                                latlng.setText(strInfo);
                            }
                        }

                        if (makepolygon) {//拾取矩形顶点
                            LogUtils.setLog(mTag, "拾取矩形顶点");
                            if (hasp1) {

                                /*mBaiduMap.removeOverLays(overlayss);
                                mBaiduMap.re(mPolygonTwo);*/
                                markerOptionsLists.clear();
                                markerOptionsLists.add(ooc);
                                LogUtils.setLog(mTag, "添加矩形顶点1" + ooc.getPosition().latitude);
                                latitudep2 = latitude;
                                longitudep2 = longitude;
                                LogUtils.setLog(mTag, longitudep2 + "拾取矩形顶点2" + latitudep2);
                                BitmapDescriptor mbitmap = BitmapDescriptorFactory.fromResource(R.mipmap.map_2);
                                MarkerOptions oob = new MarkerOptions().position(new LatLng(latitudep2, longitudep2)).icon(mbitmap);
                                markerOptionsLists.add(oob);
                                mBaiduMap.clear();
                                mBaiduMap.addOverlays(markerOptionsLists);
                                mBaiduMap.addOverlays(markerOptionsLists);
                                polygon(new LatLng(latitudep1, longitudep1), new LatLng(latitudep2, longitudep2));
                                setPolygonSelectMachine();

                                updateCluster();
                                showPopWindow(select);
                            } else {
                                latitudep1 = latitude;
                                longitudep1 = longitude;
                                hasp1 = true;
                                LogUtils.setLog(mTag, longitudep1 + "拾取矩形顶点1" + latitudep1);
                                BitmapDescriptor mbitmap = BitmapDescriptorFactory.fromResource(R.mipmap.map_1);
                                ooc = new MarkerOptions().position(new LatLng(latitudep1, longitudep1)).icon(mbitmap);
                                markerOptionsLists.add(ooc);
                                mBaiduMap.addOverlays(markerOptionsLists);
                            }
                        }

                    }

                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult arg0) {
                    }
                });
            }
        });
    }

    private void showPopWindow(View v) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.select_button, null);
        final PopupWindow popWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        popWindow.setBackgroundDrawable(new BitmapDrawable());//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(false);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        v.getLocationOnScreen(a);
        popWindow.showAtLocation(select, Gravity.CENTER | Gravity.BOTTOM, 0, Utils.getScreenHeight(mContext) - a[1]);
        confirm = (Button) view.findViewById(R.id.listen);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//确定
                LogUtils.setLog(mTag, "是否可以操作手势" + guestenable);
                mUiSettings.setAllGesturesEnabled(false);
                mUiSettings.setScrollGesturesEnabled(true);
                mUiSettings.setZoomGesturesEnabled(true);
                makepolygon = false;
                edit_lat.setChecked(false);
                ooc = null;
                markerOptionsLists.clear();
                ooA = null;
                hasp1 = false;
                mBaiduMap.clear();
                updateCluster();
                popWindow.dismiss();
            }
        });
        cancle = (Button) view.findViewById(R.id.tag);
        cancle.setOnClickListener(new View.OnClickListener() {//取消
            @Override
            public void onClick(View v) {
                mUiSettings.setAllGesturesEnabled(false);
                mUiSettings.setScrollGesturesEnabled(true);
                mUiSettings.setZoomGesturesEnabled(true);

                mUiSettings.setRotateGesturesEnabled(false);
                makepolygon = false;
                ooc = null;
                markerOptionsLists.clear();
                ooA = null;
                hasp1 = false;
                for (int i = 0; i < machineInfos.size(); i++) {
                    for (int j = 0; j < kuangChoose.size(); j++) {
                        if (machineInfos.get(i).getId() == kuangChoose.get(j).getId()) {
                            machineInfos.get(i).setChoose(false);
                        }
                    }
                }
                mBaiduMap.clear();
                updateCluster();
                popWindow.dismiss();
            }
        });

    }

    private void polygon(LatLng l1, LatLng l2) {     // 绘制矩形
        LogUtils.setLog(mTag, "绘制矩形" + "经纬度" + l1.latitude + "--" + l1.longitude);
        maxla = l1.latitude > l2.latitude ? l1.latitude : l2.latitude;
        minla = l1.latitude < l2.latitude ? l1.latitude : l2.latitude;
        maxlo = l1.longitude > l2.longitude ? l1.longitude : l2.longitude;
        minlo = l1.longitude < l2.longitude ? l1.longitude : l2.longitude;
        LogUtils.setLog(mTag, "maxla" + maxla + "minla" + minla + "maxlo" + maxlo + "minlo" + minlo);
        LatLng latLngF = new LatLng(minla, minlo);
        LatLng latLngG = new LatLng(maxla, minlo);
        LatLng latLngH = new LatLng(maxla, maxlo);
        LatLng latLngL = new LatLng(minla, maxlo);
        List<LatLng> LatLngs = new ArrayList<LatLng>();
        LatLngs.add(latLngF);
        LatLngs.add(latLngG);
        LatLngs.add(latLngH);
        LatLngs.add(latLngL);

        OverlayOptions overlayOptions = new PolygonOptions()
                .points(LatLngs)// 设置多边形坐标点列表
                .stroke(new Stroke(IntConstans.mStrokeWidth, Color.argb(255, IntConstans.mColor, 0, 0)))// 设置多边形边框信息
                .fillColor(Color.argb(IntConstans.mFillAlpha, 0, 0, 255));// 设置多边形填充颜色
        // 添加覆盖物
        mBaiduMap.addOverlay(overlayOptions);
    }

    private void setPolygonSelectMachine() {//设置矩形框内选中的终端
        //clearSelect();
        kuangChoose.clear();
        LogUtils.setLog(mTag, "maxla" + maxla + "minla" + minla + "maxlo" + maxlo + "minlo" + minlo);
        for (int i = 0; i < machineInfos.size(); i++) {
            LogUtils.setLog(mTag, " 框选终端的数量e" + machineInfoArrayList.get(i).getName());
            if (Double.parseDouble(machineInfos.get(i).getLatitude()) > minla && Double.parseDouble(machineInfos.get(i).getLatitude()) < maxla &&
                    Double.parseDouble(machineInfos.get(i).getLongitude()) > minlo && Double.parseDouble(machineInfos.get(i).getLongitude()) < maxlo) {
                machineInfos.get(i).setChoose(true);
                kuangChoose.add(machineInfos.get(i));
                LogUtils.setLog(mTag, "框选中的终端名称" + machineInfos.get(i).getName());
            }
        }
    }

    @Override
    public void onGetOfflineMapState(int i, int i1) {

    }


    //获取手机当前定位
    private void getLocalLocation() {
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        // 定位初始化 (see createLocalLocation for why this is wrapped)
        try {
            mLocClient = new LocationClient(this);
        } catch (Exception e) {
            LogUtils.setLog(mTag, "LocationClient init failed: " + e.getMessage());
            return;
        }
        //mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(5000); // 定位时间间隔
        option.setIsNeedAddress(true); // 返回省市区等地址信息

        mLocClient.setLocOption(option);
        mLocClient.start();
    }

    //保存设备经纬度
    public void savegitude(final MachineInfo chooseMachine) throws IOException {
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(chooseMachine);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.saveMahcineLatitude);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(final int code, String response) {
                LogUtils.setLog(mTag, "经纬度存储" + code);

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (code == 200) {
                            showToast("编辑成功");
                        } else {
                            showToast("编辑失败");
                        }
                        mBaiduMap.clear();
                        mClusterManager.clearItems();
                        for (int i = 0; i < machineInfos.size(); i++) {
                            if (machineInfos.get(i).getId() == chooseMachine.getId()) {
                                machineInfos.get(i).setLatitude(chooseMachine.getLatitude());
                                machineInfos.get(i).setLongitude(chooseMachine.getLongitude());
                                machineInfos.get(i).setChoose(false);
                                machineInfos.get(i).setmPosition(new LatLng(Double.parseDouble(chooseMachine.getLatitude()), Double.parseDouble(chooseMachine.getLongitude())));
                                LogUtils.setLog(mTag, "改变的终端" + machineInfos.get(i).getName() + chooseMachine.getName() + chooseMachine.getId());
                            }
                        }
                        updateCluster();
                        hideProgress();
                    }
                });

            }

            @Override
            public void onFailed(int code, String message) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("编辑失败");
                        hideProgress();
                        clearSelect();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.zone:
                showZonePop();
                break;
            case R.id.state:
                isstate = !isstate;
                if (isstate) {
                    fourstate.setVisibility(View.VISIBLE);
                } else {
                    fourstate.setVisibility(View.GONE);
                }
                break;
            case R.id.showorhide:
                isshoworhide = !isshoworhide;
                if (isshoworhide) {
                    operation.setVisibility(View.VISIBLE);
                    showorhide.setText("隐藏");
                } else {
                    showorhide.setText("显示");
                    operation.setVisibility(View.GONE);
                }
                break;

            case R.id.edit:

                chooseMachine.clear();
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).isChoose()) {
                        chooseMachine.add(machineInfos.get(i));
                    }
                }
                if (chooseMachine.size() == 1) {
                    LogUtils.setLog(mTag, chooseMachine.size() + "选中编辑经纬度的终端" + chooseMachine.get(0).getName());
                    if (tempmachine.getLatitude().length() != 0) {
                        TipDialog tipEditDialog = new TipDialog(mContext, "确定编辑经纬度吗", new TipDialog.OnViewClickListener() {
                            @Override
                            public void onConfirmClick(View v) {
                                showUpProgress();
                                chooseMachine.get(0).setLongitude(tempmachine.getLongitude().substring(0, tempmachine.getLongitude().indexOf(".") + 7));
                                chooseMachine.get(0).setLatitude(tempmachine.getLatitude().substring(0, tempmachine.getLatitude().indexOf(".") + 7));

                                LogUtils.setLog(mTag, chooseMachine.get(0).getName() + "设置的经纬度" + chooseMachine.get(0).getLatitude() + chooseMachine.get(0).getLongitude());
                                try {
                                    savegitude(chooseMachine.get(0));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onCancelClick(View v) {

                            }
                        });
                        tipEditDialog.show();
                    } else {
                        showToast("请先在地图上拾取点，并复制");
                    }
                } else if (chooseMachine.size() == 0) {

                    showToast("请先选一个终端 ");
                } else if (chooseMachine.size() > 1) {
                    LogUtils.setLog(mTag, "只能选一个终端" + chooseMachine.size());
                    showToast("只能选一个终端 ");
                }

                break;
            case R.id.clear_select:
                clearSelect();
                break;
            case R.id.select:
                mUiSettings.setAllGesturesEnabled(false);
                mUiSettings.setScrollGesturesEnabled(false);
                mUiSettings.setZoomGesturesEnabled(true);
                makepolygon = true;
                edit_lat.setChecked(false);
                break;
            case R.id.call:
                chooseMachine.clear();
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).isChoose()) {
                        chooseMachine.add(machineInfos.get(i));
                    }
                }
                for (int i = 0; i < chooseMachine.size(); i++) {
                    VariableConstant.chooseMachine.add(chooseMachine.get(i));
                }
                if (VariableConstant.chooseMachine.size() > 0) {
                    TipDialog tipCallDialog = new TipDialog(mContext, "确定寻呼吗", new TipDialog.OnViewClickListener() {
                        @Override
                        public void onConfirmClick(View v) {
                            MainMethod.startCall(chooseMachine);
                            clearSelect();
                        }

                        @Override
                        public void onCancelClick(View v) {
                        }
                    });
                    tipCallDialog.show();
                } else {
                    showToast("请先选择终端");
                }
                // finish();
                break;
            case R.id.play:
                chooseMachine.clear();
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).isChoose()) {
                        chooseMachine.add(machineInfos.get(i));
                    }
                }
                for (int i = 0; i < chooseMachine.size(); i++) {
                    VariableConstant.chooseMachine.add(chooseMachine.get(i));
                }
                if (chooseMachine.size() > 0) {//在选音乐里面添加到静态变量里面
                    new MusicPop(mContext, "", new ArrayList<MusicInfoModel>(), chooseMachine);
                } else {
                    showToast("请先选择终端");
                }
                break;
            case R.id.speach:
                chooseMachine.clear();
                for (int i = 0; i < machineInfos.size(); i++) {
                    if (machineInfos.get(i).isChoose()) {
                        chooseMachine.add(machineInfos.get(i));
                    }
                }
                if (chooseMachine.size() == 1) {
                    if (chooseMachine.get(0).getType() == 41 || chooseMachine.get(0).getType() == 28 || chooseMachine.get(0).getType() == 17) {
                        TipDialog tipSpeachDialog = new TipDialog(mContext, "确定发起对讲吗", new TipDialog.OnViewClickListener() {
                            @Override
                            public void onConfirmClick(View v) {
                                MainMethod.startSpeech(chooseMachine);
                                clearSelect();
                            }

                            @Override
                            public void onCancelClick(View v) {

                            }
                        });
                        tipSpeachDialog.show();
                    } else {
                        showToast("该类型终端不支持对讲");
                    }
                } else {
                    showToast("只能选择一个终端进行对讲");
                }
                break;
            case R.id.back:
                isdestroy = true;
                stopRefreshMachine();
                finish();
                break;
            case R.id.resets:
                mMapStatus = new MapStatus.Builder().zoom(7).build();
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));
                break;
        }
    }

    private void clearSelect() {
        mClusterManager.clearItems();
        for (int i = 0; i < machineInfos.size(); i++) {
            if (machineInfos.get(i).isChoose()) {
                machineInfos.get(i).setChoose(false);
                MachineInfo s = machineInfos.get(i);
                MachineInfo machineInfoss = new MachineInfo(s.getType(), s.getTaskstate(), s.getDevicestate(), s.getNetstate(), s.getSpeechstate(), s.getVolume(), s.getIsinstancy(), s.getName(), s.getIp(), s.getId(), s.getGroupid(), s.getLongitude(), s.getLatitude());
                machineInfoss.setmPosition(new LatLng(s.getPosition().latitude, s.getPosition().longitude));
                machineInfoss.setChoose(s.isChoose());
                machineInfoss.getBitmapDescriptor();
                LogUtils.setLog(mTag, "改变选中" + s.getName() + machineInfoss.isChoose());
                mClusterManager.addItem(machineInfoss);
            } else {
                mClusterManager.addItem(machineInfos.get(i));
            }
        }
        mClusterManager.cluster();
        chooseline.setText("已选终端：  " + 0);
    }

    private void showZonePop() {
        new ZonePop(mContext, new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                if (o != null) {
                     machineInfos.clear();
                    ArrayList<MachineInfo> machineInfosss = (ArrayList<MachineInfo>) o;
                    if(machineInfosss.size()>0)
                        for (int i = 0; i <machineInfosss.size() ; i++) {
                            isneedshow=false;
                            for (int j = 0; j < terminaltype.length; j++) {
                                if(machineInfosss.get(i).getType()==terminaltype[j]){
                                    isneedshow=true;
                                    LogUtils.setLog(mTag,j+"当前s"+i);
                                    return;

                                }
                            }
                            if(isneedshow) {
                                machineInfos.add(machineInfosss.get(i));
                            }
                        }
                    zonename.setText("分区名称： " + VariableConstant.zonename);
                    LogUtils.setLog(mTag, "改变的终端s" + machineInfos.get(0).getName());
                    LogUtils.setLog(mTag,"分区终端数量"+ machineInfos.size());
                    for (int i = 0; i < machineInfos.size(); i++) {
                        machineInfos.get(i).setChoose(false);
                        LogUtils.setLog(mTag, "改变的终端" + machineInfos.get(i).getName());
                        for (int s = 0; s < allMachineList.size(); s++) {
                            if (machineInfos.get(i).getId() == allMachineList.get(s).getId()) {
                                machineInfos.get(i).setLatitude(allMachineList.get(s).getLatitude());
                                machineInfos.get(i).setLongitude(allMachineList.get(s).getLongitude());
                            }
                        }
                        LogUtils.setLog(mTag, "改变的终端" +machineInfos.get(i).getLongitude()+ machineInfos.get(i).getName()+machineInfos.get(i).getLatitude());
                        machineInfos.get(i).setmPosition(new LatLng(Double.parseDouble(machineInfos.get(i).getLatitude()), Double.parseDouble(machineInfos.get(i).getLongitude())));
                    }
                    updateCluster();
                    refreshText();
                } else {
                    LogUtils.setLog(mTag, "分区无终端");
                }
            }
        });

    }
}


