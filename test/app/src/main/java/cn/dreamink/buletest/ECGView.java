package cn.dreamink.buletest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 心电图
 * Created by zhaoqin on 7/10/15.
 * Modified by meibenshan on 2017-2-23 17:29:48
 */
public class ECGView extends View {

    private static final String TAG = "ECGView";
    public static final int COLOR = 0xff00b7ee;

    private Paint mPaint;
    private List<Float> list;
    private boolean keepRunning = false;
    private int parentWidth;
    private int parentHeight;
    private float step;

    /**
     * 数据长度600个点，即3s，一般不要更改
     */
    private int dataSize = 600;

    /**
     * 刷新图像
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    invalidate();
                    break;
                default:
                    break;
            }
        }
    };

    public ECGView(Context context, AttributeSet attributes) {
        super(context, attributes);
        mPaint = new Paint();
        mPaint.setColor(COLOR);
        mPaint.setStrokeWidth(switchDIP(context, 1.0f));
        mPaint.setAntiAlias(true);
        list = new ArrayList<>();
    }
    /**
     * dp转px
     */
    public static int switchDIP(Context context, float size) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (size * scale + 0.5f);
    }

    /**
     * get the view height and width
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        step = (float) parentWidth / dataSize;
        this.setMeasuredDimension(parentWidth, parentHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        List<Float> plotList = list;
        int size = plotList.size();
        if (size != 0) {
            canvas.drawLine(0, parentHeight / 2, step, plotList.get(0), mPaint);
            for (int i = 1; i < size; i++) {
                canvas.drawLine(i * step, plotList.get(i - 1), (i + 1) * step, plotList.get(i), mPaint);
            }
        }
    }

    /**
     * 加载数据
     */
    public void addData(float[] data) {
        for (float aData : data) {
            int size = list.size();
            if (size == dataSize) {
                list.remove(0);
            }
            list.add((1 - aData) * (parentHeight / 2));
        }
    }

    /**
     * start a new thread to plot lines
     * 心电监测界面绘制实时的心电图，要调用此函数以自动更新图像
     */
    public synchronized void startThread() {
        if (!keepRunning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        keepRunning = true;
                        while (isKeepRunning()) {
                            //200ms更新一次
                            Thread.sleep(200);
                            Message message = new Message();
                            message.what = 0;
                            mHandler.sendMessage(message);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public synchronized boolean isKeepRunning() {
        return keepRunning;
    }

    /**
     * stop running thread
     */
    public synchronized void stopThread() {
        keepRunning = false;
    }
}