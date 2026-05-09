package com.htgd.radiocontrol.aeroradiocontrol.httptask;

 

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.constant.EorroCode;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by wzq on 2018-07-27.
 */
public class RequestManger {
    private static final String mTag = "ReqeustManeger";
    private static  RequestManger instance;
    private static OkHttpClient mOkHttpClient ;

    public RequestManger()   {

        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20000L, TimeUnit.MILLISECONDS)
                .readTimeout(20000L, TimeUnit.MILLISECONDS)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
    }

    public static  RequestManger getInstance() {
        synchronized (  RequestManger.class) {
            if (instance == null) {
                instance = new  RequestManger();
            }
        }
        return instance;
    }

    public static String get(String url) throws IOException {//在子线程中运行，结果反馈到子线程
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.addHeader(Constant.header,  ServerToken.serverToken);
        Request request = builder.build();
        Response response = mOkHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            LogUtils.setLog(mTag+"the request is sucess" + response.code());
        } else {
            LogUtils.setLog(mTag+"the request is failed" + response.code());
        }
        return response.body().toString();
    }

    //到回调函数中监听返回结果
    public static void postHashMap(final MyRequestBuilder requestdata, final onRequestLister lister) throws IOException {

        FormBody.Builder formbody = new FormBody.Builder();
        if (requestdata != null && !ValueUtil.isEmpty(requestdata.getUrl()) && requestdata.getBodyMap() != null) {
            if (requestdata.getBodyMap().size() > 0) {
                for (String key : requestdata.getBodyMap().keySet()) {
                    formbody.add(key, requestdata.getBodyMap().get(key));
                    LogUtils.setLog(mTag+" request key is " + key + "value is " + requestdata.getBodyMap().get(key));
                }
            }


            Request.Builder requestbuilder = new Request.Builder();
            requestbuilder.url(requestdata.getUrl());
            if (requestdata.isNeedToken()) {
                requestbuilder.addHeader(Constant.header,  ServerToken.serverToken);
            }
            Request request = requestbuilder.post(formbody.build()).build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LogUtils.setLog(mTag+" post "  +"shibai"+e.getMessage() );

                    lister.onFailed(0, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonStr = response.body().string();
                    LogUtils.setLog(mTag+" post "+requestdata.getUrl()+" -back data is- " + response.code(), jsonStr);
                    if (response.code() == EorroCode.SUCESS) {
                        lister.onSucess(response.code(), jsonStr);
                        
                    } else {
                        lister.onFailed(response.code(), jsonStr);
                    }
                }
            });
        } else {
            LogUtils.setLog(mTag+"error code is" + EorroCode.InputParmNull + "message is" + EorroCode.InputParmNullMsg);
        }

    }

    public static void get(final String url, final onRequestLister lister) throws IOException {//在子线程中运行，结果反馈到子线程

        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.addHeader(Constant.header,  ServerToken.serverToken);
        Request request = builder.build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.setLog(mTag, e.getMessage());

                lister.onFailed(0, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonStr = response.body().string();
                LogUtils.setLog(mTag+" get "+url+" -back data is- " + response.code(), jsonStr);
                if (response.code() == EorroCode.SUCESS) {
                    lister.onSucess(response.code(), jsonStr);
                } else {
                    lister.onFailed(response.code(), jsonStr);
                }
            }


        });
    }

    // 带回调的put请求
    public static void putHaspMap(final MyRequestBuilder requestdata, final onRequestLister lister) throws IOException {

        FormBody.Builder formBody = new FormBody.Builder();
        if (requestdata != null && !ValueUtil.isEmpty(requestdata.getUrl()) && requestdata.getBodyMap() != null) {
            if (requestdata.getBodyMap().size() > 0) {
                for (String key : requestdata.getBodyMap().keySet()) {
                    formBody.add(key, requestdata.getBodyMap().get(key));
                    LogUtils.setLog(mTag+" request key is - " + key + "value is - " + requestdata.getBodyMap().get(key));
                }
            }
            Request.Builder requestbuilder = new Request.Builder();
            requestbuilder.url(requestdata.getUrl());
            if (requestdata.isNeedToken()) {
                requestbuilder.addHeader(Constant.header,  ServerToken.serverToken);
            }
            requestbuilder.put(formBody.build());

            Request request = requestbuilder.build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LogUtils.setLog(mTag, e.getMessage());
                    lister.onFailed(0, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonStr = response.body().string();
                    LogUtils.setLog(mTag+" put"+requestdata.getUrl()+" -back data is- " + response.code(), jsonStr);
                    if (response.code() == EorroCode.SUCESS) {
                        lister.onSucess(response.code(), jsonStr);
                    } else {
                        lister.onFailed(response.code(), jsonStr);
                    }
                }


            });
        } else {
            LogUtils.setLog(mTag+"error code is" + EorroCode.InputParmNull + "message is" + EorroCode.InputParmNullMsg);
        }
    }

    // 带回调的delete请求
    public static void deleteHaspMap(final MyRequestBuilder requestdata, final onRequestLister lister) throws IOException {

        FormBody.Builder formBody = new FormBody.Builder();
        if (requestdata != null && !ValueUtil.isEmpty(requestdata.getUrl()) && requestdata.getBodyMap() != null) {
            if (requestdata.getBodyMap().size() > 0) {
                for (String key : requestdata.getBodyMap().keySet()) {
                    formBody.add(key, requestdata.getBodyMap().get(key));
                    LogUtils.setLog(mTag+" request key is " + key + "value is" + requestdata.getBodyMap().get(key));
                }
            }
            LogUtils.setLog(mTag+" request url is " + requestdata.getUrl());

            Request.Builder requestbuilder = new Request.Builder();
            requestbuilder.url(requestdata.getUrl());
            if (requestdata.isNeedToken()) {
                requestbuilder.addHeader(Constant.header,  ServerToken.serverToken);
            }
            requestbuilder.delete(formBody.build());
            Request request = requestbuilder.build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LogUtils.setLog(mTag, e.getMessage());
                    lister.onFailed(0, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonStr = response.body().string();
                    LogUtils.setLog(mTag+"delete"+requestdata.getUrl()+" -back data is- " + response.code(), jsonStr);
                    if (response.code() == EorroCode.SUCESS) {
                        lister.onSucess(response.code(), jsonStr);
                    } else {
                        lister.onFailed(response.code(), jsonStr);
                    }
                }


            });
        } else {
            LogUtils.setLog(mTag+"error code is" + EorroCode.InputParmNull + "message is" + EorroCode.InputParmNullMsg);
        }
    }
    //文件上传
    public static void updateFile(final MyRequestBuilder requestdata, final onRequestLister lister) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder();
       builder.setType(MultipartBody.FORM);
        if (requestdata != null && !ValueUtil.isEmpty(requestdata.getUrl()) && requestdata.getBodyMap() != null) {
            LogUtils.setLog(mTag,"文件名"+requestdata.getBodyMap().get("mediafile"));
            File file =  new File(requestdata.getBodyMap().get("mediafile"));
            builder.addFormDataPart("mediafile", file.getName(), RequestBody.create(MultipartBody.FORM, file));
            Request.Builder requestbuilder = new Request.Builder();
           // requestbuilder.url(requestdata.getUrl()+"?taskid="+requestdata.getBodyMap().get("taskid")+"&speed=25&male=42&volume=24&taskname=22&sort=242&content=242");
            requestbuilder.url(requestdata.getUrl() );
            if (requestdata.isNeedToken()) {
                requestbuilder.addHeader(Constant.header, ServerToken.serverToken);
            }
            requestbuilder.post(builder.build());
            Request request = requestbuilder.build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LogUtils.setLog(mTag, "访问接口失败");
                    lister.onFailed(-1, "failed");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonStr = response.body().string();
                    LogUtils.setLog(mTag+" upfile "+requestdata.getUrl()+" -back data is- " + response.code(), jsonStr);
                    if (response.code() == EorroCode.SUCESS) {
                        lister.onSucess(response.code(), jsonStr);

                    } else {
                        lister.onFailed(response.code(), jsonStr);
                    }
                }
            });
        } else {

            LogUtils.setLog(mTag+"error code is" + EorroCode.InputParmNull + "message is" + EorroCode.InputParmNullMsg);
        }
    }

    public interface onRequestListers {
        void  onSucess(int code, Response response);
        void  onFailed(int code, String message);
    }
    public <T> void downLoadFile(String fileUrl, final String destFileDir, final onRequestListers lister) {
        final Request request = new Request.Builder().url(fileUrl).build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.setLog(mTag,"down fail"+request.toString()+"except"+e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                lister.onSucess(response.code(),response );
                LogUtils.setLog(mTag,"down response");
            }

        });
    }
}
