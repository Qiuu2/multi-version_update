package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.activity.TempTTSActivity;


/**
 * Created by matou0289 on 2016/10/14.
 */

public class LoadMoreListView extends ListView implements AbsListView.OnScrollListener {
    private Context mContext;
    private View mHeaderView;
    private int mTotalItemCount;
    private OnLoadMoreListener mLoadMoreListener;
    private boolean mIsLoading=false;
    private boolean isEnd=false;

    public LoadMoreListView(Context context) {
        super(context);
        init(context);
    }

    public LoadMoreListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LoadMoreListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        this.mContext=context;
        mHeaderView= LayoutInflater.from(context).inflate(R.layout.listrefresh_view_header,null);
        setOnScrollListener(this);
    }


    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {


       int firstVisibleIndex= listView.getFirstVisiblePosition();
        if(!mIsLoading&&scrollState == OnScrollListener.SCROLL_STATE_IDLE
                && firstVisibleIndex ==0/*&&isEnd*/){
            mIsLoading=true;
            addHeaderView(mHeaderView);
            if (mLoadMoreListener!=null) {
                mLoadMoreListener.onloadMore();
            }
        }

    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mTotalItemCount=totalItemCount;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener){
        mLoadMoreListener=listener;
    }

    public interface OnLoadMoreListener{
        void onloadMore();
    }
    public void setLoadCompleted(){
        mIsLoading=false;
       removeHeaderView(mHeaderView);
    }


    //重置
    public void loadReset(){
        isEnd = true;

        if ( isEnd){
            ((TempTTSActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //此时已在主线程中，可以更新UI了
                    mHeaderView.findViewById(R.id.end).setVisibility(View.VISIBLE);
                    mHeaderView.findViewById(R.id.still).setVisibility(View.GONE);
                }
            });


        }else{
            ((TempTTSActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //此时已在主线程中，可以更新UI了
                    mHeaderView.findViewById(R.id.end).setVisibility(View.GONE);
                    mHeaderView.findViewById(R.id.still).setVisibility(View.VISIBLE);
                }
            });

        }


    }


}
