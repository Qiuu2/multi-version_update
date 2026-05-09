package com.htgd.radiocontrol.aeroradiocontrol.component.SwipableRecycleView;


import androidx.recyclerview.widget.ItemTouchHelper;

/**
 * Created by wzq on 2019/6/9.
 * <p>
 * 核心:自定义ItemTouchHelper
 * 可实现移动item  左右滑动
 */

public class DefaultItemTouchHelper extends ItemTouchHelper {
    private DefaultItemTouchHelpCallback callback;

    public DefaultItemTouchHelper(Callback callback) {
        super(callback);
        this.callback = (DefaultItemTouchHelpCallback) callback;
    }

    /**
     * 包装一个方便点的Helper
     */
    public static class PackTouchHelper extends DefaultItemTouchHelper {    // TODO 直接使用这个内部对象
        private DefaultItemTouchHelpCallback callback;

        public PackTouchHelper(DefaultItemTouchHelpCallback.OnItemTouchCallbackListener listener) {
            super(new DefaultItemTouchHelpCallback(listener));
            this.callback = super.callback;
        }

        /**
         * 设置是否可以被拖拽
         *
         * @param canDrag 是true，否false
         */
        public void setDragEnable(boolean canDrag) {
            callback.setCanDrag(canDrag);
        }

        /**
         * 设置是否可以被滑动
         *
         * @param canSwipe 是true，否false
         */
        public void setSwipeEnable(boolean canSwipe) {
            callback.setCanSwipe(canSwipe);
        }
    }

}
