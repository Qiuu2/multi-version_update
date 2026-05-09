package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;


/**
 * Created by wzw on 2017/12/28.
 */
public class ReUpDialog implements View.OnClickListener, DialogInterface.OnDismissListener {

    private final OnViewClickListener listener;
    private Context  mContext;
    private Dialog dialog;
    private View dialogView;
    private TextView reUp_bt;
    private TextView cancel_bt;
    private boolean isCancle=true;
    private TextView tips;

    public  ReUpDialog(Context context, OnViewClickListener listener) {
            this.mContext = context;
            this.listener = listener;
            initView();
    }
    private void  initView(){
        dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_re_up, null);
        tips = (TextView) dialogView.findViewById(R.id.tips);
        reUp_bt = (TextView) dialogView.findViewById(R.id.re_up_bt);
        reUp_bt.setOnClickListener(this);
        cancel_bt = (TextView) dialogView.findViewById(R.id.cancel_bt);
        cancel_bt.setOnClickListener(this);
        dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(dialogView);
        dialog.setOnDismissListener(this);
    }
      public void setMainText(String title,String confirm,String cancel){
          reUp_bt.setText(confirm);
          cancel_bt.setText(cancel);
          tips.setText(title);
      }

    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.re_up_bt://确定按钮
                if (listener != null) {

                    listener.confirmClickListener(v);
                }
                if (dialog != null&&isCancle) {
                    dialog.cancel();
                }
                break;
            case R.id.cancel_bt:
            if (listener != null) {
                listener.cancelClickListener(v);
            }
            if (dialog != null && isCancle) {
                dialog.cancel();
            }
        }
    }

    /***
     * 显示对话框
     */
    public Dialog show() {

        if (dialog != null) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            if (!((Activity) mContext).isFinishing() && !dialog.isShowing()) {
                dialog.show();
            }
        }
        return dialog;
    }
    public void setCancle(boolean isCancle) {
        this.isCancle = isCancle;
    }
    public void cancel() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }
    public interface OnViewClickListener{
        public void confirmClickListener(View v);
        public void cancelClickListener(View v);
        public void dialogDismiss();
    }
}



