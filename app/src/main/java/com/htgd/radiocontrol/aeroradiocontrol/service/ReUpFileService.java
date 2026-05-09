package com.htgd.radiocontrol.aeroradiocontrol.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.datautil.TTSContentDao;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusFresh;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusHasDown;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusShutService;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusUpErro;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import androidx.annotation.Nullable;

/**
 * Created by wzw on 2018/1/24.
 */

public class ReUpFileService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
   /* private String content;
    private int len = 100;//一段内容
    private ArrayList<String> contentList;
    private ArrayList<String> filenames = new ArrayList<String>(), filepaths = new ArrayList<String>();
    private TextToSpeech mTextToSpeech;
    private TempTTSModel model;
    private String speed, volume;
    private ArrayList<String> hasBuildList = new ArrayList<>();
    private ArrayList<String> hasUpList = new ArrayList<>();
    private TTSContentDao mDao;
    private String TAG = "ReUpFileService";
    private int progre;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.setLog(TAG, "in onCreate");
        EventBus.getDefault().register(this);
        mDao = new TTSContentDao(getApplicationContext());
        if (mTextToSpeech != null) {
            mTextToSpeech.shutdown();
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.setLog(TAG, "in onStartCommand");
        model = (TempTTSModel) intent.getSerializableExtra("model");
        content = model.getContent();
        if (mTextToSpeech == null) {
            mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        if (mTextToSpeech != null && !mTextToSpeech.isSpeaking()) {
                            int supported = mTextToSpeech.setLanguage(Locale.CHINA);
                            mTextToSpeech.setOnUtteranceProgressListener(new ReUpFileService.ttsUtteranceListener());
                            if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported != TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                            } else {
                                buildFile();
                            }
                        }
                    } else {
                        LogUtils.setLog("FAIL");
                    }
                }
            } *//*, "EngineInfo{name=com.iflytek.vflynote}"*//*);
        } else {
            LogUtils.setLog("yinqingbuweikong");
            buildFile();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() executed");
        EventBus.getDefault().unregister(this);
        mTextToSpeech.shutdown();
    }

    public void buildFile() {
        hasBuildList.clear();
        hasUpList.clear();
        filepaths.clear();
        filenames.clear();
        for (int i = 0; i < Integer.parseInt(model.getSort()) + 1; i++) {
            hasBuildList.add("");
            hasUpList.add("");
            filepaths.add("");
            filenames.add("");
        }

        contentList = stringSpilt(content, len);
        if (Integer.parseInt(model.getSort()) + 1 < contentList.size()) {
            model.setNumber(contentList.size() + "");


            speed = "[s" + model.getSpeed() + "]";
            volume = "[v" + model.getSpeed() + "]";
            String fName = model.getTaskname() + (hasBuildList.size() + 1) + ".wav";
            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fName;
            filepaths.add(file_path);
            filenames.add(fName);
            LogUtils.setLog("wenjianshuliang" + filenames.size() + model.getSort());
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "R.id.add_tts_et");
            if (model.getMale().equals("0")) {
                String male = "[m3]";
                int f = mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(Integer.parseInt(model.getSort()) + 1).toString(), map, filepaths.get(Integer.parseInt(model.getSort()) + 1));
                LogUtils.setLog("wenjianfanhui" + f);
            } else if (model.getMale().equals("1")) {
                String male = "[m51]";
                int f = mTextToSpeech.synthesizeToFile(speed + volume + male + contentList.get(Integer.parseInt(model.getSort()) + 1).toString(), map, filepaths.get(Integer.parseInt(model.getSort()) + 1));
                LogUtils.setLog("wenjianfanhui" + f);
            }
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
            model.setContent(contentList.get(hasUpList.size()));
            model.setSort(hasUpList.size() + "");
            try {
                postTempTaskMedia(filenames.get(hasUpList.size()), filepaths.get(hasUpList.size()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(String utteranceId) {
        }
    }

    //上传单个媒体文件
    private void postTempTaskMedia(String name, final String file_path) throws IOException {
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
        RequestManger.getInsatcne().uploadFile(myRequest, name, file, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TempTaskRsp responseData = JsonUtil.getInstance().deSerializeString(response, TempTaskRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TempRspModel tempModel = responseData.getData().get(0);
                    if (tempModel != null && !ValueUtil.isEmpty(tempModel.getState())) {

                        LogUtils.setLog("dbsort" + mDao.alterDate(model.getCreatetime()) + hasUpList.size());
                        hasUpList.add("");
                        String persent = myPercent(hasUpList.size(), contentList.size()).replace(".", "");
                        EventBus.getDefault().post(new EventBusHasDown(persent));
                        mDao.updateSort(model.gettaskid(), hasUpList.size() + "");
                        if (hasBuildList.size() < contentList.size()) {
                            String fName = model.getTaskname() + hasBuildList.size() + ".wav";
                            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fName;
                            filepaths.add(file_path);
                            filenames.add(fName);
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "R.id.erro");
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
                deleteFile(file_path);
                EventBus.getDefault().post(new EventBusUpErro("shangchuanshibai"));

            }
        });
    }

    public static String myPercent(int y, int z) {
        String baifenbi = "";// 接受百分比的值
        double baiy = y * 1.0;
        double baiz = z * 1.0;
        double fen = baiy / baiz;
        DecimalFormat df1 = new DecimalFormat("##.00");
        baifenbi = df1.format(fen);
        return baifenbi;
    }

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        LogUtils.setLog("删除文件");
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(EventBusFresh event) {
        int progress = Integer.parseInt(event.getCount());

    }*/
}
