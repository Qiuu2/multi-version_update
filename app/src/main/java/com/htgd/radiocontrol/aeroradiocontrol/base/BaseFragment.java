package com.htgd.radiocontrol.aeroradiocontrol.base;

/**
 * 作者：wzq
 * 时间：2019/1/17:15:43
 * 邮箱：535708929
 * 说明：
 */

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import android.text.TextUtils;
import android.widget.Toast;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger;
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister;
import com.htgd.radiocontrol.aeroradiocontrol.model.requestModel.GetTokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModel;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TokenModelRsp;
import com.htgd.radiocontrol.aeroradiocontrol.utils.JsonUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.TransBeanMapUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ValueUtil;
import com.htgd.radiocontrol.screanadaption.internal.CustomAdapt;

import java.io.IOException;
import java.util.HashMap;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class BaseFragment extends Fragment implements CustomAdapt {
    private Toast mToast;
    private String mTag = "BaseFragment";
    private Context mContext;


    public BaseFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this.getContext();
    }
    @Override
    public void onResume() {
        super.onResume();
        LogUtils.setLog(mTag,"onresume" );
    }
    //子fragment切换时调用普通和视频寻呼切换
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {//显示
            LogUtils.setLog(mTag, "onHiddenChanged" + "zaicixianshi");
        }else{//隐藏

        }
    }
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        onResume();
    }

    @Override
    public boolean isBaseOnWidth() {
        return true;
    }

    @Override
    public float getSizeInDp() {
        return getResources().getDisplayMetrics().xdpi;
    }


    /*  //在其它fragment返回到当前fragment所做的复原操作
        @Override
        public void setMenuVisibility(boolean menuVisible) {
            super.setMenuVisibility(menuVisible);
            if (menuVisible) {
                getDate();
                LogUtils.setLog(mTag, "setMenuVisibility" + "zaicixianshi");
            }
            if(!menuVisible){
                LogUtils.setLog(mTag, "setMenuVisibility" + "list复原");
                for (int i = 0; i < BaseActivity.userList.size(); i++) {
                    BaseActivity.userList.get(i).setChoose(false);
                }for (int i = 0; i < BaseActivity.musicList.size(); i++) {
                    BaseActivity.musicList.get(i).setChoose(false);
                }
            }
        }
        //子fragment切换时调用
        @Override
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            if (!hidden) {
                getDate();
                LogUtils.setLog(mTag, "onHiddenChanged" + "zaicixianshi");
            }else{
                LogUtils.setLog(mTag, "setMenuVisibility" + "list复原");
                for (int i = 0; i < BaseActivity.userList.size(); i++) {
                    BaseActivity.userList.get(i).setChoose(false);
                } for (int i = 0; i < BaseActivity.musicList.size(); i++) {
                    BaseActivity.musicList.get(i).setChoose(false);
                }
            }
        }*/
    /*public void getDate() {
        userList = BaseActivity.userList;
       // userAdapter.refrech(userList);
    }*/
   /* @Override
    public void onPause() {
        super.onPause();
        LogUtils.setLog(mTag,"basefragment_onpause");
        for (int i = 0; i < BaseActivity.userList.size(); i++) {
            BaseActivity.userList.get(i).setChoose(false);
        }
    }*/
     public interface updatelister {
        void onSucess();

        void onFailed();
    }
    public void updateToken(final Context mContext, final updatelister lister) throws IOException {
        final GetTokenModel model = PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel, GetTokenModel.class, mContext);

        HashMap<String, String> map = TransBeanMapUtil.transBeanToMap(model);
        MyRequestBuilder myRequest = new MyRequestBuilder(mContext);
        myRequest.setUrl(Constant.getAuthorization);
        myRequest.setBodyMap(map);
        RequestManger.getInstance().postHashMap(myRequest, new onRequestLister() {
            @Override
            public void onSucess(int code, String response) {
                TokenModelRsp responseData = JsonUtil.getInstance().deSerializeString(response, TokenModelRsp.class);
                if (responseData != null && responseData.getData() != null && responseData.getData().size() > 0) {
                    TokenModel tokenModel = responseData.getData().get(0);
                    if (tokenModel != null && !ValueUtil.isEmpty(tokenModel.getToken())) {
                        ServerToken.serverToken = Constant.token_tag + tokenModel.getToken();
                        PreferencesUtil.getInstance().keepEntity(Constant.key_tokenString, ServerToken.serverToken, mContext);
                        lister.onSucess();
                    } else {
                        lister.onFailed();
                    }
                } else {
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
    /**
     * 提示消息
     *
     * @param msg 消息值
     */
    public void showMsg(final String msg) {
                if (!TextUtils.isEmpty(msg)) {
                        //  mToast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
                        ToastUtil.showToastOnUIThread((Activity) getContext(), msg);
                }
            }

}

