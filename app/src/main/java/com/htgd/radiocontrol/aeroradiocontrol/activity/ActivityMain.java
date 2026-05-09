package com.htgd.radiocontrol.aeroradiocontrol.activity;




import static com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication.activityStack;
import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import com.example.htapplib.HTIntf;
import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.FragmentDianBo;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.FragmentDuiJiang;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.FragmentRenwu;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.FragmentShortcutTask;
import com.htgd.radiocontrol.aeroradiocontrol.fragment.FragmentXunHu;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PermissionUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.CustomScrollViewPager;
import com.htgd.radiocontrol.aeroradiocontrol.widget.viewpager.DepthPageTransformer;
import androidx.fragment.app.FragmentActivity;
/**
 * 作者：wzq
 * 时间：2019/1/16:9:13
 * 邮箱：535708929
 * 说明：主页
 */
public class ActivityMain extends BaseActivity implements View.OnClickListener {
    private FragmentXunHu fgXunhu;
    private FragmentDuiJiang fgDuijiang;
    private FragmentDianBo fgDianbo;
    private FragmentRenwu fgMonitor;
    private FragmentShortcutTask fgSetting;
    private RadioButton rdMenuXunhu, rdMenuDuijiang, rdMenuDianbo, rdMenuMonitor, rdMenuSetting;
    private String mTag = "MainActivity";
    private CustomScrollViewPager mViewPager;
    private ArrayList<Fragment> fList;
    private OnPageChanageListener onPageChanageListener;
    private Context mContext;
    private HTIntf htIntf = new HTIntf();
    private TextView tvTitle;
    private TextView setting;
    private MyFragStateAdapter mMyFragStateAdapter;
    private TextView baiduMap;
    private Notification notification;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }


    @Override
    protected void initSubViews() {
        mContext = this;
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ONE_ID = "com.htgd.radiocontrol.aeroradiocontrol";
        String CHANNEL_ONE_NAME = "Channel One";
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent notificationIntent = new Intent(this, ConnectActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            notification = new Notification.Builder(this , NotificationManager.IMPORTANCE_HIGH+"")
                    .setChannelId(CHANNEL_ONE_ID)
                    .setContentTitle(getText(R.string.ipaddress))
                    .setContentText(getText(R.string.ipaddress))
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentIntent(pendingIntent)
                    .setTicker(getText(R.string.ipaddress))
                    .build();
        }
       // notifyManager.notify(1, notification /*mNotification*/);
        if(notification!=null) {
            LogUtils.setLog(mTag,"notification not null");
            htIntf.setNotification(notification, notification);
        }else{

            LogUtils.setLog(mTag,"notification null");

        }
        LogUtils.setLog(mTag,"WaitingActivity to mainactivity"+activityStack.size());
        new PermissionUtils(mContext).judgePermission("android.permission.RECORD_AUDIO");
        initView();
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {//滑动
                showPageIndex(arg0);
                setPageListener(arg0);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
                System.out.println("wwzq" + arg0);
            }
        });
        mViewPager.setCurrentItem(0);
        mViewPager.setPageTransformer(true, new DepthPageTransformer());
        showPageIndex(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(htIntf!=null){
            htIntf.release();
        }
        MyApplication.getInstances().finishAllActivity();

    }



    private void initView() {
        tvTitle = (TextView) findViewById(R.id.title_text);
        setting = (TextView) findViewById(R.id.title_button);
        baiduMap = (TextView) findViewById(R.id.title_go_back);
        baiduMap.setVisibility(View.VISIBLE);
        baiduMap.setBackgroundResource(R.mipmap.mapop);
        baiduMap.setOnClickListener(this);
        setting.setBackgroundResource(R.drawable.bt_setting_style);

        setting.setOnClickListener(this);
        setting.setVisibility(View.VISIBLE);
        rdMenuXunhu = (RadioButton) findViewById(R.id.rd_menu_xunhu);
        rdMenuXunhu.setOnClickListener(this);
        rdMenuDianbo = (RadioButton) findViewById(R.id.rd_menu_dianbo);
        rdMenuDianbo.setOnClickListener(this);
        rdMenuDuijiang = (RadioButton) findViewById(R.id.rd_menu_duijiang);
        rdMenuDuijiang.setOnClickListener(this);
        rdMenuMonitor = (RadioButton) findViewById(R.id.rd_menu_renwu);
        rdMenuMonitor.setOnClickListener(this);
        rdMenuSetting = (RadioButton) findViewById(R.id.rd_menu_kuaijie);
        rdMenuSetting.setOnClickListener(this);
        reSetRadioImage();
        rdMenuXunhu.setChecked(true);
        mViewPager = (CustomScrollViewPager) findViewById(R.id.my_viewpager);
        mViewPager.setScrollable(false);
        fgXunhu = FragmentXunHu.newInstance("xunhu",mContext);
        fgDuijiang = FragmentDuiJiang.newInstance("duijiang",mContext );
        fgDianbo = FragmentDianBo.newInstance("dianbo",mContext);
        fgMonitor = FragmentRenwu.newInstance("renwu",mContext);
        fgSetting = FragmentShortcutTask.newInstance("kuaijie",mContext);
        fList = new ArrayList<Fragment>();
        fList.add(fgXunhu);
        fList.add(fgDuijiang);
        fList.add(fgDianbo);
        fList.add(fgMonitor);
        fList.add(fgSetting);
        if(mMyFragStateAdapter==null){
            mMyFragStateAdapter=  new MyFragStateAdapter( getSupportFragmentManager());
        }
        mViewPager.setAdapter(mMyFragStateAdapter);
        mViewPager.setOffscreenPageLimit(5);
    }

    class MyFragStateAdapter extends FragmentStatePagerAdapter {

        public MyFragStateAdapter(FragmentManager fm) {
            super(fm);
            // TODO Auto-generated constructor stub
        }




        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return fList.size();
        }

        @Override
        public Fragment getItem(int arg0) {
            // TODO Auto-generated method stub
            return fList.get(arg0);
        }


    }


    //viewpager显示页码
    private void showPageIndex(int index) {
        reSetRadioImage();
        switch (index) {
            case 0:
                rdMenuXunhu.setBackgroundResource(R.mipmap.radio_xunhu_true);
                tvTitle.setText(R.string.call_other);
                break;
            case 1:
                rdMenuDuijiang.setBackgroundResource(R.mipmap.radio_duijiang_true);
                tvTitle.setText(R.string.speech);
                break;
            case 2:
                rdMenuDianbo.setBackgroundResource(R.mipmap.radio_dianbo_true);
                tvTitle.setText(R.string.order_music);
                break;
            case 3:
                rdMenuMonitor.setBackgroundResource(R.mipmap.radio_renwu_true);
                tvTitle.setText(R.string.task);
                break;
            case 4:
                rdMenuSetting.setBackgroundResource(R.mipmap.radio_kuaijie_true);
                tvTitle.setText(R.string.short_task);
                break;
        }
    }

    private void reSetRadioImage() {

        rdMenuXunhu.setBackgroundResource(R.mipmap.radio_xunhu_false);
        rdMenuDuijiang.setBackgroundResource(R.mipmap.radio_duijiang_false);
        rdMenuDianbo.setBackgroundResource(R.mipmap.radio_dianbo_false);
        rdMenuMonitor.setBackgroundResource(R.mipmap.radio_renwu_false);
        rdMenuSetting.setBackgroundResource(R.mipmap.radio_kuaijie_false);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rd_menu_xunhu:
                mViewPager.setCurrentItem(0);
                break;
            case R.id.rd_menu_duijiang:
                mViewPager.setCurrentItem(1);
                break;
            case R.id.rd_menu_dianbo:
                mViewPager.setCurrentItem(2);
                break;
            case R.id.rd_menu_renwu:
                mViewPager.setCurrentItem(3);
                break;
            case R.id.rd_menu_kuaijie:
                mViewPager.setCurrentItem(4);
                break;
            case R.id.title_button:
                startActivity(new Intent(this,SettingActivity.class));
                break;
                case R.id.title_go_back:
                startActivity(new Intent(this,LocationInMapActivity.class));
                break;
        }
    }

    public void setPageListener(int index) {
        try {
            this.onPageChanageListener.OnPageChanage(index);
        } catch (Exception e) {
        }
    }

    public interface OnPageChanageListener {
        void OnPageChanage(int index);
    }


}




