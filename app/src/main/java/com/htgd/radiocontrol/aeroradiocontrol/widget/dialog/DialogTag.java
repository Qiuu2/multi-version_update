package com.htgd.radiocontrol.aeroradiocontrol.widget.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.CacheConstants;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;


/**
 * Created by wzq on 2017-07-12.
 */
public class DialogTag implements View.OnClickListener  {
    private String tag;
    private Dialog dialog;

    private View dailogView;
    private Context mContext;
    private OnViewClickListener listener;
    private Button confirm_bt, cancle_bt;
    private boolean isCancle = true;
    private EditText etTag;
    private TextView titletext;

    public DialogTag(Context context, OnViewClickListener listener) {
        super();
        this.mContext = context;
        this.listener = listener;
        init();
    }

    public void init() {
        dailogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_tag_set, null);
        etTag = (EditText) dailogView.findViewById(R.id.tag_et);
        titletext = (TextView) dailogView.findViewById(R.id.title_text);
        confirm_bt = (Button) dailogView.findViewById(R.id.positiveButton1);
        confirm_bt.setOnClickListener(this);
        cancle_bt = (Button) dailogView.findViewById(R.id.positiveButton2);
        cancle_bt.setOnClickListener(this);
        dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dailogView);
        dialog.setCanceledOnTouchOutside(false);
        setDialogUnableDismiss();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etTag, InputMethodManager.RESULT_SHOWN);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
     public   void setTitleText(String s){
        titletext.setText(s);
     }
     public String getEditText(){
         String text = etTag.getText().toString();
         return  text;
     }
     public void setInitText(String s){
        etTag.setText(s);
     }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.positiveButton1:
                tag = etTag.getText().toString();
                PreferencesUtil.getInstance().keepField(CacheConstants.tag, tag, mContext);
                if (listener != null) {
                    listener.onConfirmClick(v);
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;
            case R.id.positiveButton2:
                if (listener != null) {
                    listener.dialogDismiss();
                }
                if (dialog != null && isCancle) {
                    dialog.cancel();
                }
                break;
        }
    }

    /**
     * 设置谭框不能消失
     */
    public void setDialogUnableDismiss() {
        dialog.setCancelable(true);
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
