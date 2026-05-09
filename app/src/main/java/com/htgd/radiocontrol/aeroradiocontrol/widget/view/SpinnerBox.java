package com.htgd.radiocontrol.aeroradiocontrol.widget.view;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;


/**
 * 作者：wzq
 * 时间：2020/12/31:16:53
 * 邮箱：535708929
 * 说明：带spinner的box
 */
public class SpinnerBox extends LinearLayout {
    private   Context mContext;
    private  final View view;
    private    TextView tvRight;
    public String result="1";
    private String mTag = "SpinnerBox";
    private Spinner spinner;
    public GetResultListener listener;
    private String[] itemss;
    private SpinnerAdapter adapter;

    public interface GetResultListener{
       public void getResult(String s);
    }
    public SpinnerBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        view = LayoutInflater.from(context).inflate(R.layout.box_spineer, this);
        spinner = (Spinner) view.findViewById(R.id.preopen_spinner);
        tvRight = (TextView)view.findViewById(R.id.spinner_Right);
    }

    public void setSpinner(String[] items) {
        itemss=items;
        adapter = new SpinnerAdapter(mContext, R.layout.simple_spinner_item, items);
        spinner.setAdapter(adapter);
        getResult(items);

    }

    public void setSpinnerRightText(String s){

        tvRight.setVisibility(View.VISIBLE);
        LogUtils.setLog(mTag,"显示右边文字"+s);
        tvRight.setText(s);
    }

    public void setListener(GetResultListener listener) {
        this.listener = listener;
    }
    public String getResult(final String[] items){

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(mTag, "onItemSelected: "+position);
                result = items[position];
                listener.getResult(items[position]);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
               // tvRight.setVisibility(GONE);

            }
        });
        return result;
    }
    public void setName(String name) {
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setText(name);
    } public void setNameAndColor(String name,int i) {
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setTextColor(i);
        textView.setText(name);
    }
    public void setSpinnerSelected(String s ){
        int index=0;
        for (int i = 0; i <itemss.length ; i++) {
            if(s.equals(itemss[i])){
                index=i;
            }
        }
        spinner.setAdapter(adapter);
          adapter.notifyDataSetChanged();
        spinner.setSelection(index, false);
    }
    private class SpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        String[] items = new String[]{};

        public SpinnerAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
            this.items = objects;
            this.context = context;
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.simple_spinner_item, parent, false);
            }

            TextView tv = (TextView) convertView.findViewById(R.id.name);
            tv.setText(items[position]);
            tv.setTextSize(18);
            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.simple_spinner_item, parent, false);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.name);
            tv.setText(items[position]);
            tv.setTextSize(18);
            return convertView;
        }
    }

}
