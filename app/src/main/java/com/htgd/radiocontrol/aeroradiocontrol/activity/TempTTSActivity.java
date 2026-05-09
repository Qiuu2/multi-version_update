package com.htgd.radiocontrol.aeroradiocontrol.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidkun.xtablayout.XTabLayout;
import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.IntConstans;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.datautil.TTSContentDao;
import com.htgd.radiocontrol.aeroradiocontrol.datautil.TerminalDao;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.TabFragment;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.ZoneMethod;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusHide;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRecord;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRefreshRecord;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRunTask;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusSetVolume;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusTimeOutUp;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusUpErro;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.ZoneRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusEdit;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusPostTempTask;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusUpDataNewUI;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.FileResponseModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MediaIdRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskIdModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.service.UpFileService;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MediaManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PermissionUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TaskManageUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.DialogAttrSet;
import com.htgd.radiocontrol.aeroradiocontrol.widget.ReUpDialog;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constring.temptag;
import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

/**
 * Created by wzq on 2018/3/15.
 */

public class TempTTSActivity extends BaseActivity implements View.OnClickListener {
    private TempTTSActivity mContext;
    private TTSContentDao mDao;
    private TaskManageUtils taskManageUtils;
    private TextToSpeech tts;

    private Button attr;
    private TempTTSModel model;
    private TextView up_file;
    private EditText content_et;
    private String mTag = "TempTTSActivity";
    private ArrayList<ZoneModel> zoneList;
    private String username;

    private RelativeLayout btnSpeak;
    private boolean btn_vocie = true;
    private LinearLayout del_re;
    private ImageView   img1, volume;
    private LinearLayout recording, record_loading, record_tooshort;
    private int flag = 1;
    private View rcChat_popup;
    private Handler mHandler = new Handler();
    private String voiceName;//录音文件名
    private boolean isShosrt = false;

    private String recordPath=null;
    private String UP_SUCCESS = "true" ;
    private DialogAttrSet dia;

    private String ip;
    private Vibrator vibrator;
    private int frequence = 16000;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private HTIntf htinf;
    private boolean isRecording;
    private int random = new Random().nextInt(10);
    private float mTime = 0;
    private double mean, volumes;
    private ProgressBar upWait;
    private XTabLayout tabLayout;
    private ViewPager viewPager;
    private ArrayList<TabFragment> fragments = new ArrayList<>();
    private MyFragStateAdapter pagerAdapter;
    private int vpCurrentitem;
    private int groupid;
    private ZoneMethod zoneMethod;
    private int terminalsize=0;
    private ArrayList<MachineInfo> zoneMachineList = new ArrayList<>();
    private Button speak_or_text;
    private LinearLayout recordModule;
    private LinearLayout editModule;
    private PermissionUtils permissionUtils;

    @Override
    protected int getLayoutId() {
        this.mContext = this;
        return R.layout.activity_temptts;
    }

    @Override
    protected void initSubViews() {

        mDao = new TTSContentDao(this);
        htinf = new HTIntf();
        ip = PreferencesUtil.getInstance().getField("ipAddress", mContext);
        taskManageUtils = new TaskManageUtils(mContext);
        requestPermission(this);
        initView();
        initTempList( );
        initModel();
        if (!EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().register(this);
        }
        permissionUtils = new PermissionUtils(this);
        permissionUtils.judgePermission("android.permission.WRITE_EXTERNAL_STORAGE");
        permissionUtils.judgePermission("android.permission.READ_EXTERNAL_STORAGE");
        permissionUtils.judgePermission("android.permission.RECORD_AUDIO");
    }
    private void requestPermission(Context context){
        LogUtils.setLog(mTag,"可以写文件");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
               // writeFile();
                LogUtils.setLog(mTag,"可以写文件0");
            } else {
                LogUtils.setLog(mTag,"不可以写文件0");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
              startActivityForResult(intent, 1);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                LogUtils.setLog(mTag,"可以写文件1");
                //writeFile();
            } else {
                LogUtils.setLog(mTag,"不可以写文件1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            LogUtils.setLog(mTag,"可以写文件2");
            //writeFile();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                LogUtils.setLog(mTag,"可以写文件3");
              //  writeFile();
            } else {
                showToast("存储权限获取失败");
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        //judgeEngine();
    }

    private void initModel() {
        username = PreferencesUtil.getInstance().getField(Constant.key_terminalName, mContext);
        model = new TempTTSModel("", "35", "5", null, "1", "0", "任務1",
                "", "1", "", "2", "", "false",
                "0", "", "", "", "", ip);
        model.setUsername(username);
        if (PreferencesUtil.getInstance().getField("mspeed", mContext) != "") {
            model.setSpeed(PreferencesUtil.getInstance().getField("mspeed", mContext));
        }
        if (PreferencesUtil.getInstance().getField("mvolume", mContext) != "") {
            model.setVolume(PreferencesUtil.getInstance().getField("mvolume", mContext));
        }
        if (PreferencesUtil.getInstance().getField("mmale", mContext) != "") {
            model.setMale(PreferencesUtil.getInstance().getField("mmale", mContext));
        }
        if (PreferencesUtil.getInstance().getField("mTimeLength", mContext) != "") {
            model.setTimelength(PreferencesUtil.getInstance().getField("mTimeLength", mContext));
        }
    }
    private void initTempList( ) {
        tabLayout = (XTabLayout)  findViewById(R.id.tablayout);
        viewPager = (ViewPager)  findViewById(R.id.viewpager);

          zoneMethod = new ZoneMethod(mContext);
        try {
            getZone();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void initView() {
        attr = (Button) findViewById(R.id.title_button);
        attr.setVisibility(View.VISIBLE);
        attr.setBackgroundResource(R.drawable.attr_style);
        attr.setOnClickListener(this);

        up_file = (TextView) findViewById(R.id.upfile);
        up_file.setBackgroundResource(R.color.LightGrey);
        speak_or_text = (Button) findViewById(R.id.speak_or_text);
        btnSpeak = (RelativeLayout) findViewById(R.id.btn_speak);
        btnSpeak.setOnClickListener(this);
        up_file.setOnClickListener(this);
        content_et = (EditText) findViewById(R.id.add_tts_et);
        //文字输入框判定




        content_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
               /* if (content_et.getLineCount() == 1) {
                    changRl.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dip2px(mContext,50)));
                }*/
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (content_et.getText().toString().replace(" ", "").length() > 4 && !s.toString().matches(".*\\p{So}.*")) {
                    up_file.setBackgroundResource(R.color.colorBuleDark);
                    up_file.setTextColor(getResources().getColor(R.color.white));
                    up_file.setClickable(true);

                } else if (s.toString().matches(".*\\p{So}.*")) {
                    up_file.setBackgroundResource(R.color.LightGrey);
                    up_file.setClickable(false);
                    showToast("请输入正常字符");
                } else {
                    up_file.setBackgroundResource(R.color.LightGrey);
                    up_file.setClickable(false);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Button back = (Button) findViewById(R.id.title_go_back);
          recordModule= (LinearLayout)findViewById(R.id.record_module);
          editModule= (LinearLayout)findViewById(R.id.edit_module);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(this);
        upWait = (ProgressBar) findViewById(R.id.dang);
        speak_or_text.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btn_vocie) {
                    speak_or_text.setBackgroundResource( R.mipmap.record_m);
                    recordModule.setVisibility(View.GONE);
                    editModule.setVisibility(View.VISIBLE);

                    btn_vocie = false;
                } else {
                    speak_or_text.setBackgroundResource( R.mipmap.edit_m);
                    recordModule.setVisibility(View.VISIBLE);
                    editModule.setVisibility(View.GONE);

                    content_et.setText("");
                 //   changRl.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dip2px(mContext,50)));
                    btn_vocie = true;

                }
            }
        });
        volume = (ImageView) this.findViewById(R.id.volume);
        initRecordPop();
        btnSpeak.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                LogUtils.setLog(mTag, "ontouch" + zoneList.size());
                if (zoneList.size() > 0) {
                    if (zoneList.get(0).getId() != 0) {
                        onTouchEvents(event);
                    } else {
                        showToast("添加分区");
                    }
                } else {
                    showToast("添加分区");
                }
                return true;
            }
        });
    }

    private void initRecordPop() {
        rcChat_popup = this.findViewById(R.id.rcChat_popup);
        img1 = (ImageView) this.findViewById(R.id.img1);
        del_re = (LinearLayout) this.findViewById(R.id.del_re);
        recording = (LinearLayout) this.findViewById(R.id.recording);
        record_loading = (LinearLayout) this.findViewById(R.id.record_loading);
        record_tooshort = (LinearLayout) this.findViewById(R.id.record_tooshort);
    }



    @SuppressLint("MissingPermission")
    private void doShock() {//录音满一分钟震动
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100, 400, 100, 400};   // 停止 开启 停止 开启
        vibrator.vibrate(pattern, -1);           //重复两次上面的pattern 如果只想震动一次，index设为-1
    }



    public boolean onTouchEvents(MotionEvent event) {
        LogUtils.setLog(mTag,"执行触摸动作");
        if (!Environment.getExternalStorageDirectory().exists()) {
            showToast("没有sdcard");
        }
        if (btn_vocie) {
            LogUtils.setLog(mTag,"录音图标");
            int[] location = new int[2];
            btnSpeak.getLocationInWindow(location); // 获取在当前窗口内的绝对坐标
            int btn_rc_Y = location[1];
            int btn_rc_X = location[0];
            int allWidth = Utils.getScreenWidth(mContext);
            int allheigth = Utils.getScreenHeight(mContext);
            if (event.getAction() == MotionEvent.ACTION_DOWN && flag == 1) {
                if (!Environment.getExternalStorageDirectory().exists()) {
                    showToast("没有sdcard");
                    return false;
                }
                LogUtils.setLog(mTag,event.getY()+"===="+event.getX()+"显示录音弹框"+(allWidth - btn_rc_X)+"=====" + (allheigth - btn_rc_Y));
                if (event.getY() > 0 && event.getY()-40 <= allheigth - btn_rc_Y && event.getX() > 0 && event.getX() < allWidth - btn_rc_X) {//判断手势按下的位置在语音录制按钮的范围内
                    rcChat_popup.setVisibility(View.VISIBLE);
                    LogUtils.setLog(mTag,"显示录音弹框");
                    record_loading.setVisibility(View.VISIBLE);
                    recording.setVisibility(View.GONE);
                    record_tooshort.setVisibility(View.GONE);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (!isShosrt) {
                                record_loading.setVisibility(View.GONE);
                                recording.setVisibility(View.VISIBLE);
                            }
                        }
                    }, 300);
                    img1.setVisibility(View.VISIBLE);
                    del_re.setVisibility(View.GONE);
                    int startVoiceT = Integer.parseInt(Utils.getDateTime());
                    voiceName = model.getUsername() + startVoiceT +temptag+ random + ".mp3";
                    //开始录制
                    MediaManager.release();
                    recordPath = Environment.getExternalStorageDirectory() + "/" + voiceName;
                    model.setMediaurl(recordPath);
                    LogUtils.setLog(mTag,"开始录音0"+recordPath);
                    EventBus.getDefault().post(new EventBusRecord(CacheConstants.startRecord));
                    flag = 2;
                }
            }
            if (mTime >= 60.0 && flag == 2) {
                doShock();
                recording.setVisibility(View.GONE);
                this.isRecording = false;
            }
            if ((event.getY() >= -250 && event.getY() <= btnSpeak.getHeight() && event.getX() >= 0
                    && event.getX() <= btnSpeak.getWidth())) {
                img1.setVisibility(View.VISIBLE);
                del_re.setVisibility(View.GONE);
            } else {
                img1.setVisibility(View.GONE);
                del_re.setVisibility(View.VISIBLE);
            }
            if (event.getAction() == MotionEvent.ACTION_UP && flag == 2) {//松开手势时执行录制完成
                if (event.getY() < -250) {//松开时在删除按钮内
                    rcChat_popup.setVisibility(View.GONE);
                    img1.setVisibility(View.VISIBLE);
                    del_re.setVisibility(View.GONE);
                    this.isRecording = false;
                    flag = 1;
                    File file = new File(recordPath);
                    if (file.exists()) {
                        file.delete();
                    }
                } else {//不在删除按钮内
                    recording.setVisibility(View.GONE);
                    this.isRecording = false;
                    flag = 1;
                    if (mTime < 1) {//录制时间太短
                        isShosrt = true;
                        record_loading.setVisibility(View.GONE);
                        recording.setVisibility(View.GONE);
                        record_tooshort.setVisibility(View.VISIBLE);
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                record_tooshort.setVisibility(View.GONE);
                                rcChat_popup.setVisibility(View.GONE);
                                isShosrt = false;
                            }
                        }, 500);
                        return false;
                    }
                    upWait.setVisibility(View.VISIBLE);
                    //上传录音媒体
                    try {
                        if (model.getContent() == "") {//改变录音的长度
                            String a = "";
                                for (int i = 0; i < (int) mTime; i++) {
                                    a = a + "  ";
                                }
                                model.setContent(a);
                                LogUtils.setLog(mTag,"录音内容"+model.getContent());
                        }
                        postTempTask( );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }


    private void updateDisplay(double v) {
        LogUtils.setLog("volume", (int) v + "");
        switch ((int) v) {

            case 0:
            case 1:
                volume.setImageResource(R.mipmap.amp1);
                break;
            case 2:
            case 3:
                volume.setImageResource(R.mipmap.amp2);
                break;
            case 4:
            case 5:
                volume.setImageResource(R.mipmap.amp3);
                break;
            case 6:
            case 7:
                volume.setImageResource(R.mipmap.amp4);
                break;
            case 8:
            case 9:
                volume.setImageResource(R.mipmap.amp5);
                break;
            case 10:

                break;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaManager.release();
        EventBus.getDefault().unregister(this);

    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_button:
                setAttr();
                break;
            case R.id.title_go_back:
                finish();
                break;

            case R.id.upfile:


                if (zoneList.size() > 0) {
                    LogUtils.setLog(mTag, "上传文字前" + zoneList.size());
                    if (zoneList.get(0).getId() != 0) {
                        if (content_et.getText().toString().length() > 6) {
                            if (zoneMachineList.size() > 0 && zoneMachineList.get(0).getName() != null) {
                                try {
                                    model.setContent(content_et.getText().toString());

                                    postTempTask();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                content_et.getText().clear();
                            } else {
                                showToast(ChinaConstants.zonehasnoterminal/*"分区无终端，无法上传"*/);
                            }
                        } else {
                            showToast(ChinaConstants.writemore);
                        }
                    } else {
                        showToast(ChinaConstants.addzone);
                    }
                } else {
                    showToast(ChinaConstants.addzone);
                }
                break;
        }
    }

    //设置属性对话框
    private void setAttr() {
        dia = new DialogAttrSet(mContext, model, new DialogAttrSet.OnViewClickListener() {
            @Override
            public void onConfirmClick(View v) {
            }
            @Override
            public void dialogDismiss() {
            }
        });
        dia.show();
    }

    private void initTabLayout() {//初始化菜单布局
        /*   tabLayout.setupWithViewPager(viewPager, false);//很关键免去无数据显示*/
        FragmentManager childFragmentManager = getSupportFragmentManager();
        pagerAdapter = new  MyFragStateAdapter(childFragmentManager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                vpCurrentitem = viewPager.getCurrentItem();
                LogUtils.setLog(mTag,"fenqu拿终端viewpager");
                groupid = zoneList.get(position).getId();
                LogUtils.setLog(mTag,groupid+"groupid"+viewPager.getCurrentItem());
                try {
                      zoneMachineList = zoneMethod.getZoneTerminal(zoneList.get(viewPager.getCurrentItem()).getId(), zoneMachineList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LogUtils.setLog(mTag,  "分区终端数量"+zoneMachineList.size());
                model.setZonename(zoneList.get(viewPager.getCurrentItem()).getName());
                groupid = zoneList.get(position).getId();
                LogUtils.setLog(mTag,groupid+"groupid"+viewPager.getCurrentItem());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                LogUtils.setLog(mTag,"fenqu viewpager s");
            }
        });

    }
    class MyFragStateAdapter extends FragmentStatePagerAdapter {
        public MyFragStateAdapter(FragmentManager fm) {
            super(fm);

            // TODO Auto-generated constructor stub
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub

            return fragments.size();
        }

        @Override
        public Fragment getItem(int arg0) {
            // TODO Auto-generated method stub
            return fragments.get(arg0);
        }
    }
    //查找分区
    private void getZone() throws IOException {
        RequestManger.getInstance().get(PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.SearchZone, new onRequestLister() {
                    @Override
                    public void onSucess(int code, String response) {
                        ZoneRsp responseData = JsonUtil.getInstance().deSerializeString(response, ZoneRsp.class);
                        if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                            zoneList = responseData.getData();
                            LogUtils.setLog(mTag, "zoneList sizes" + zoneList.size() +"首页id"+ zoneList.get(0).getId());
                            if (zoneList.size() == 1 && zoneList.get(0).getId() == 0) {
                                  zoneList.clear();

                            }
                            ZoneModel zoneModel = new ZoneModel(zoneList.size()+1+"",zoneList.size()+1,3,0,0,"2017-9-5" ,"全部终端","所有终端");
                            zoneList.add(zoneModel);
                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragments.clear();
                                        LogUtils.setLog(mTag, tabLayout.getScrollBarSize() + "tablayout  数量" + zoneList.size());
                                        for (int i = 0; i < zoneList.size(); i++) {
                                            fragments.add(TabFragment.newInstance(zoneList.get(i).getName()));
                                            tabLayout.addTab(tabLayout.newTab()/*.setText(zoneList.get(i).getName())*/);
                                        }
                                        LogUtils.setLog(mTag, tabLayout.getScrollBarSize() + "tablayout  sd数量");
                                        if (pagerAdapter == null) {
                                            initTabLayout();
                                            viewPager.setCurrentItem(0);
                                            LogUtils.setLog(mTag, tabLayout.getScrollBarSize() + "tablayout  第一页");
                                            try {
                                                 zoneMachineList = zoneMethod.getZoneTerminal(zoneList.get(viewPager.getCurrentItem()).getId(), zoneMachineList);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            LogUtils.setLog(mTag, "第一次拿数据" + pagerAdapter.toString());
                                        } else {
                                            viewPager.setAdapter(pagerAdapter);
                                            showToast(getString(R.string.freshzone));
                                            LogUtils.setLog(mTag, "第n次拿数据" + pagerAdapter.toString());
                                        }
                                        LogUtils.setLog(mTag, tabLayout.getScrollBarSize() + "tablayout  ssddd数量");
                                        tabLayout.setupWithViewPager(viewPager);
                                        pagerAdapter.notifyDataSetChanged();

                                        for (int i = 0; i < zoneList.size(); i++) {
                                            tabLayout.getTabAt(i).setText(zoneList.get(i).getName());
                                        }
                                    }
                                });


                        }


                    }

                    @Override
                    public void onFailed(int code, String message) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                            }

                        });

                    }
                });
    }

    //请求终端
    private void postTaskTerminal(String task_id, final String group_id, String terminalid) throws IOException {
        MyRequestBuilder requestdata = new MyRequestBuilder(mContext);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", task_id);
        map.put("groupid", group_id);
        map.put("terminalid",terminalid + "");
        LogUtils.setLog(mTag + "terminalid" + terminalid);
        map.put("area", "255");
        requestdata.setUrl(Constant.postTaskTerminal);
        requestdata.setNeedToken(true);
        requestdata.setBodyMap(map);
        RequestManger.getInstance().postHashMap(requestdata, new onRequestLister() {


            @Override
            public void onSucess(int code, String response) {
                if (terminalsize < zoneMachineList.size()) {
                    if (!(zoneMachineList.get(terminalsize).getId() == htIntf.getterminalid())) {
                        try {
                            postTaskTerminal(model.gettaskid(), groupid + "", zoneMachineList.get(terminalsize).getId() + "");//
                            LogUtils.setLog(mTag, zoneMachineList.size() + "上传第几个终端--" + terminalsize + "终端id" + zoneMachineList.get(terminalsize).getId());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            postTaskTerminal(model.gettaskid(), "0", htIntf.getterminalid() + "");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                      LogUtils.setLog(mTag,"false为文字"+btn_vocie);
                    if (!btn_vocie) {//文字
                        startService();
                    } else {//录音
                        try {
                            postRecordMedia(model.getMediaurl());
                            LogUtils.setLog(mTag, "mediaurl==" + model.getMediaurl());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    LogUtils.setLog(mTag, "terminal up over" + terminalsize + ".." + zoneMachineList.size());
                }
                terminalsize++;
            }

            @Override
            public void onFailed(int code, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        upWait.setVisibility(View.GONE);
                        showToast("上传失败");
                    }
                });
            }
        });
    }

    public void startService() {

        Intent startIntent = new Intent(this, UpFileService.class);
        startIntent.putExtra("model", model);
        startService(startIntent);
    }  //上传单个媒体文件

    private void postRecordMedia(final String file_path) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        File file = new File(file_path);
        map.put("mediafile", file_path);
        map.put("folderid", "6");
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.postFile);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        if (!file.exists()) {
            LogUtils.setLog(mTag + "文件不存在");
        } else {
            LogUtils.setLog(mTag + "文件存在");
        }
        RequestManger.getInstance().updateFile(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                MediaIdRsp responseData = JsonUtil.getInstance().deSerializeString(response, MediaIdRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    FileResponseModel tempModel = (FileResponseModel) responseData.getData().get(0);
                    if (tempModel != null && !ValueUtil.isEmpty(tempModel.getState())) {
                        String mediaid = tempModel.getmediaid();
                        LogUtils.setLog(mTag,"mediaid" + mediaid);
                        try {
                            postSetTaskMusic(model.gettaskid(), mediaid);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            @Override
            public void onFailed(int code, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        upWait.setVisibility(View.GONE);
                        showToast("上传失败");
                    }
                });
            }
        });
    }

    //任务媒体绑定
    private void postSetTaskMusic(String taskid, String mediaid) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", taskid);
        map.put("mediaid", mediaid);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.setTaskMusic);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override

            public void onSucess(int code, String response) {
               // model.setMediaurl(recordPath);
                model.setState(UP_SUCCESS);

                LogUtils.setLog(mTag,"charushujukuqiandenei容"+model.getContent());
                try {
                    EventBus.getDefault().post(new EventBusHide("quchudengdai"));

                    runOrStopTask(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                LogUtils.setLog(mTag,"charushujukuqiandeneirong"+model.getContent());
                mDao.addDate(model);

               // new TerminalDao(mContext).addDate(new MachineInfo( 1,1, 1, 1,   2,   2, 1, 1,   "name“ ,    ”ip“,  2,  ”longitude“,  ”latitude“));
                LogUtils.setLog("gggg");
                model.setContent("");
                model.setTaskname(username + Utils.getDateTime() +temptag+ random);
                mTime = 0;
            }

            @Override
            public void onFailed(int code, String message) {
                LogUtils.setLog("chenggong11" + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        upWait.setVisibility(View.GONE);
                        showToast("上传失败");
                    }
                });
            }
        });
    }

    //提交临时任务
        private void postTempTask() throws IOException {

        model.setTaskname(username + Utils.getDateTime() + temptag + random);//刷新任务名
        model.setPriority(PreferencesUtil.getInstance().getField(Constant.key_user_priority, mContext));
        LogUtils.setLog(mTag,"优先级"+model.getPriority());
        LogUtils.setLog(mTag,"优先级"+model.getPriority());
        model.setCreatetime(Utils.getDate());//刷新时间
        upWait.setVisibility(View.VISIBLE);
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.addTempTask);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        LogUtils.setLog(mTag,"提交临时任务时model参数shichang"+model.getTimelength()+"时长类型"+model.getTimelengthtype()+model.getVolume()+model.getPriority()+model.getCreatetime());
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TaskIdModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TaskIdModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TaskIdModel tmodel = responseData.getData().get(0);
                    if (tmodel != null && !ValueUtil.isEmpty(tmodel.getTaskid())) {
                        model.settaskid(tmodel.getTaskid());
                        LogUtils.setLog(mTag + "task_id" + model.gettaskid());
                      /*  PreferencesUtil.getInstance().keepField(Constant.TASK_ID, model.gettaskid(), mContext);
                        PreferencesUtil.getInstance().keepField(Constant.Task_VOICE, model.getVolume(), mContext);*/
                        try {//
                            postTaskTerminal(model.gettaskid(), "0",htIntf.getterminalid() + "");
                            terminalsize=0;
                    } catch (IOException e) {
                            e.printStackTrace();
                        }
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
            updateToken(mContext, new updatelister() {
                @Override
                public void onSucess() {
                    try {
                        postTempTask();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed() {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            upWait.setVisibility(View.GONE);
                            showToast("上传失败");
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void updateNewUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TempTTSModel tempmodels = new TempTTSModel(model.getContent(), model.getVolume(), model.getSpeed(), model.gettaskid(), model.getMale(), model.getSort(), model.getTaskname(), model.getCreatetime(), model.getTimelength(), model.getPriority(), model.getTimelengthtype(), model.getTerminal(), model.getState(), model.getNumber(), model.getUsername(), model.getZonename(), model.getMediaurl(), model.getTag(), model.getip());
                EventBus.getDefault().post(new EventBusUpDataNewUI(tempmodels));
                LogUtils.setLog(mTag, "当前分区添加内容post"+tempmodels.getZonename());
            }
        });
    }

        private void addTerminalAndProgBeforeRun() {
            Cons.chooseMachine.clear();
            Cons.chooseProgList.clear();
            if (zoneMachineList.size() > 0) {
                for (int i = 0; i < zoneMachineList.size(); i++) {
                    Cons.chooseMachine.add(zoneMachineList.get(i));
                    LogUtils.setLog(mTag, "add terminal for temp" + zoneMachineList.get(i).getName());
                }
            }
            MusicInfoModel musicInfoModel = new MusicInfoModel();
            musicInfoModel.setName(model.getTaskname());
            Cons.chooseProgList.add(musicInfoModel);
        }


    //执行或停止方案
    private synchronized void runOrStopTask(final int state) throws IOException {
        updateNewUI();
         PreferencesUtil.getInstance().keepField(Constring.TASK_ID, model.gettaskid(), mContext);
        PreferencesUtil.getInstance().keepField(Constring.Task_VOICE, model.getVolume(), mContext);
        addTerminalAndProgBeforeRun();
        taskManageUtils.runOrStopTask( model.gettaskid() , state, new TaskManageUtils.onTaskLister() {
            @Override
            public void onChangeSucess() {
                mDao.updateState(model.gettaskid(), "true");
                LogUtils.setLog(mTag + "zhuangtai" + "gaibianchengg");
            }

            @Override
            public void onTheSameStatu() {
                LogUtils.setLog(mTag + "zhuangtai" + "bubian");
            }

            @Override
            public void onRetry() {
                //  reTry();
            }
        });
    }

    //上传完执行任务
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventBusRunTask event) {
        LogUtils.setLog(mTag + "onEvent" + event.getMessage());
        model.setState(UP_SUCCESS);


        try {
            upWait.setVisibility(View.GONE);
            runOrStopTask(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.setContent("");
        LogUtils.setLog(mTag + "zhixingwanwenzishangchuan", model.gettaskid());

    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(EventBusRecord event) {
        LogUtils.setLog(mTag + "开始录制1" +event.getMessage() );
        if (event.getMessage().equals(CacheConstants.startRecord)||event.getMessage().contains("kai")) {//录音参数配置
            isRecording = true;
            LogUtils.setLog(mTag + "开始录制1.1" +recordPath );
            try {

                permissionUtils.judgePermission("android.permission.WRITE_EXTERNAL_STORAGE");
                permissionUtils.judgePermission("android.permission.READ_EXTERNAL_STORAGE");
                FileOutputStream out = new FileOutputStream(recordPath ,true);
                LogUtils.setLog(mTag + "开始录制1.2"  );
                //根据定义好的几个配置，来获取合适的缓冲大小
                int bufferSize = AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding);
                LogUtils.setLog(mTag + "开始录制1.3"  );
                //实例化AudioRecord
                @SuppressLint("MissingPermission") AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, frequence, channelConfig, audioEncoding, bufferSize);
                //开始录制
                LogUtils.setLog(mTag + "开始录制1.4"  );
                record.startRecording();
                LogUtils.setLog(mTag + "开始录制2"  );
                //定义缓冲
                short[] buffer = new short[bufferSize];
                byte[] mp3buff = new byte[htinf.getmp3encodebuffersize(bufferSize, 1)];
                htinf.mp3encodeini(frequence, frequence, 1, 128);
                Timer timer = new Timer();
                LogUtils.setLog(mTag + "开始录制" + mTime);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mTime += 0.3;
                        LogUtils.setLog(mTag + "已录制" + mTime);
                    }
                }, 300, 300);
                while (isRecording) {
                    bufferSize = record.read(buffer, 0, buffer.length);
                    int size = htinf.mp3encodebuffer(buffer, buffer, bufferSize, mp3buff);
                    out.write(mp3buff, 0, size);
                    long v = 0;
                    for (int i = 0; i < bufferSize; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    mean = v / (double) bufferSize;
                    volumes = Math.log10(mean);
                    EventBus.getDefault().post(new EventBusRefreshRecord("shuaxinluyin"));
                }

                try {
                    record.stop();
                    record.release();
                    record = null;
                    timer.cancel();
                    timer = null;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                EventBus.getDefault().post(new EventBusSetVolume("volume reset"));
                htinf.mp3encoderelease();
                htinf.mp3encodebufferflush(mp3buff);
                out.close();
                LogUtils.setLog(mTag + "luzhijieshu");
            } catch (Exception e) {
                LogUtils.setLog(mTag + "output exception"+e.getMessage());
                throw new RuntimeException("文件写入失败，请重试");
                // TODO: handle exception
            }
        } /*else if (event.getMessage().equals("dingshi")) {

        }*/


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusSetVolume event) {

            volume.setImageResource(R.mipmap.amp1);

        }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusRefreshRecord event) {

            LogUtils.setLog(mTag + "shuaxinluyin");
            updateDisplay(volumes);
        }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusHide event) {
        if (event.getMessage().equals("quchudengdai")) {
            upWait.setVisibility(View.GONE);
        }
        if (event.getMessage().equals("shangchuanshibai")) {
            upWait.setVisibility(View.GONE);
            showToast("上传失败");

            //  mDao.updateState(model.gettaskid(),UP_FAILED);
        }
    }   @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusUpErro event) {
        if (event.getMessage().equals("erro is visible")) {
            //changRl.setVisibility(View.INVISIBLE);
        } else if (event.getMessage().equals("erro is disvisible")) {
            //changRl.setVisibility((View.VISIBLE));
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusTimeOutUp event) {
        if (event.getMessage().equals("up file")) {
            recording.setVisibility(View.GONE);
            LogUtils.setLog(mTag + "daoshishangchuan");
            if (model.getContent() == "") {//改变录音的长度
                String a = "";
                for (int i = 0; i < (int) mTime; i++) {
                    a = a + "  ";
                }
                model.setContent(a);
            }
            try {
                postTempTask( );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //点击popwindow的执行后执行任务
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventBusPostTempTask event) {
        TempTTSModel models = event.getMessage();
        if (models.getContent().replace(" ", "") != "") {
            btn_vocie = false;
        } else {
            btn_vocie = true;
        }
        LogUtils.setLog(mTag, "zhixingrenwuqian mediaurl" + models.getMediaurl() + models.getTag() + "-content- " + models.getContent());
        try {
            //TempTTSModel tempmodels = new TempTTSModel(models.getContent(), models.getVolume(), models.getSpeed(), models.gettaskid(), models.getMale(), models.getSort(), models.getTaskname(), models.getCreatetime(), models.getTimelength(), models.getPriority(), models.getTimelengthtype(), models.getTerminal(), models.getState(), models.getNumber(), models.getUsername(), models.getZonename(), models.getMediaurl(), models.getTag(), models.getip());
            setNewModel(models);
            postTempTask();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void setNewModel(TempTTSModel m) {
        model.setContent(m.getContent());
        model.setVolume(m.getVolume());
        model.setSpeed(m.getSpeed());
        model.settaskid(m.gettaskid());
        model.setMale(m.getMale());
        model.setSort(m.getSort());
        model.setTaskname(m.getTaskname());
        model.setCreatetime(m.getCreatetime());
        model.setTimelength(m.getTimelength());
        model.setPriority(m.getPriority());
        model.setTimelengthtype(m.getTimelengthtype());
        model.setTerminal(m.getTerminal());
        model.setState(m.getState());
        model.setNumber(m.getNumber());
        model.setUsername(m.getUsername());
        model.setZonename(m.getZonename());
        model.setMediaurl(m.getMediaurl());
        model.setTag(m.getTag());
        model.setip(m.getip());
    }
    //点击popwindow的使用后编辑文字
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventBusEdit event) {
        String message = event.getMessage();
        if (message.replace(" ", "").length() > 0) {
            if (message.length() > IntConstans.maxcontent) {
                content_et.setText(message.substring(0, IntConstans.maxcontent));
            } else {
                content_et.setText(message);
            }
            if (message.indexOf("X") != -1) {
                //弹出软键盘
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(content_et, InputMethodManager.RESULT_SHOWN);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                content_et.setSelection(message.indexOf("X") + 1);
                if (message.indexOf("XX") == message.indexOf("X")) {
                    content_et.setSelection(message.indexOf("XX") + 2);
                }
                if (message.indexOf("XXX") == message.indexOf("X")) {
                    content_et.setSelection(message.indexOf("XXX") + 3);
                }
                //点击软键盘的确认键触发
                content_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            content_et.setSelection(content_et.getText().toString().indexOf("X") + 1);
                        }
                        return false;
                    }
                });
            }
        }

    }
}
