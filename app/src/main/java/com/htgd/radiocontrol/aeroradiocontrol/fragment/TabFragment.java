package com.htgd.radiocontrol.aeroradiocontrol.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnLoadMoreListener;
import com.github.jdsjlzx.interfaces.OnNetWorkErrorListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.base.BaseFragment;
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constring;
import com.htgd.radiocontrol.aeroradiocontrol.datautil.TTSContentDao;
import com.htgd.radiocontrol.aeroradiocontrol.model.EventBusReFresh;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusEdit;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusPostTempTask;
import com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel.EventBusUpDataNewUI;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.MediaManager;
import com.htgd.radiocontrol.aeroradiocontrol.utils.PreferencesUtil;
import com.htgd.radiocontrol.aeroradiocontrol.utils.ScreenUtils;
import com.htgd.radiocontrol.aeroradiocontrol.utils.Utils;
import com.htgd.radiocontrol.aeroradiocontrol.widget.MyLRecycView;
import com.htgd.radiocontrol.aeroradiocontrol.widget.dialog.DialogTag;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * 作者：wzq
 * 时间：2021/7/20:15:54
 * 邮箱：535708929
 * 说明：
 */
public class TabFragment extends BaseFragment {
    private String zoneNamed;
    private String mTag = "TabFragment";
    private Context mContext;
    private ArrayList<TempTTSModel> ttsList = new ArrayList<>();
    private TTSContentDao mDao;
    private ArrayList<TempTTSModel> tempList;
    private String ip;
    private Button btnTag;
    private Button btnListen;

    private TempTTSModel model;
    private MyLRecycView list;
    private LinearLayout content;
    private LinearLayout empty;
    private LRecyclerViewAdapter LAdapter;
    private CommonAdapter<TempTTSModel> mAdapter;
    private ArrayList<AnimationDrawable> mAnimationDrawables = new ArrayList<>();
    private AnimationDrawable animationDrawable;
    public  boolean isPlaying;
    private int pos;

    public static TabFragment newInstance(String zoneNamed  ) {
        TabFragment tabFragment = new TabFragment();
        tabFragment.zoneNamed = zoneNamed;
        //tabFragment.model = model;
        tabFragment.mTag = "TabFragment" + zoneNamed;
        Bundle bundle = new Bundle();
        tabFragment.setArguments(bundle);
        return tabFragment;
    }
    public TabFragment() {
        super();
    }


    //在同级别其它fragment返回到当前fragment所做的复原操作
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (!menuVisible) {
            LogUtils.setLog(mTag, zoneNamed + "setMenuVisibility" + "zaicixianshi");
            stopPlay();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {//二级fragment显示
        super.onHiddenChanged(hidden);
        if (!hidden) {
            getData();
            LogUtils.setLog(mTag, "onHiddenChanged" + "zaicixianshi");
        }else{
            stopPlay();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.setLog(mTag, zoneNamed + "onresume");
        if(mDao!=null){
            getData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
        ttsList.clear();

    }


    @Override
    public void onPause() {
        super.onPause();
        stopPlay();
    }
    private void   stopPlay(){
        if(isPlaying) {
            LogUtils.setLog(mTag,"onpause");
            resetAnim(animationDrawable);
            MediaManager.release();
            animationDrawable.stop();
            animationDrawable.selectDrawable(0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.common_list  , null);
        mContext = this.getActivity();

        if (!EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().register(this);
        }
        ip = PreferencesUtil.getInstance().getField("ipAddress", mContext);
        mDao = new TTSContentDao(mContext);

        getData();
        initListView(view);
        return view;
    }

    public void getData() {
        ttsList.clear();
        String currentTime = Utils.getDate();
         LogUtils.setLog(mTag,"取数据的zonename"+zoneNamed);
        ArrayList<TempTTSModel> list = mDao.getNearData(currentTime, zoneNamed, ip);
        for (int i = 0; i < list.size(); i++) {
            ttsList.add(list.get(i));
        }
        LogUtils.setLog(mTag,"刷新数据当前时间"+currentTime+"拿了几条数据"+ttsList.size());
    }

    private void initListView(View v) {
        list = new MyLRecycView(getActivity(), R.color.grey);
        content = (LinearLayout) v.findViewById(R.id.content);
        empty = (LinearLayout) v.findViewById(R.id.empty);
        list.setLayoutManager(new LinearLayoutManager(mContext));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(10,  0, 10, 10);
        list.setLayoutParams(lp);
        renderView();
        LAdapter = new LRecyclerViewAdapter(mAdapter);
        LAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {//单点item
                EventBus.getDefault().post(new EventBusEdit(ttsList.get(position).getContent()));
            }
        });
       /* LAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {//长按item
                showPopWindow(view, position);
                LogUtils.setLog(mTag,"ladapter"+position);
            }
        });*/
        list.setAdapter(LAdapter);
        list.setEmptyView(empty);
        list.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
                updateUI();
            }
        });

        list.setOnNetWorkErrorListener(new OnNetWorkErrorListener() {
            @Override
            public void reload() {
                getData();
                updateUI();
            }
        });
        list.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

                getNewData();
                updateUI();
            }
        });
        content.removeViewAt(0);
        content.addView(list, 0);
        list.setAdapter(LAdapter);
    }

    public void renderView() {
        mAdapter = new CommonAdapter<TempTTSModel>(mContext, R.layout.history_tts_item, ttsList) {
            @Override
            protected void convert(ViewHolder viewHolder, final TempTTSModel model, final int i) {
                if(model!=null){
                viewHolder.setText(R.id.tts_part, model.getContent());
                viewHolder.setText(R.id.time, model.getCreatetime());
                if (model.getTag() != null) {
                    viewHolder.setText(R.id.tag, model.getTag());
                }
                if (model.getContent().replace(" ", "").length() == 0) {

                    viewHolder.setVisible(R.id.singer, true);

                } else {
                    viewHolder.setVisible(R.id.singer, false);
                }
                final LinearLayout ieaLlSinger = viewHolder.getView(R.id.singers);
                viewHolder.setOnLongClickListener(R.id.tts_part, new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        LogUtils.setLog(mTag,"madapter"+i);
                        showPopWindow(view, i-1);
                        return true;
                    }
                });
                LogUtils.setLog(mTag,model.getContent().toString().length()+""+model.getCreatetime());
                viewHolder.setText(R.id.time_length,model.getContent().toString().length()- Constring.blank_record.length()+"s");

                viewHolder.setOnClickListener(R.id.tts_part, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtils.setLog(mTag,"点击的内容"+model.getContent()+"shijian"+model.getCreatetime());
                        if (model.getContent().replace(" ", "").length() == 0) {
                            LogUtils.setLog(mTag,"点击的内容"+model.getContent()+"shijian"+model.getCreatetime());
                            animationDrawable = (AnimationDrawable) ieaLlSinger.getBackground();
                            //重置动画
                            resetAnim(animationDrawable);
                            animationDrawable.start();
                            LogUtils.setLog(mTag, "kaishi play record");
                            //处理点击正在播放的语音时，可以停止；再次点击时重新播放。
                            if (pos == i) {
                                if (isPlaying) {
                                    LogUtils.setLog(mTag, "stop play  record");
                                    isPlaying = false;
                                    MediaManager.release();
                                    animationDrawable.stop();
                                    animationDrawable.selectDrawable(0);//reset
                                    return;
                                } else {
                                    LogUtils.setLog(mTag, "continue playing record");
                                    isPlaying = true;
                                }
                            }
                            //记录当前位置正在播放。
                            pos = i;
                            isPlaying = true;

                            //播放前重置。
                            MediaManager.release();

                            //开始实质播放
                            animationDrawable.start();
                            MediaManager.playSound(model.getMediaurl(),
                                    new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mp) {
                                            animationDrawable.selectDrawable(0);//显示动画第一帧
                                            animationDrawable.stop();
                                            //播放完毕，当前播放索引置为-1。
                                            pos = -1;
                                        }
                                    });
                        }else{
                            EventBus.getDefault().post(new EventBusEdit(model.getContent()));
                        }
                    }

                });
                }else{
                    LogUtils.setLog(mTag,"数据wei空");
                }
            }
        };
    }



    private void resetAnim(AnimationDrawable animationDrawable) {
        if (!mAnimationDrawables.contains(animationDrawable)) {
            mAnimationDrawables.add(animationDrawable);
        }
        for (AnimationDrawable ad : mAnimationDrawables) {
            ad.selectDrawable(0);
            ad.stop();
        }
    }

    private void showPopWindow(View v, final int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_pop_window, null);
        final PopupWindow popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setBackgroundDrawable(new BitmapDrawable());//设置PopupWindow的背景为一个空的Drawable对象，如果不设置这个，那么PopupWindow弹出后就无法退出了
        popWindow.setOutsideTouchable(true);//设置是否点击PopupWindow外退出PopupWindow
        int[] a = new int[2];
        v.getLocationOnScreen(a);
        popWindow.showAtLocation(list, Gravity.CENTER | Gravity.BOTTOM, 0, ScreenUtils.getScreenPixelheight(mContext) - a[1]);
        btnListen = (Button) view.findViewById(R.id.listen);
        btnListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//执行
                popWindow.dismiss();
                model = ttsList.get(position);
                LogUtils.setLog(mTag,"zhixingrenwuqian mediaurl qian"+model.getMediaurl()+model.getTag()+"-content- "
                        +model.getContent()+position+ttsList.get(position).getContent()+ttsList.get(0).getContent());
                EventBus.getDefault().post(new EventBusPostTempTask(model));
                //   postTempTask();
            }
        });
        btnTag = (Button) view.findViewById(R.id.tag);
        btnTag.setOnClickListener(new View.OnClickListener() {//标签
            @Override
            public void onClick(View v) {
                DialogTag tagDia = new DialogTag(mContext, new DialogTag.OnViewClickListener() {
                    @Override
                    public void onConfirmClick(View v) {
                        // model.setTag(PreferencesUtil.getInstance().getField("tag",mContext));
                        ttsList.get(position).setTag(PreferencesUtil.getInstance().getField("tag", mContext));
                        mDao.updateTag(ttsList.get(position).gettaskid(), PreferencesUtil.getInstance().getField("tag", mContext));
                        updateUI();
                    }

                    @Override
                    public void dialogDismiss() {

                    }
                });
                tagDia.show();

                popWindow.dismiss();
            }
        });

    }



    private void getNewData() {//刷新数据
        if (ttsList.size() != 0) {
            LogUtils.setLog(mTag,"拿临时语音数据起点时间"+ttsList.get(ttsList.size() - 1).getCreatetime());
            tempList = mDao.getNearData(ttsList.get(ttsList.size() - 1).getCreatetime(), zoneNamed, ip);
            LogUtils.setLog(mTag,"拿了几条临时语音数据"+tempList.size());
             ttsList.addAll(tempList);
            if (tempList.size() == 0) {
                    /*View footview=LayoutInflater.from(mContext).inflate(R.layout.footer_view,null);
                    LAdapter.addFooterView(footview);*/
              /*  CommonFooter footerView = new CommonFooter(mContext, R.layout.footer_view);
                LAdapter.addFooterView(footerView);
                LogUtils.setLog(mTag, "数据刷到底了");*/
            }
        }
        updateUI();
    }

    public void updateUI() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                LAdapter.notifyDataSetChanged();
                list.refreshComplete(ttsList.size());
                LogUtils.setLog(mTag,"shuaxinshi list size"+ttsList.size());
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusUpDataNewUI event) {//
        LogUtils.setLog(mTag, "EventBusUpDataNewUI" + zoneNamed  );
        LogUtils.setLog(mTag, "EventBusUpDataNewUI" + event.getMessage().getZonename());
        if (zoneNamed.equals(event.getMessage().getZonename())) {
            LogUtils.setLog(mTag, event.getMessage().getZonename()+"当前分区列表添加内容"+event.getMessage().getContent());
           ttsList.add(0, event.getMessage());
            updateUI();
            LogUtils.setLog(mTag,"添加新纪录成功");
        }
    }
   /* @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusPause event) {

    }*/

}