package com.htgd.radiocontrol.aeroradiocontrol.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.htgd.radiocontrol.aeroradiocontrol.R;

public class VoiceRatingBar extends View {

    private static final String TAG = "VolumeView";
    private  Context context;
    private AttributeSet attrs;
    // 小喇叭图片
    private Bitmap volume;
    private Paint paint = new Paint();
    // 控件高度
    private int height = 100;
    // 控件宽度
    private int width = 350;
    // 最大音量
    private int MAX = 15;
    // 两个音量矩形最左侧之间的间隔
    private int rectMargen = 20;
    // 音量矩形高
    private int rectH = 40;
    // 音量矩形宽
    private int recW = 30;
    // 当前选中的音量
    private int current = 0;
    // 最左侧音量矩形距离控件最左侧距离
    private int leftMargen = 0;

    public VoiceRatingBar(Context context) {
        super(context);
        init();
    }

    public VoiceRatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        init();
    }

    public VoiceRatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        volume = BitmapFactory.decodeResource(getResources(), R.mipmap.voice_small_image);
        leftMargen = volume.getWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制背景颜色
        paint.setColor(getResources().getColor(R.color.colorWhite));
        canvas.drawRect(0, 0, width, height, paint);

        // 绘制没有被选中的白色音量矩形
        paint.setColor(getResources().getColor(R.color.colorYellw));
        for (int i = current; i < MAX; i++) {
            canvas.drawRect(leftMargen + (i + 2) * rectMargen, (height - rectH) / 2, leftMargen + (i + 2) * rectMargen + recW, (height - rectH) / 2 + rectH,
                    paint);
        }

        // 绘制被选中的橘黄色音量矩形
        paint.setColor(getResources().getColor(R.color.colorBlue));
        for (int i = 0; i < current; i++) {
            canvas.drawRect(leftMargen + (i + 2) * rectMargen, (height - rectH) / 2, leftMargen + (i + 2) * rectMargen + recW, (height - rectH) / 2 + rectH,
                    paint);
        }
        // 绘制音量图片
        canvas.drawBitmap(volume, volume.getWidth() / 2, (height - volume.getHeight()) / 2, paint);
        // 绘制音量减少图片
     //   canvas.drawBitmap(volume, volume.getWidth() / 2 + volume.getWidth(), (height - volume.getHeight()) / 2, paint);
        // 绘制音量增加图片
     //   canvas.drawBitmap(volume, leftMargen + (MAX + 2) * rectMargen, (height - volume.getHeight()) / 2, paint);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                // 当触摸位置在音量矩形之内时，获取当前选中的音量矩形数量
                if ((event.getX() > leftMargen + rectMargen && event.getX() < leftMargen + (MAX + 1) * rectMargen + recW)
                        && (event.getY() > (height - rectH) / 2 && event.getY() < (height - rectH) / 2 + rectH)) {
                    current = (int) ((event.getX() - (leftMargen)) / (rectMargen)) - 1;
                    if (onChangeListener != null) {
                        onChangeListener.onChange(current);
                    }
                    Log.d(TAG, "current:" + current);
                }
                break;
        }
        // 通知界面刷新
        invalidate();
        // 拦截触摸事件
        return true;
    }

    // 高度父布局要占用的位置大小
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    public interface OnChangeListener {
        public void onChange(int count);
    }

    private OnChangeListener onChangeListener;

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

}