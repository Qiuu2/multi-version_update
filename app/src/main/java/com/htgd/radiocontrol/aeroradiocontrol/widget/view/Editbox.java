package com.htgd.radiocontrol.aeroradiocontrol.widget.view;

import android.content.Context;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.constant.ChinaConstants;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：wzq
 * 时间：2020/12/23:17:32
 * 邮箱：535708929
 * 说明：文字输入框,可校验特殊字符
 */
public class Editbox extends LinearLayout {

    private   EditText ed;
    private   TextView names;
    private View view;
    private Context mContext;
    private String name;
    public  EditListener listener;
    private String mTag="Editbox";

    public interface EditListener{
        public void setEditText(String s);
    }
    public void setListener(EditListener listener) {
        LogUtils.setLog(mTag,"回调选择监听");
        this.listener = listener;
    }
    public Editbox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        view = LayoutInflater.from(context).inflate(R.layout.box_edit, this);
          names = (TextView) view.findViewById(R.id.name);
          ed = (EditText) view.findViewById(R.id.edit_taskname);
        ed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSpecialChar(s.toString())) {
                    ToastUtil.showToast(mContext, ChinaConstants.isSpecial);
                }
                name=s.toString();

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public String getTaskname(){
        return name;
    }
    public void setTextName(String s) {

        names.setText(s);

    }
    public void setEdit(String s) {
         ed.setText(s);

    }

    public boolean isSpecialChar(String str) {
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

}
