package com.htgd.radiocontrol.aeroradiocontrol.httptask;

/**
 * Created by wzq on 2017-07-28.
 */
public interface onRequestLister {
     void  onSucess(int code,String response);
     void  onFailed(int code,String message);
}
