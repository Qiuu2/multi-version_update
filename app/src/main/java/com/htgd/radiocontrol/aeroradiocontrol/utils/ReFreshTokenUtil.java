package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.content.Context;

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.GetTokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModelRsp;

import java.io.IOException;
import java.util.HashMap;


/**
 * Created by zongwei on 2017-08-10.
 */
public class ReFreshTokenUtil {
    private Context mcontext;

    private static ReFreshTokenUtil instance;
    public static ReFreshTokenUtil getInstance(Context context){
        if(instance == null){
            synchronized (ReFreshTokenUtil.class){
                instance = new ReFreshTokenUtil(context);
            }
        }
        return  instance;
    }
    public ReFreshTokenUtil(Context context){
        this.mcontext = context;
    }

    interface   onRsponseLister{
        void onSucess();
        void onFailed();
    }

    private void  reFreshToken(final  onRsponseLister lister)throws IOException {
            GetTokenModel model = PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel,GetTokenModel.class,mcontext);
            HashMap<String, String> map =   TransBeanMapUtil.transBeanToMap(model);
            MyRequestBuilder myRequest = new MyRequestBuilder(mcontext);
            myRequest.setUrl(Constant.getAuthorization);
            myRequest.setBodyMap(map);
            RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
                @Override
                public void onSucess(int code, String response) {
                    LogUtils.setLog("the request sucess data back is" + response);
                    TokenModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TokenModelRsp.class);
                    if(responseData!=null && responseData.getData()!=null && responseData.getData().size()>0){
                        TokenModel tokenModel =responseData.getData().get(0);
                        if(tokenModel!=null &&! ValueUtil.isEmpty(tokenModel.getToken())){
                            ServerToken.serverToken =Constant.token_tag + tokenModel.getToken();
                            lister.onSucess();
                        }else {
                            lister.onFailed();
                        }
                    }else{
                        lister.onFailed();
                    }
                }

                @Override
                public void onFailed(int code, String message) {
                    LogUtils.setLog("the request failed data back is" + code + message);
                    lister.onFailed();
                }
            });

        }
}
