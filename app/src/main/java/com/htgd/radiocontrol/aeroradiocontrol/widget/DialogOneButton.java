package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;


/**
 * Created by wzq on 2017-07-12.
 */
public class DialogOneButton implements View.OnClickListener, DialogInterface.OnDismissListener {
    private   String tag;
    private Dialog dialog;
    private String buttonText ;
    private View dailogView;
    private Context mContext;
    private OnViewClickListener listener;
    private TextView  title_tv,main_tv;
    private Button confirm_bt;
    private boolean isCancle = true;
    private EditText etTag;

    public DialogOneButton(Context context) {
        super();
        this.mContext = context;
        init();
    }
    public DialogOneButton(Context context ,OnViewClickListener listener){
        super();
        this.mContext = context;
        this.listener =listener;

        init();
    }

    public void init(){

        dailogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_tag_set, null);
        title_tv = (TextView)dailogView.findViewById(R.id.title);
        main_tv = (TextView)dailogView.findViewById(R.id.message);
        etTag=(EditText)dailogView.findViewById(R.id.tag_et);
        confirm_bt = (Button)dailogView.findViewById(R.id.positiveButton);
        confirm_bt.setOnClickListener(this);
        dialog = new Dialog(mContext);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 去掉背景
        dialog.setContentView(dailogView);
        dialog.setOnDismissListener(this);
        dialog.setCanceledOnTouchOutside(false);
        setDialogUnableDismiss();
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etTag, InputMethodManager.RESULT_SHOWN);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }
    public void setButtonText(String titleText,String mainText){
        main_tv.setText(mainText);
        title_tv.setText(titleText);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case  R.id.positiveButton:
                tag=etTag.getText().toString();
                PreferencesUtil.getInstance().keepField("tag",tag,mContext);
                if (listener != null) {
                    listener.onConfirmClick(v);


                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;
        }
    }

    /**
     *  设置谭框不能消失
     */
    public void setDialogUnableDismiss() {
        dialog.setCancelable(true);
    }
    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    public interface OnViewClickListener {
        /***
         * 响应确定按钮
         *
         * @param v
         */
        public void onConfirmClick(View v);

        /***
         * 对话框消失回调
         */
        public void dialogDismiss();
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

    /***
     * 检查对话框 是否已经显示
     *
     * @return
     */
    public boolean isShowing() {
        if (dialog != null && dialog.isShowing()) {
            return true;
        } else {
            return false;
        }
    }
    /***
     * 取消对话框
     */
    public void cancel() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }
    public void setCancle(boolean isCancle) {
      this.isCancle = isCancle;
    }
}
