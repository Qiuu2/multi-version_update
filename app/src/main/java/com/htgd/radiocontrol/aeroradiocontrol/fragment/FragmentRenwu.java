package com.htgd.radiocontrol.aeroradiocontrol.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.ImageView;


import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.TaskGuangboActivity;
import com.htgd.radiocontrol.aeroradiocontrol.activity.TaskZuoxiActivity;
import com.htgd.radiocontrol.aeroradiocontrol.activity.TempTTSActivity;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseFragment;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ToastUtil;
import com.htgd.radiocontrol.aeroradiocontrol.widget.ReUpDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by zongwei on 2017-07-12.
 */
public class FragmentRenwu extends BaseFragment implements View.OnClickListener {
    private Context mContext;
    private String message;
    private final String mTAG = "FragmentRenwu";
    private ViewHolder viewHolder = new ViewHolder();
    private TextToSpeech tts;
    private Intent localIntent;
    private CustomDialog dia;

    public static FragmentRenwu newInstance(String message, Context mContext) {
        FragmentRenwu fragmentRenWu = new FragmentRenwu();
        Bundle bundle = new Bundle();
        fragmentRenWu.setArguments(bundle);
        return fragmentRenWu;
    }
    public FragmentRenwu() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = this.getActivity();
        View view = inflater.inflate(R.layout.fragment_renwu_new, container, false);
        if (view != null) {
            initView(view);
        } else {
            LogUtils.setLog(mTAG, "the main view is still null");
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        switch (v.getId()) {
            case R.id.broadcast_zuoxi:
                intent.setClass(mContext, TaskZuoxiActivity.class);
                startActivity(intent);
                break;
            case R.id.broadcast_wenjian:
                bundle.putInt(Constant.BUNDLE_KEY_TYPE, Constant.keyGuangbo);
                intent.setClass(mContext, TaskGuangboActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.broadcast_caibo:
                bundle.putInt(Constant.BUNDLE_KEY_TYPE, Constant.keyCaibo);
                intent.setClass(mContext, TaskGuangboActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.broadcast_gongfang:
                bundle.putInt(Constant.BUNDLE_KEY_TYPE, Constant.keyGongfang);
                intent.setClass(mContext, TaskGuangboActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.broadcast_yuyin:
                bundle.putInt(Constant.BUNDLE_KEY_TYPE, Constant.keyWenZiYuYin);
                intent.setClass(mContext, TaskGuangboActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.broadcast_add:

                tts = new TextToSpeech(mContext, new FragmentRenwu.TTSListener());
                List<TextToSpeech.EngineInfo> list = tts.getEngines();
                for (TextToSpeech.EngineInfo info : list) ;
                int has = -1;
                for (int i = 0; i < list.size(); i++) {
                    String yinqing = list.get(i).toString();
                    LogUtils.setLog("所有引擎" + yinqing);
                    has = yinqing.indexOf("iflytek");
                    if (has != -1) {
                        /*intent.setClass(mContext, TempTTSActivity.class);
                        startActivity(intent);*/
                        break;
                    }
                }
                intent.setClass(mContext, TempTTSActivity.class);
                startActivity(intent);
                /*if (has == -1) {
                    final ReUpDialog dialog = new  ReUpDialog(mContext, new  ReUpDialog.OnViewClickListener() {
                        @Override
                        public void confirmClickListener(View v) {
                            dia = new CustomDialog(mContext);
                            ToastUtil.showToast(mContext,"如安装闪退，由未获取root权限，可以自行到市场下载语记app（自带中文引擎）",3000);
                            dia.show();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    File f = getAssetFile();
                                    // dia.cancel();
                                    smartInstall(f);
                                    dia.cancel();
                                    tts.shutdown();
                                }
                            }).start();
                        }

                        @Override
                        public void cancelClickListener(View v) {

                        }

                        @Override
                        public void dialogDismiss() {

                        }
                    });
                    dialog.show();
                    dialog.setMainText("未安装引擎，无法使用", "安装", "取消");
                }*/

                break;
        }
    }

    public class CustomDialog extends ProgressDialog {
        public CustomDialog(Context context) {
            super(context);
        }

        public CustomDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            init(getContext());
        }

        private void init(Context context) {
            //设置不可取消，点击其他区域不能取消，实际中可以抽出去封装供外包设置
            setCancelable(true);
            setCanceledOnTouchOutside(false);
            setContentView(R.layout.load_dialog);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(params);
        }


        @Override
        public void show() {
            super.show();
        }

    }


    private class ViewHolder {
        private View item_zuoxi;
        private View item_guangbo;
        private View item_caibo;
        private View item_gongfang;
        private View item_yuyin;
        private View item_add;
    }

    private void initView(View view) {
        viewHolder.item_zuoxi = view.findViewById(R.id.broadcast_zuoxi);
        viewHolder.item_zuoxi.setOnClickListener(this);
        /*TextView zuoxi_text = (TextView)viewHolder.item_zuoxi.findViewById(R.id.task_info_text);
        zuoxi_text.setText("作息方案");*/
        ImageView zuoxi_image = (ImageView) viewHolder.item_zuoxi.findViewById(R.id.task_info_image);
        zuoxi_image.setImageResource(R.mipmap.image_zuoxi);


        viewHolder.item_guangbo = view.findViewById(R.id.broadcast_wenjian);
        viewHolder.item_guangbo.setOnClickListener(this);
        ImageView guangbo_image = (ImageView) viewHolder.item_guangbo.findViewById(R.id.task_info_image);
        guangbo_image.setImageResource(R.mipmap.image_wenjian);


        viewHolder.item_caibo = view.findViewById(R.id.broadcast_caibo);
        viewHolder.item_caibo.setOnClickListener(this);
        ImageView caibo_image = (ImageView) viewHolder.item_caibo.findViewById(R.id.task_info_image);
        caibo_image.setImageResource(R.mipmap.image_caibo);

        viewHolder.item_gongfang = view.findViewById(R.id.broadcast_gongfang);
        viewHolder.item_gongfang.setOnClickListener(this);
        ImageView gongfang_image = (ImageView) viewHolder.item_gongfang.findViewById(R.id.task_info_image);
        gongfang_image.setImageResource(R.mipmap.image_gongfang);


        viewHolder.item_yuyin = view.findViewById(R.id.broadcast_yuyin);
        viewHolder.item_yuyin.setOnClickListener(this);
        ImageView yuyin_image = (ImageView) viewHolder.item_yuyin.findViewById(R.id.task_info_image);
        yuyin_image.setImageResource(R.mipmap.texttospeech);


        viewHolder.item_add = view.findViewById(R.id.broadcast_add);
        viewHolder.item_add.setOnClickListener(this);
        ImageView add_image = (ImageView) viewHolder.item_add.findViewById(R.id.add);
        add_image.setImageResource(R.mipmap.add_temp_tts);

    }

    //智能安装
    public void smartInstall(File file) {
        Uri uri = Uri.fromFile(file);

        localIntent = new Intent(Intent.ACTION_VIEW);

        localIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(localIntent);


    }

    public File getAssetFile() {
        File f = null;
        try {

            File local_dir = new File(Constant.LOCAL_DIR);
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("assets/" + "xunfei.apk");
            f = new File(local_dir, "xunfei.apk");
            if (!f.exists()) {
                f.createNewFile();
            }
            LogUtils.setLog("文件创建");
            FileOutputStream fOut = new FileOutputStream(f);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fOut.write(buffer, 0, len);
            }
            fOut.flush();
            is.close();
            fOut.close();

            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    private class TTSListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int i) {

        }
    }
}
