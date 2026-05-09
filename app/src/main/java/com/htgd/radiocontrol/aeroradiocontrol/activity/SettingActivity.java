package com.htgd.radiocontrol.aeroradiocontrol.activity;

import static com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants.overtimeConnectTime;
import static com.htgd.radiocontrol.aeroradiocontrol.constant.Constant.key_terminalName;
import static com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod.htIntf;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.MyApplication;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Cons;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.VariableConstant;
import com.htgd.radiocontrol.aeroradiocontrol.method.MainMethod;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusReFresh;
import com.htgd.radiocontrol.aeroradiocontrol.utils.CleanCacheUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.FileUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.TipDialog;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.VolumeSetDialog;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.ButtonBox;
import com.htgd.radiocontrol.aeroradiocontrol.widget.view.SpinnerBox;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

public class SettingActivity extends BaseActivity   {
    private SettingActivity mContext;
    private TipDialog tipClearCacheDialog;
    private ButtonBox item_clear,item_exit, item_volume,item_zone;
    private SpinnerBox   item_version,item_ring,item_receive ;
    private String mTag="SettingActivity";
    private TextView version;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void initSubViews() {
      //  LogUtils.setLog(mTag,"经纬度"+ VariableConstant.callmachineList.get(1).getLatitude()+VariableConstant.callmachineList.get(1).getLongitude());
        mContext = this;
        version = (TextView) findViewById(R.id.version);

        version.setText(PreferencesUtil.getInstance().getField( key_terminalName,mContext));
        //服务器版本
        item_version = (SpinnerBox) findViewById(R.id.item_version);
        item_version.setNameAndColor(ChinaConstants.version,R.color.black);
        item_version.setSpinner(getResources().getStringArray(R.array.version_array));
        item_version.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                LogUtils.setLog(mTag,"the byte is"+(byte) (Integer.parseInt(s.substring(s.length()-1)) -1));
                htIntf.setserverversion((byte) (Integer.parseInt(s.substring(s.length()-1)) -1)  );
                LogUtils.setLog(mTag,"版本号 the byte is"+(Integer.parseInt( s.substring(s.length()-1))-2)+"");
                PreferencesUtil.getInstance().keepField(Constring.versionsp,  (Integer.parseInt( s.substring(s.length()-1))-2)+"" , mContext);
            }
        });
        //接听时间
        item_receive = (SpinnerBox) findViewById(R.id.item_receive);
        item_receive.setSpinner(getResources().getStringArray(R.array.auto_time));
        item_receive.setNameAndColor(ChinaConstants.receivetime,R.color.black);
        item_receive.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
                PreferencesUtil.getInstance().keepField(overtimeConnectTime,s.substring(0,s.length()-1),mContext);
            }
        });
       /* //设置铃声
        item_ring = (SpinnerBox) findViewById(R.id.item_ring);
        item_ring.setSpinner(getResources().getStringArray(R.array.sound_name));
        item_ring.setNameAndColor(ChinaConstants.ring,R.color.black);
        item_ring.setListener(new SpinnerBox.GetResultListener() {
            @Override
            public void getResult(String s) {
           PreferencesUtil.getInstance().keepField(CacheConstants.ringtone,s,mContext);
            }
        });*/
        //清除记录
        item_clear = (ButtonBox) findViewById(R.id.item_clear);
        item_clear.setNameAndColor(ChinaConstants.clear,R.color.black);
        item_clear.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipClearCacheDialog = new TipDialog(mContext, "确定清除离线数据吗", new TipDialog.OnViewClickListener() {
                    @Override

                    public void onConfirmClick(View v) {
                        CleanCacheUtils.cleanDatabases(mContext);//清除数据库表
                        CleanCacheUtils.cleanSharedPreference(mContext);
                        CleanCacheUtils.cleanFiles(mContext);
                        CleanCacheUtils.cleanExternalCache(mContext);
                        tipClearCacheDialog.dismiss();
                        new MyApplication().getInstances().setDefault();
                        EventBus.getDefault().post(new EventBusReFresh());
                        deleteFile();
                    }

                    @Override
                    public void onCancelClick(View v) {

                    }
                });
                tipClearCacheDialog.show();
            }
        });
        //分区管理
        item_zone = (ButtonBox) findViewById(R.id.item_zone);
        item_zone.setNameAndColor(ChinaConstants.zone,R.color.black);
        item_zone.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(mContext, ZoneManageActivity.class),1);
            }
        });

        //注销
        item_exit = (ButtonBox) findViewById(R.id.item_exit);
        item_exit.setNameAndColor(ChinaConstants.exit,R.color.black);
        item_exit.setButton(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainMethod.htIntf.release();
                new MyApplication().getInstances().finishAllActivity();
                startActivity(new Intent(mContext, LoginActivity.class));
            }
        });
     initDate();
    }

    private void initDate() {
        LogUtils.setLog(mTag,"版本号:"+"V2."+(Integer.parseInt(PreferencesUtil.getInstance().getField(Constring.versionsp,mContext))+2));
        item_version.setSpinnerSelected("V2."+(Integer.parseInt(PreferencesUtil.getInstance().getField(Constring.versionsp,mContext))+2));
        item_receive.setSpinnerSelected(PreferencesUtil.getInstance().getField(overtimeConnectTime,mContext));
     //   item_ring.setSpinnerSelected(PreferencesUtil.getInstance().getField(CacheConstants.ringtone ,mContext));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

           if(resultCode==0&&requestCode==1){
               if(data!=null) {
                   String result = data.getExtras().getString(CacheConstants.ACTIVITY_RESULT);//得到新Activity 关闭后返回的数据
                   LogUtils.setLog(mTag, "返回了" + result);
                   //  item_zone.setButtonText(result);
               }else{
                   LogUtils.setLog(mTag,"data为空");
               }
           }


    }

    private void setAutoTime() {//设置自动接听时间
        final String[] timelengthlist = mContext.getResources().getStringArray(R.array.auto_time);
        for (int i = 0; i < timelengthlist.length; i++) {
            if (timelengthlist[i].replace("s", "000").equals(PreferencesUtil.getInstance().getField(overtimeConnectTime, mContext))) {
              //  timeSpinner.setSelection(i);
                LogUtils.setLog(mTag, "超时连接选项" + i);
            }
        }
       /* timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferencesUtil.getInstance().keepField(CacheConstants.overtimeConnectTime, timelengthlist[position].replace("s", "000"), mContext);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });*/
    }
    public void deleteFile() {
        File file = new File(Constant.APK_DIR);
        if (file.exists()) {
            new FileUtils().deleteDirectory(file);
        }

    }
}
