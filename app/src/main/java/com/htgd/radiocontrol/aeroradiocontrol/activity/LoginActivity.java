package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TimeUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acker.simplezxing.activity.CaptureActivity;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusConnectString;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.GetTokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.service.HTIntfHandler;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PermissionUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ScreenUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


/**
 * Created by wzq on 2019-07-14.
 * 登录界面及修改服务器信息界面活动
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class LoginActivity extends BaseActivity implements View.OnClickListener/*, MKOfflineMapListener*/ {
    private Context mContext = this;
    private GetTokenModel requestModel;
    private final String mTAG = "LoginActivity";
    private final ViewHolder vHolder = new ViewHolder();

    private TextToSpeech tts;
    private boolean showPassword = false;
    private String ipAddress, severNo;
    private CustomDialog dia;
    private static final int REQ_CODE_PERMISSION = 0x1111;

    /*private MKOfflineMap mOffline;
    public MyLocationListenner myListener = new MyLocationListenner();
    private LocationClient mLocClient;
    private ArrayList<MKOLUpdateElement> localMapList;*/
    private boolean isfirstloc;
    private String mTag= "LoginActivity";



    @Override
    protected int getLayoutId() {
        return R.layout.activity_login_new;
    }

    @Override
    protected void initSubViews() {

        LogUtils.setLog(mTag,"WaitingActivity to LoginActivity");
        PermissionUtils  permissionUtils= new PermissionUtils(this);
        permissionUtils.judgePermission(Manifest.permission.READ_PHONE_STATE);
        permissionUtils.judgePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionUtils.judgePermission(Manifest.permission.READ_EXTERNAL_STORAGE);
      //  getThePermission();
        initView();
        LogUtils.setLog(mTag,   ScreenUtils.getScreenPixelWidth(this) + "屏幕宽高" + ScreenUtils.getScreenPixelheight(this) + "屏幕宽度dp" + getResources().getDisplayMetrics().xdpi);


        //getLocalMap();
      /*  try {
            httpsTest();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }

    //获取权限
    private void getThePermission() {
        String[] permissionStr = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_APN_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_APN_SETTINGS,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.DISABLE_KEYGUARD,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(this, permissionStr, 6);
        for (int i = 0; i < permissionStr.length; i++) {
            boolean flag = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            LogUtils.setLog(permissionStr[i] + "is have" + flag);
        }
    }



    /*private void getLocalMap() {
        SDKInitializer.initialize(getApplicationContext());//初始化地图
        mOffline = new MKOfflineMap();
        mOffline.init(this);
        getLocalLocation();
    }*/


   /* //定位本地地址
    private void getLocalLocation() {
        //isfirstloc = PreferencesUtil.getInstance().getField(Constring.isfirstloc, mContext);
        if (isfirstloc) {
            //  PreferencesUtil.getInstance().keepField(Constring.isfirstloc, "false", mContext);
            // 定位初始化
            mLocClient = new LocationClient(this);
            mLocClient.registerLocationListener(myListener);
            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);// 打开gps
            option.setPriority(LocationClientOption.NetWorkFirst);
            option.setCoorType("bd09ll"); // 设置坐标类型
            option.setScanSpan(5000); // 定位时间间隔
            mLocClient.setLocOption(option);
            mLocClient.start();
        } else {
            showToast("地图已下载");
        }
    }*/

    /**
     * 定位SDK监听函数
     */
    /*public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null)
                return;
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            if (longitude > 0 && latitude > 0 && location.getCity() != null) {
                isfirstloc = false;
                LogUtils.setLog(mTAG, String.format("纬度:%f 经度:%f", latitude, longitude));
               *//* PreferencesUtil.getInstance().keepField(Constring.latitude, latitude + "", mContext);
                PreferencesUtil.getInstance().keepField(Constring.longitude, longitude + "", mContext);
                PreferencesUtil.getInstance().keepField(Constring.localcity, location.getCity() + "", mContext);
        *//*
            }
            LogUtils.setLog(mTAG, location.getCityCode() + "本地城市" + location.getCity() + location.getAltitude());
            mLocClient.stop();
        }


    }*/

    @Override
    public void onResume() {
        super.onResume();
        //setEditDefault();

    }


    private void initView() {
        vHolder.see = (ImageView) findViewById(R.id.see);
        vHolder.see.setOnClickListener(this);
        vHolder.ed_id_address = (EditText) findViewById(R.id.login_ip);
        vHolder.ed_username = (EditText) findViewById(R.id.login_uername);
        vHolder.ed_password = (EditText) findViewById(R.id.login_password);
        vHolder.ed_ip_nomber = (EditText) findViewById(R.id.login_ip_nomber);
        vHolder.bt_login = (Button) findViewById(R.id.bt_login);
        vHolder.scan = (Button) findViewById(R.id.scan);
        vHolder.bt_login.setOnClickListener(this);
        vHolder.scan.setOnClickListener(this);
        vHolder.downmap = (Button) findViewById(R.id.down_map);
        vHolder.downmap.setOnClickListener(this);

       /* PermissionUtil permissionUtil = new PermissionUtil(this);
        permissionUtil.checkAccessedLocation();*/
       /* String haslocalmap = PreferencesUtil.getInstance().getField(Constring.haslocalmap, this);
        LogUtils.setLog(mTAG, "有无地图" + haslocalmap);
        if (haslocalmap.equals("false")) {
            vHolder.downmap.setVisibility(View.VISIBLE);
        } else {
            vHolder.downmap.setVisibility(View.GONE);
        }*/
        vHolder.version_sp = (Spinner) findViewById(R.id.version_choose);
        vHolder.ttsInstall = (TextView) findViewById(R.id.install);
        vHolder.ttsInstall.setOnClickListener(this);
        vHolder.set_machine = (TextView) findViewById(R.id.set_machine);
        vHolder.set_machine.setOnClickListener(this);
        vHolder.version_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogUtils.setLog(mTAG, "the byte is" + (byte) (position + 1));
                htIntf.setserverversion((byte) (position + 1));


                LogUtils.setLog(mTag,"版本号 the byte is"+position);
                PreferencesUtil.getInstance().keepField(Constring.versionsp, position + "", mContext);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        setEditDefault();
        showOrHidePassword();
    }

    private void setEditDefault() {//填充输入框
        if (PreferencesUtil.getInstance().getField(Constring.versionsp, mContext) != null &&
                PreferencesUtil.getInstance().getField(Constring.versionsp, mContext) != "" ) {
            // 不再恢复用户上次选择的协议版本 —— 协议版本由下方代码强制锁定为 V2.4。
            vHolder.ed_id_address.setText(PreferencesUtil.getInstance().getField(Constring.ipaddress, mContext));
            vHolder.ed_ip_nomber.setText(PreferencesUtil.getInstance().getField(Constring.ipnumber, mContext));
            vHolder.ed_username.setText(PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext));
            vHolder.ed_password.setText(PreferencesUtil.getInstance().getField(Constring.password, mContext));
        }
        // 强制锁定协议版本为 V2.3 (Spinner position=1, 对应 htIntf.setserverversion 的 byte=2)。
        // 经过 B6(V2.4)/B8(V2.2) 两次现场验证均失败，剩下唯一未尝试的就是 V2.3，
        // 且用户基于现场经验也判断应为 V2.3 —— 现网服务器实际跑的协议版本。
        // 触发上方 OnItemSelectedListener，由其负责 setserverversion + 写入 SharedPreferences。
        // Spinner 本身 UI 已在 layout 中隐藏 (android:visibility="gone")，用户无法切换。
        vHolder.version_sp.setSelection(1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_login:

                String username = vHolder.ed_username.getText().toString();
                String userpwd = vHolder.ed_password.getText().toString();
                ipAddress = vHolder.ed_id_address.getText().toString();
                severNo = vHolder.ed_ip_nomber.getText().toString();
                LogUtils.setLog(mTAG, "mimakuangneirong" + userpwd);
                prelogin(username, userpwd, ipAddress, severNo);
                break;
           /* case R.id.down_map:
                mOffline.getUpdateInfo(0);
                mOffline.start(153);
                // PreferencesUtil.getInstance().keepField(Constring.haslocalmap, "true", mContext);
                updateView();
                break;*/
            case R.id.scan:


                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CODE_PERMISSION);
                } else {
                    startCaptureActivityForResult();
                }
                break;

            case R.id.see:
                showOrHidePassword();
                break;
            /*case R.id.install:
                dia = new LoginActivity.CustomDialog(mContext);
                dia.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File f = getAssetFile();
                        smartInstall(f);
                        dia.cancel();
                    }
                }).start();
                vHolder.ttsInstall.setClickable(false);
                vHolder.ttsInstall.setTextColor(getResources().getColor(R.color.LightGrey));
                vHolder.set_machine.setClickable(true);
                vHolder.set_machine.setTextColor(getResources().getColor(R.color.colorBuleDark));
                break;*/
            case R.id.set_machine:
                startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
                vHolder.set_machine.setClickable(false);
                vHolder.set_machine.setTextColor(getResources().getColor(R.color.LightGrey));
                break;
        }
    }

   /* private void updateView() {
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<MKOLUpdateElement>();
        }

    }*/

    private void startCaptureActivityForResult() {
        Intent intent = new Intent(this, CaptureActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(CaptureActivity.KEY_NEED_BEEP, CaptureActivity.VALUE_BEEP);
        bundle.putBoolean(CaptureActivity.KEY_NEED_VIBRATION, CaptureActivity.VALUE_VIBRATION);
        bundle.putBoolean(CaptureActivity.KEY_NEED_EXPOSURE, CaptureActivity.VALUE_NO_EXPOSURE);
        bundle.putByte(CaptureActivity.KEY_FLASHLIGHT_MODE, CaptureActivity.VALUE_FLASHLIGHT_OFF);
        bundle.putByte(CaptureActivity.KEY_ORIENTATION_MODE, CaptureActivity.VALUE_ORIENTATION_AUTO);
        bundle.putBoolean(CaptureActivity.KEY_SCAN_AREA_FULL_SCREEN, CaptureActivity.VALUE_SCAN_AREA_FULL_SCREEN);
        bundle.putBoolean(CaptureActivity.KEY_NEED_SCAN_HINT_TEXT, CaptureActivity.VALUE_SCAN_HINT_TEXT);
        intent.putExtra(CaptureActivity.EXTRA_SETTING_BUNDLE, bundle);
        startActivityForResult(intent, CaptureActivity.REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // User agree the permission
                    startCaptureActivityForResult();
                } else {
                    // User disagree the permission
                    Toast.makeText(this, "You must agree the camera permission request before you use the code scan function", Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CaptureActivity.REQ_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                       /* String code = data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT);
                        LogUtils.setLog(mTAG, "sd二维码内容" + code);
                        String content = DesUtil.decryptDES(code, "12312312");
                        LogUtils.setLog(mTAG, "二维码内容" + content);
                        LoginModel loginModel = new LoginModel();
                        if (content == null)
                            return;
                        String[] arr = content.split(",");
                        loginModel.setUsername(arr[0]);
                        loginModel.setUserpwd(arr[1]);
                        loginModel.setServerip(arr[2]);
                        loginModel.setServerport(arr[3]);
                        LogUtils.setLog(mTAG, loginModel.getUsername() + "model创建成功" + loginModel.getUserpwd());
                        prelogin(loginModel.getUsername(), loginModel.getUserpwd(), loginModel.getServerip(), loginModel.getServerport());*/
                        break;
                    case RESULT_CANCELED:
                        if (data != null) {
                            // for some reason camera is not working correctly
                            showToast(data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT));
                            // tvResult.setText(data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT));
                        }
                        break;
                }
                break;
        }
    }

   /* @Override
    public void onGetOfflineMapState(int i, int i1) {
        Log.i(mTAG, "" + "onGetOfflineMapState");
        switch (i) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE: {
                Log.i(mTAG, "进度刷新" + "TYPE_DOWNLOAD_UPDATE");
                MKOLUpdateElement update = mOffline.getUpdateInfo(i1);
                // 处理下载进度更新提示
                if (update != null) {
                    // ratio.setText(String.format("%s : %d%%", update.cityName, update.ratio));
                    //  updateView();
                }
            }
            break;
            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.i(mTAG, "新离线安装" + "TYPE_NEW_OFFLINE");
                Log.d("OfflineDemo", String.format("add offlinemap num:%d", i1));
                break;
            case MKOfflineMap.TYPE_VER_UPDATE:
                Log.i(mTAG, "新的离线地图" + "TYPE_VER_UPDATE");
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);

                break;
            default:
                break;
        }
    }*/


    public class CustomDialog extends ProgressDialog {
        public CustomDialog(Context context) {
            super(context);
        }

        public CustomDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            init(getContext());
        }

        private void init(Context context) {
            //设置不可取消，点击其他区域不能取消，实际中可以抽出去封装供外包设置
            setCancelable(true);
            setCanceledOnTouchOutside(false);
            setContentView(R.layout.load_dialog);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(params);
        }

        @Override
        public void show() {
            super.show();
        }

    }

    private void prelogin(String username, String userpwd, String ipAddress, String severNo) {
        String serverAddress = "http://" + ipAddress + ":" + severNo + "/api";
        LogUtils.setLog(mTAG,"地址的内存地址"+Constant.serveraddress);
        Constant.serveraddress = serverAddress;
        LogUtils.setLog(mTAG,"地址的内存地址"+Constant.serveraddress);
        PreferencesUtil.getInstance().keepField(Constring.serverAddress, serverAddress, mContext);
        LogUtils.setLog(mTag + "set serverAddress" + PreferencesUtil.getInstance().getField(Constring.serverAddress, mContext));
        PreferencesUtil.getInstance().keepField(Constring.ipaddress, ipAddress, mContext);
        PreferencesUtil.getInstance().keepField(Constring.ipnumber, severNo, mContext);
        if (!ValueUtil.isEmpty(ipAddress) && !ValueUtil.isEmpty(username) &&
                !ValueUtil.isEmpty(userpwd) && ValueUtil.analyzDns(ipAddress)) {
            requestModel = new GetTokenModel(username, userpwd);
            PreferencesUtil.getInstance().keepField(Constant.key_terminalName, requestModel.getUsername(), mContext);
            PreferencesUtil.getInstance().keepField(Constring.password, requestModel.getUserpwd(), mContext);

            try {
                postServer(requestModel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showToast(getString(R.string.pleasewriteall));
        }
        if (!Utils.checkNetworkEnable(this)) {

            showToast(getString(R.string.nowan));
        }
    }

    public void showOrHidePassword() {
        if (showPassword) {//显示密码
            showPassword = !showPassword;
            vHolder.see.setImageResource(R.mipmap.open_eye);
            vHolder.ed_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            vHolder.ed_password.setSelection(vHolder.ed_password.getText().toString().length());
        } else {//隐藏密码
            showPassword = !showPassword;
            vHolder.see.setImageResource(R.mipmap.close_eye);
            vHolder.ed_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            vHolder.ed_password.setSelection(vHolder.ed_password.getText().toString().length());
        }
    }

    //登陆网络服务器
    private void postServer(final GetTokenModel model) throws IOException {
        LogUtils.setLog(mTAG, model.getUsername() + "denglushimima" + model.getUserpwd());
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.getAuthorization);
        myRequest.setBodyMap(map);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                LogUtils.setLog(mTAG, "post login server" + response);
                TokenModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TokenModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TokenModel tokenModel = responseData.getData().get(0);
                    if (tokenModel != null && !ValueUtil.isEmpty(tokenModel.getToken())) {
                        ServerToken.serverToken = Constant.token_tag + tokenModel.getToken();
                        ServerToken.priority = tokenModel.getPriority();
                        LogUtils.setLog(mTAG, ServerToken.priority + "sd" + tokenModel.getPriority());
                        PreferencesUtil.getInstance().keepEntity(Constant.key_tokenModel, model, mContext);
                        PreferencesUtil.getInstance().keepField(Constant.key_terminalName, model.getUsername(), mContext);
                        PreferencesUtil.getInstance().keepEntity(Constant.key_tokenString, ServerToken.serverToken, mContext);
                        PreferencesUtil.getInstance().keepField(Constant.key_user_priority, ServerToken.priority, mContext);
                        getServerNomber();
                    } else {
                        showToast(R.string.sever_exception);
                    }
                } else {
                    showToast(R.string.sever_exception);
                }
            }

            @Override
            public void onFailed(int code, String message) {
                if (code == 401) {
                    showToast(R.string.scan_account);
                }
                LogUtils.setLog(mTAG, "post login server" + code + message);
                //showToast(R.string.conntect_time_out+code);
            }
        });
    }

    private class ViewHolder {
        private EditText ed_username, ed_password, ed_id_address, ed_ip_nomber;
        private Button bt_login, scan, downmap;
        private Spinner version_sp;
        private ImageView see;
        private TextView ttsInstall, set_machine;
    }





    private void initSDKAndStartMain() {
        htIntf = htIntf;
        HTIntfHandler htIntfHandler = new HTIntfHandler(mContext);
        LogUtils.setLog(mTAG, "sdkversion" + Constant.SDK_SERVER_NOMBER);
        LogUtils.setLog(mTAG, "chushihuasdkmima" + requestModel.getUserpwd());
        htIntfHandler.initCallBack(PreferencesUtil.getInstance().getField(Constring.ipaddress, mContext), Constant.SDK_SERVER_NOMBER, requestModel.getUsername(), requestModel.getUserpwd(), Constant.VOICE_NOMBER_INIT, requestModel.getUsername(), (byte) 41);
    }

    //获取功能服务器端口码
    public void getServerNomber() {
        TaskManageUtils manageUtils = new TaskManageUtils(mContext);
        try {
            manageUtils.getServeNomber(new TaskManageUtils.onGetLister() {
                @Override
                public void onGetSucess() {
                    initSDKAndStartMain();
                }

                @Override
                public void onGetFaid() {
                    showToast("功能服务器未连接，端口获取未成功");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

    }

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        LogUtils.setLog("按了返回键");
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            LogUtils.setLog("退出");

            MyApplication.getInstances().appExit(mContext);

            finishAffinity();
        }
        return false;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusConnectString event) {
        if (event.getMessage().equals("") || event.getMessage() == "1") {
            if (currentActivity.contains("Login")) {
                Intent intent = new Intent();
                intent.setClass(mContext, ActivityMain.class);
                startActivity(intent);
            }
        }
    }
}
