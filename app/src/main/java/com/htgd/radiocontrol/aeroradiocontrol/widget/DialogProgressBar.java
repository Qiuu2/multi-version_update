package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;


/**
 * Created by zongwei on 2017-07-15.
 */
public class DialogProgressBar implements DialogInterface.OnDismissListener  {

    private ProgressBar progressBar;
    private TextView  message_tv;
    private Context mContext;
    private View view;
    public DialogProgressBar(Context context){
        this.mContext =context;
    }

    private void init(){
        view = LayoutInflater.from(mContext).inflate(R.layout.dialog_progress_bar,null);
        progressBar = (ProgressBar)view.findViewById(R.id.dialog_progress);
        message_tv = (TextView)view.findViewById(R.id.dialog_text);
    }

    public void setProgressBarImage(int imageId){
        progressBar.setBackgroundResource(imageId);
    }
    public void setDialogMessage(String message){
        message_tv.setText(message);
    }
    @Override
    public void onDismiss(DialogInterface dialog) {

    }

}
