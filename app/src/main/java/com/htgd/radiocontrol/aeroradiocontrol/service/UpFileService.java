package com.htgd.radiocontrol.aeroradiocontrol.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.RemoteViews;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.datautil.TTSContentDao;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusFresh;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusRunTask;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusShutService;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusString;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempRspModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTaskRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Created by wzw on 2018/1/2.
 */

public class UpFileService extends Service {

    private String content;
    private int len = 100;//一段内容
    private ArrayList<String> contentList;
    private ArrayList<String> filenames, filepaths;
    private TextToSpeech mTextToSpeech;
    private TempTTSModel model;
    private String speed, volume;
    private ArrayList<String> hasBuildList = new ArrayList<>();
    private HashMap<String, ArrayList> messageMap = new HashMap<>();
    private ArrayList<String> hasUpList = new ArrayList<>();


    private NotificationManager notifyManager;
    private TTSContentDao mDao;
    private NotificationCompat.Builder builder;
    private Notification mNotification;
    private RemoteViews contentView;
    private int RECORD=1;
    private String mTag="UpFileService";


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.setLog(TAG, "in onCreate");
       EventBus.getDefault().register(this);
        mDao = new TTSContentDao(getApplicationContext());


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.setLog(TAG, "in onStartCommand");
        model = (TempTTSModel) intent.getSerializableExtra("model");
        content = model.getContent();
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                LogUtils.setLog(TAG, "TextToSpeech onInit");
                if (status == TextToSpeech.SUCCESS) {
                    LogUtils.setLog("su");
                    if (mTextToSpeech != null && !mTextToSpeech.isSpeaking()) {
                        int supported = mTextToSpeech.setLanguage(Locale.CHINA);
                        mTextToSpeech.setOnUtteranceProgressListener(new ttsUtteranceListener());
                        if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported != TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                            LogUtils.setLog("suu");
                        } else {
                            hasUpList.clear();
                            hasBuildList.clear();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    buildFile();
                                }
                            }).start();
                        }
                    }
                } else {
                    LogUtils.setLog(mTag
                            ,"FAIL");
                }
            }
        } );

        return START_STICKY;
    }

    public static final String TAG = "UpFileService";

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() executed");
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        if(mTextToSpeech!=null) {
            mTextToSpeech.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNotification() {
        //获取NotificationManager实例
        notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       //实例化NotificationCompat.Builde并设置相关属性
        builder = new NotificationCompat.Builder(this)

                .setSmallIcon(R.mipmap.logo)
                .setContentTitle("文件上传")
                .setProgress(100, 0, false)
                .setContentText("上传")
                .setTicker("文件开始上传")
                .setLights(getResources().getColor(R.color.colorBlue),200,300);

      /*  int icon = R.mipmap.logo;
        CharSequence tickerText = "文件正在上传";
        long when = System.currentTimeMillis();

        mNotification = new Notification(icon, tickerText, when);
        //实例化NotificationCompat.Builde并设置相关属性
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
       contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.upfile_notification_layout);
        contentView.setTextViewText(R.id.fileName,tickerText);
        mNotification.contentView = contentView;
        contentView.setProgressBar(R.id.progress,100,0,false);*/


        notifyManager.notify(1, builder.build() /*mNotification*/);
    }

    private void cancelNotification(int id) {
        notifyManager.cancel(id);
    }

    public void buildFile() {
        LogUtils.setLog(mTag,"文字转语音的文字"+content);
        contentList = stringSpilt(content, len);
        model.setNumber(contentList.size() + "");
        mDao.addDate(model);//添加数据到表
        filenames = new ArrayList<String>();
        filepaths = new ArrayList<String>();
        speed = "[s" + model.getSpeed() + "]";
        volume = "[v" + model.getSpeed() + "]";
        String fName = model.getTaskname() + 0 + ".wav";
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fName;
        filepaths.add(file_path);
        LogUtils.setLog(mTag,"生成的文件路径"+file_path);
        filenames.add(fName);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "R.id.add_tts_et");
        String male="";
        if (model.getMale().equals("0")) {
              male = "[m3]";
        } else if (model.getMale().equals("1")) {
              male = "[m51]";
        }
        if(CacheConstants.HAS_XUNFEI_ENGINE) {
            mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(0).toString(), map, filepaths.get(0));
        }else{
            mTextToSpeech.synthesizeToFile( contentList.get(0).toString(), map, filepaths.get(0));
        }
    }

    //内容分组
    public ArrayList<String> stringSpilt(String s, int len) {
        ArrayList<String> a = new ArrayList<String>();
        for (int i = 0; i < s.length() / len; i++) {
            a.add(s.substring(i * len, (i + 1) * len));
        }
        a.add(s.substring(s.length() / len * len, s.length()));
        return a;
    }



    //文件生成监听
    private class ttsUtteranceListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
        }
        @Override
        public void onDone(String utteranceId) {
            hasBuildList.add("");
            LogUtils.setLog("生成文件的数量" + hasBuildList.size());
            model.setContent(contentList.get(hasUpList.size()));
            model.setSort(hasUpList.size() + "");
          //  sendNotification();
            try {
                postTempTaskMedia(filepaths.get(hasUpList.size()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onError(String utteranceId) {
        }
    }

    //上传单个媒体文件
    private void postTempTaskMedia(  String file_path) throws IOException {
        HashMap<String, String> maps = TransBeanMapUtil.transBeanToMap(model);
        //  HashMap<String, String> maps = new HashMap<>();
        maps.put("mediafile", file_path);
       /* maps.put("taskid", model.gettaskid());
        maps.put("speed", "6");
        maps.put("male", "0");
        maps.put("volume", "80");
        maps.put("taskname", "dssds");
        maps.put("sort", "0");
        maps.put("content", "dsdd");*/
        LogUtils.setLog(mTag + "上传id--"+maps.get("mediafile"));
        LogUtils.setLog(mTag + "上传id--"+maps.get("taskid"));
        LogUtils.setLog(mTag + "上传id--"+maps.get("speed"));
        LogUtils.setLog(mTag + "上传id--male"+maps.get("male"));
        LogUtils.setLog(mTag + "上传id--volume"+maps.get("volume"));
        LogUtils.setLog(mTag + "上传id--taskname"+maps.get("taskname"));
        LogUtils.setLog(mTag + "上传id--sort"+maps.get("sort"));
        LogUtils.setLog(mTag + "上传id--content"+maps.get("content"));
        LogUtils.setLog(mTag + "上传id--taskid"+model.gettaskid());
        LogUtils.setLog(mTag + "上传id--file_path"+file_path);
        // maps.put("taskid","70010");
        MyRequestBuilder myRequest = new MyRequestBuilder(getApplicationContext());
        myRequest.setUrl(Constant.postTempMediaFile+"?taskid="+model.gettaskid()+"&speed="+model.getSpeed()+"&male="+model.getMale()+"&volume="+model.getVolume()+"&taskname="+model.getTaskname()+"&sort="+model.getSort()+"&content="+model.getContent());
        myRequest.setBodyMap(maps);
        myRequest.setNeedToken(true);
        File file = new File(file_path);
        if (!file.exists()) {
            LogUtils.setLog(mTag + "文件不存在");
        } else {
            LogUtils.setLog(mTag + "文件存在"+file.length());
        }
        RequestManger.getInstance().updateFile(myRequest,  new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TempTaskRsp responseData = JsonUtil.getInstance().deSerializeString(response, TempTaskRsp.class);
                LogUtils.setLog(mTag + "文字转语音媒体上传响应"+responseData.getData().toString());

                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TempRspModel tempModel = responseData.getData().get(0);
                    if (tempModel != null && !ValueUtil.isEmpty(tempModel.getState())) {
                        mDao.updateSort(model.gettaskid(), hasUpList.size() + "");
                        hasUpList.add("");
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        numberFormat.setMaximumFractionDigits(0);
                        String progress = numberFormat.format(((float) hasUpList.size() / (float) contentList.size()) * 100);
                        LogUtils.setLog(mTag + "shangchuanjindu" + progress);
                      //  EventBus.getDefault().post(new EventBusFresh(progress));
                        if (hasUpList.size() == 1) {
                            EventBus.getDefault().post(new EventBusRunTask("kaishizhixing"));
                        }
                        if (hasBuildList.size() < contentList.size()) {
                            String fName = model.getTaskname() + hasBuildList.size() + ".wav";
                            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fName;
                            filepaths.add(file_path);
                            filenames.add(fName);
                            LogUtils.setLog(mTag,"FragmentTemp"+"生成的文件名"+fName);
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "R.id.add_tts_et");
                            if (model.getMale().equals("0")) {
                                String male = "[m3]";
                                mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(hasBuildList.size()).toString(), map, filepaths.get(hasBuildList.size()));
                            } else if (model.getMale().equals("1")) {
                                String male = "[m51]";
                                mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(hasBuildList.size()).toString(), map, filepaths.get(hasBuildList.size()));
                            }
                        } else {
                            for (int i = 0; i < filepaths.size(); i++) {
                                deleteFile(filepaths.get(i));
                            }
                            mDao.updateState(model.gettaskid(), "true");
                            EventBus.getDefault().post(new EventBusShutService("shangchuanwancheng keyi guanbi fuwu"));
                        }
                    }
                }
            }

            @Override
            public void onFailed(int code, String message) {

                LogUtils.setLog(mTag + "文字转语音媒体上传未响应"+message);
               // deleteFile(file_path);
            }
        });
    }
   /* private void postTempTaskMedia(String name, final String file_path) throws IOException {
        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);

        MyRequestBuilder myRequest = new MyRequestBuilder(getApplicationContext());
        myRequest.setUrl(Constant.postTempMediaFile);
        myRequest.setBodyMap(map);
        myRequest.setNeedToken(true);
        File file = new File(file_path);
        if (!file.exists()) {
            LogUtils.setLog(TAG + "文件不存在");
        } else {
            LogUtils.setLog(TAG + "文件存在");
        }
        RequestManger.getInstance().uploadFile(myRequest, name, file, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TempTaskRsp responseData = JsonUtil.getInstance().deSerializeString(response, TempTaskRsp.class);
                LogUtils.setLog(TAG + "dsd文件存在");
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TempRspModel tempModel = responseData.getData().get(0);
                    if (tempModel != null && !ValueUtil.isEmpty(tempModel.getState())) {
                        mDao.updateSort(model.gettaskid(), hasUpList.size() + "");
                        hasUpList.add("");
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        numberFormat.setMaximumFractionDigits(0);
                        String progress = numberFormat.format(((float) hasUpList.size() / (float) contentList.size()) * 100);
                        LogUtils.setLog(TAG+"shangchuanjindu" + progress);
                        EventBus.getDefault().post(new EventBusFresh(progress));
                        if (hasUpList.size() == 1) {
                            EventBus.getDefault().post(new EventBusRunTask( "kaishizhixing"));
                        }
                        if (hasBuildList.size() < contentList.size()) {
                            String fName = model.getTaskname() + hasBuildList.size() + ".wav";
                            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fName;
                            filepaths.add(file_path);
                            filenames.add(fName);
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "R.id.add_tts_et");
                            if (model.getMale().equals("0")) {
                                String male = "[m3]";
                                mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(hasBuildList.size()).toString(), map, filepaths.get(hasBuildList.size()));
                            } else if (model.getMale().equals("1")) {
                                String male = "[m51]";
                                mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(hasBuildList.size()).toString(), map, filepaths.get(hasBuildList.size()));

                            }
                        } else {
                            for (int i = 0; i < filepaths.size(); i++) {
                                deleteFile(filepaths.get(i));
                            }

                            //cancelNotification(1);
                            mDao.updateState(model.gettaskid(), "true");
                            EventBus.getDefault().post(new EventBusShutService("shangchuanwancheng keyi guanbi fuwu"));
                        }
                    }
                }
            }
            @Override
            public void onFailed(int code, String message) {
                deleteFile(file_path);
               // contentView.setTextViewText(R.id.fileName, "上传失败");
               // builder.setContentTitle("上传失败");
                builder.setContentText("上传失败");
                LogUtils.setLog(TAG+"shangchuanjindu" + "sk");
                 EventBus.getDefault().post(new EventBusString("shangchuanshibai"));
                notifyManager.notify(1,  builder.build() *//*mNotification*//*);
            }
        });
    }*/

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        LogUtils.setLog("删除文件");
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusFresh event) {
       /* int progress = Integer.parseInt(event.getCount());
      builder.setProgress(100, progress, false);
       // builder.setContentText("上传" + progress + "%");
        //builder.setContentTitle("文件上传" + progress + "%");
        builder.setContentText("上传" + progress + "%");
        contentView.setTextViewText(R.id.rate, progress + "%");
        contentView.setProgressBar(R.id.progress, 100, progress, false);

        notifyManager.notify(1, builder.build() mNotification);
        if(progress==100){
            builder.setContentText("上传成功" );
            builder.setContentTitle("上传成功" );
            notifyManager.notify(1, builder.build());
        }*/
    }


}

