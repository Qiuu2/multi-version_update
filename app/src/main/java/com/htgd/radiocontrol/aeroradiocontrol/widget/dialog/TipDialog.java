package com.htgd.radiocontrol.aeroradiocontrol.widget.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;


/**
 * 作者：wzq
 * 时间：2018/9/13:16:49
 * 邮箱：535708929
 * 说明：文字提示对话框
 */
public class TipDialog implements View.OnClickListener{
    private final Context mContext;
    private final String tip;
    private View dailogView;
    private OnViewClickListener listener;
    private Dialog dialog;
    private boolean isCancle=true;
    private Button btConfirm;
    private Button btCancle;
    private TextView diaMessage;

    public TipDialog(Context context, String s, OnViewClickListener listener) {//a=0为文字，1为bar，2为日期，3为时间
        super();
        this.mContext = context;
        this.tip = s;
        this.listener = listener;
        initView();
    }
    private void initView() {
        dailogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_tip, null);
        btConfirm = (Button) dailogView.findViewById(R.id.positiveButton1);
        btConfirm.setOnClickListener(this);
        btCancle = (Button) dailogView.findViewById(R.id.positiveButton2);
        btCancle.setOnClickListener(this);
        diaMessage = (TextView) dailogView.findViewById(R.id.dialog_message);
        diaMessage.setText(tip);
        dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dailogView);
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER);
        lp.width =700;
        lp.height =500;
        dialogWindow.setAttributes(lp);
        dialog.setCanceledOnTouchOutside(true);
    }

    public interface OnViewClickListener {
        public void onConfirmClick(View v);
        public void onCancelClick(View v);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.positiveButton1://确定按钮
                if (listener != null) {
                    listener.onConfirmClick(v);
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;
            case R.id.positiveButton2://取消按钮
                if (listener != null) {
                    listener.onCancelClick(v);
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;

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
                dialog.setCanceledOnTouchOutside(true);
            }
        }
        return dialog;
    }

    /***
     * 取消对话框
     */
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }
}
