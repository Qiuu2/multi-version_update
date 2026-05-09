package com.htgd.radiocontrol.aeroradiocontrol.component.SwipableRecycleView;


import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by wzq on 2017/6/9.
 *
 */

public class DefaultItemTouchHelpCallback extends ItemTouchHelper.Callback {

    /* Item 操作回调 */
    private OnItemTouchCallbackListener onItemTouchCallbackListener;

    /* 是否可以拖拽排序 */
    private boolean isCanDrag = false;

    /* 是否可以滑动删除 */
    private boolean isCanSwipe = false;

    public DefaultItemTouchHelpCallback(OnItemTouchCallbackListener onItemTouchCallbackListener) {
        this.onItemTouchCallbackListener = onItemTouchCallbackListener;
    }

    public boolean isCanDrag() {
        return isCanDrag;
    }

    public void setCanDrag(boolean canDrag) {
        isCanDrag = canDrag;
    }

    public boolean isCanSwipe() {
        return isCanSwipe;
    }

    public void setCanSwipe(boolean canSwipe) {
        isCanSwipe = canSwipe;
    }

    @Override
    public boolean isLongPressDragEnabled() {   // 是否可以长按拖拽
        return isCanDrag;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {   // 是否可以滑动(最好要判断左右或者上下)
        return isCanSwipe;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {    // 事件的标志: 用户拖拽或者滑动时告诉系统方向
        // 获取当前布局
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        // 根据不同布局做出不同处理
        if (layoutManager instanceof GridLayoutManager) {   // 网格布局
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT; // flags = 0 相当于关闭这个操作
            return makeMovementFlags(dragFlags, swipeFlags);

        } else if (layoutManager instanceof LinearLayoutManager) {  // 线性布局
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            int orientation = linearLayoutManager.getOrientation();
            int dragFlags = 0;
            int swipeFlags = 0;
            if (orientation == LinearLayoutManager.HORIZONTAL) {    // 如果是水平的列表
                dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                swipeFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            } else if (orientation == LinearLayoutManager.VERTICAL) {   // 如果是垂直的列表
                dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            }
            return makeMovementFlags(dragFlags, swipeFlags);

        } else if (layoutManager instanceof StaggeredGridLayoutManager) {   // 瀑布流布局
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT; // flags = 0 相当于关闭这个操作
            return makeMovementFlags(dragFlags, swipeFlags);

        }
        return 0;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (onItemTouchCallbackListener != null) {
            return onItemTouchCallbackListener.onMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        }
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (onItemTouchCallbackListener != null) {
            onItemTouchCallbackListener.onSwiped(viewHolder.getAdapterPosition());
        }
    }

    public interface OnItemTouchCallbackListener {

        /* 当两个Item位置互换的时候被回调 */
        boolean onMove(int srcPosition, int targetPosition);

        /* 当某个Item被滑动删除的时候 */
        void onSwiped(int adapterPosition);
    }

}
