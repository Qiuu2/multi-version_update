package  com.htgd.radiocontrol.aeroradiocontrol.component;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import androidx.core.widget.ScrollerCompat;

public class SwipeItemLayout extends FrameLayout {
    private View contentView = null;
    private View menuView = null;
    private Interpolator closeInterpolator = null;
    private Interpolator openInterpolator = null;
    // 滑动的最小距离
    private int touchSlop;
    private ScrollerCompat mOpenScroller;
    private ScrollerCompat mCloseScroller;
    private boolean hasOnDeleteListener = false;
    private int mBaseX;
    private int xStart;
    private int xEnd;
    private int yStart;
    private int yEnd;
    private int state = STATE_CLOSE;

    private static final int STATE_CLOSE = 0;
    private static final int STATE_OPEN = 1;

    public SwipeItemLayout(View contentView, View menuView, Interpolator closeInterpolator, Interpolator openInterpolator) {
        super(contentView.getContext());
        this.contentView = contentView;
        this.menuView = menuView;
        this.closeInterpolator = closeInterpolator;
        this.openInterpolator = openInterpolator;
        // 表示控件移动的最小距离，手移动的距离大于这个距离才能拖动控件
         touchSlop = ViewConfiguration.get(contentView.getContext()).getScaledTouchSlop() *10;
        init();
    }

    private void init() {
        setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        if (closeInterpolator != null) {
            mCloseScroller = ScrollerCompat.create(getContext(),
                    closeInterpolator);
        } else {
            mCloseScroller = ScrollerCompat.create(getContext());
        }
        if (openInterpolator != null) {
            mOpenScroller = ScrollerCompat.create(getContext(), openInterpolator);
        } else {
            mOpenScroller = ScrollerCompat.create(getContext());
        }

        LayoutParams contentParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        contentView.setLayoutParams(contentParams);

        menuView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        addView(contentView);
        addView(menuView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xStart = x;
                yStart = y;
                break;
            case MotionEvent.ACTION_MOVE:
                xEnd = x;
                yEnd = y;
                int dis = (xStart - xEnd);
                if (state == STATE_OPEN) {
                    dis += menuView.getWidth();
                }
                if (hasOnDeleteListener) {
                    swipe(dis);
                }
                break;
            case MotionEvent.ACTION_UP:
                xEnd = x;
                yEnd = y;
                boolean bb =  (xStart-xEnd)>touchSlop;
                if (hasOnDeleteListener) {
                    if (bb) {
                        smoothOpenMenu();
                    } else {
                        smoothCloseMenu();
                    }
                }
                break;
        }
        return true;
    }

    public boolean isOpen() {
        return state == STATE_OPEN;
    }

    private void swipe(int dis) {

        if (dis >= menuView.getWidth()) {
            dis = menuView.getWidth();
        }
        if (dis < 0) {
            dis = 0;
        }
        contentView.layout(-dis, contentView.getTop(), contentView.getWidth() - dis, contentView.getBottom());
        menuView.layout(contentView.getWidth() - dis, menuView.getTop(), contentView.getWidth() + menuView.getWidth() - dis,
                menuView.getBottom());
    }

    @Override
    public void computeScroll() {
        if (state == STATE_OPEN) {
            if (mOpenScroller.computeScrollOffset()) {
                swipe(mOpenScroller.getCurrX());
                postInvalidate();
            }
        } else {
            if (mCloseScroller.computeScrollOffset()) {
                swipe(mBaseX - mCloseScroller.getCurrX());
                postInvalidate();
            }
        }
    }

    public void smoothCloseMenu() {
        state = STATE_CLOSE;
        mBaseX = -contentView.getLeft();
        mCloseScroller.startScroll(0, 0, mBaseX, 0, 500);
        postInvalidate();
    }



    public void smoothOpenMenu() {
        state = STATE_OPEN;
        mOpenScroller.startScroll(-contentView.getLeft(), 0, menuView.getWidth(), 0, 500);
        postInvalidate();
    }





    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        menuView.measure(MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        contentView.layout(0, 0, getMeasuredWidth(),
                contentView.getMeasuredHeight());
        menuView.layout(getMeasuredWidth(), 0,
                getMeasuredWidth() + menuView.getMeasuredWidth(),
                contentView.getMeasuredHeight());
    }

    public void setHasOnDeleteListener(boolean hasOnDeleteListener) {
        this.hasOnDeleteListener = hasOnDeleteListener;
    }
}
