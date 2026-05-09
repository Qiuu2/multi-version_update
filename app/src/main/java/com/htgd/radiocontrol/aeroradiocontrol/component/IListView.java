package com.htgd.radiocontrol.aeroradiocontrol.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;


import com.htgd.radiocontrol.aeroradiocontrol.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by wzw on 2017/12/13.
 */
public class IListView extends ListView {
    private LayoutInflater inflater;
    private int xStart;
    private int xEnd;
    private int yStart;
    private int yEnd;
    private boolean isDownSliding = false;
    private boolean isLoading = false;
    private int mCurrentViewPosition;
    private List< SwipeItemLayout> mCurrentViews;
    // 滑动的最小距离
    private int touchSlop;
   IRefreshAdapter adapter;
    private final View mHeaderView;
    private  SwipeItemLayout cur_item;
    private DeleteItemListener deleteItemListener;
    private ReuseItemListener reuseItemListener;
    private ChangeItemListener changeItemListener;
    private OnLoadNextPageListener mOnLoadListener;
    private OnAddDataListener addDataListener;

    Context context;
    ImageView imageView;

    public static int STARTINDEX = 1;
    public static int PAGECOUNT = 2;

    /**
     * 初始化操作
     */
    public IListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        inflater = LayoutInflater.from(context);
        mCurrentViews = new ArrayList< SwipeItemLayout>();
        // 表示控件移动的最小距离，手移动的距离大于这个距离才能拖动控件
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop() *20;//以前是5滑动好快
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        imageView = new ImageView(context);
        imageView.setImageResource(R.mipmap.platform_component_listview_nodata);
        imageView.setLayoutParams(params);

        imageView.setVisibility(View.GONE);
        STARTINDEX = 1;//初始化，否则公用页面时会累加
         this.setOnLoadNextPageListener(new OnLoadNextPageListener() {
            @Override
            public void onLoad() {
              Log.i("上拉加载", "onLoad");
                IListView.STARTINDEX += IListView.PAGECOUNT;
                Log.i("上拉加载", "IListView.STARTINDEX=" + IListView.STARTINDEX);
                addDataListener.onAddData();
            }
        });
        mHeaderView = View.inflate(context, R.layout.listrefresh_view_header, null);
    }
    public void addEemtyView() {
        ViewGroup parentViewGroup = (ViewGroup) this.getParent();
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        // 图片居中显示，支持 list父容器是RelativeLayout，LinearLayout
        if (parentViewGroup instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView.setLayoutParams(layoutParams);
        } else if (parentViewGroup instanceof LinearLayout) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            imageView.setLayoutParams(layoutParams);
        }
        int index = parentViewGroup.indexOfChild(this);
        parentViewGroup.removeView(imageView);
        parentViewGroup.addView(imageView, index);
        this.setEmptyView(imageView);
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d("ListView", "dispatchTouchEvent");
        super.dispatchTouchEvent(event);
        if (isLoading) {
            return false;
        }
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                xStart = x;
                yStart = y;
                mCurrentViewPosition = pointToPosition(xStart, yStart);
                if (this.deleteItemListener != null) {
                    cur_item = ( SwipeItemLayout) getChildAt(mCurrentViewPosition - getFirstVisiblePosition());
                    if (cur_item != null && !mCurrentViews.contains(cur_item)) {
                        mCurrentViews.add(cur_item);
                    }
                    //关闭其他不相干的左右滑菜单
                    for ( SwipeItemLayout swipeItemLayout : mCurrentViews) {
                        if (swipeItemLayout != cur_item) {
                            if (swipeItemLayout.isOpen()) {
                                swipeItemLayout.smoothCloseMenu();
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                xEnd = x;
                yEnd = y;
                int offsetX = xStart - xEnd;
                int offsetY = yStart - yEnd;
                 if (yEnd > yStart && Math.abs(offsetY) > touchSlop && Math.abs(offsetX) < touchSlop   ) {
                    isDownSliding = true;

                } else   {
                    //事件分发
                    if (cur_item != null) {

                        cur_item.setHasOnDeleteListener(deleteItemListener != null);
                        cur_item.dispatchTouchEvent(event);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                if (isDownSliding  ) {
                } else {
                    //事件分发
                    if (cur_item != null)
                        cur_item.dispatchTouchEvent(event);
                }

                break;
        }
        return true;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int offsetY = yStart - yEnd;
                if (isDownSliding) {
                    if (this.getHeaderViewsCount() == 0) {
                        // 显示布局
                        this.addHeaderView(mHeaderView, null, false);//header就不能点
                    } else {
                        this.layout(getLeft(), mHeaderView.getTop() - offsetY / 5, getRight(), getBottom() - offsetY / 5);
                    }
                }
                break;
          case MotionEvent.ACTION_UP:

                if (isDownSliding) {
                        if (!isLoading) {
                            isLoading = true;
                          mOnLoadListener.onLoad();
                        }
                }
                break;
        }
        return true;
    }
    public void loadingFinish() {
        if (adapter != null && adapter.getCount() == 0) {
           this.addEemtyView();
        }
        if (isDownSliding) {
            this.removeHeaderView(mHeaderView);
        }

        isLoading = false;
        isDownSliding = false;

    }
    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        this.adapter = ( IRefreshAdapter) adapter;
        this.adapter.setiListView(this);
    }
    public void setDeleteItemListener(DeleteItemListener listener) {
        deleteItemListener = listener;
    }
    public void setReuseItemListener(ReuseItemListener listener) {
        reuseItemListener= listener;
    }
    public void setChangeItemListener(ChangeItemListener listener) {
        changeItemListener= listener;
    }
    public DeleteItemListener getDeleteItemListener() {
        return deleteItemListener;
    }
    public ReuseItemListener getReuseItemListener () {
        return reuseItemListener;
    }
    public ChangeItemListener getChangeItemListener() {
        return changeItemListener;
    }
    public interface DeleteItemListener {
        void DeleteItem(int position);

    }
    public interface ReuseItemListener {
        void ReuseItem(int position);

    }
    public interface ChangeItemListener {
        void ChangeItem(int position);

    }
    public interface OnAddDataListener {
        void onAddData();
    }
    public void setOnAddDataListener(OnAddDataListener listener) {
        this.addDataListener = listener;
    }
    private interface OnLoadNextPageListener {
        void onLoad();
    }
    private void setOnLoadNextPageListener(OnLoadNextPageListener listener) {
        this.mOnLoadListener = listener;
    }
}