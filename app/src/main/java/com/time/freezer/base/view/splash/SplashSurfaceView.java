package com.time.freezer.base.view.splash;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import com.time.freezer.R;



public class SplashSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder holder;
    private Thread drawThread;
    private boolean surfaceReady = false;
    private boolean drawingActive = false;
    private Paint samplePaint = new Paint();
    private Paint samplePaintColor2 = new Paint();
    private static final int MAX_FRAME_TIME = (int) (1000.0 / 60.0);
    Bitmap fame1;
    Bitmap fame2;
    private int width;
    private int height;
    float frameW;
    float frameH;
    float offsetX;
    float offsetY;
    int scrollPos = 0;
    float delta = 0;
    private static final String LOGTAG = "surface";
    int color;
    int color2;
    final int scanerWidth = 10;

    private SplashAnimationListner listner;

    public SplashSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        Resources resource = getContext().getResources();
        color = ResourcesCompat.getColor(getResources(), R.color.selectedColor, null);
        color2 = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);

        samplePaint = new Paint();
        samplePaint.setAntiAlias(true);
        samplePaint.setStrokeWidth(3);
        samplePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        samplePaint.setShadowLayer(5 * 2, 0, 0, color2);
        samplePaint.setColor(color);

        fame1 = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.frame1);
        fame2 = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.frame2);


        samplePaintColor2 = new Paint();
        samplePaintColor2.setColor(color2);
        samplePaintColor2.setStyle(Paint.Style.FILL);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }
        width = w;
        height = h;
        frameW = (width * 1.0f) * .75f;
        frameH = frameW * (fame1.getHeight() * 1.0f / fame1.getWidth());

        offsetX = (width - frameW)/2.0f;
        offsetY = (height - frameH)/2.0f;

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder = holder;
        if (drawThread != null) {
            drawingActive = false;
            try {
                drawThread.join();
            } catch (InterruptedException e) { // do nothing
            }
        }

        surfaceReady = true;
        startDrawThread();
        Log.d(LOGTAG, "Created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDrawThread();
        holder.getSurface().release();

        this.holder = null;
        surfaceReady = false;
        Log.d(LOGTAG, "Destroyed");
    }

    public void stopDrawThread() {
        if (drawThread == null) {
            return;
        }
        drawingActive = false;
        while (true) {
            try {
                Log.d(LOGTAG, "Request last frame");
                drawThread.join(5000);
                break;
            } catch (Exception e) {
                Log.e(LOGTAG, "Could not join with draw thread");
            }
        }
        drawThread = null;
    }

    public void startDrawThread() {
        if (surfaceReady && drawThread == null) {
            drawThread = new Thread(this, "Draw thread");
            drawingActive = true;
            drawThread.start();
        }
    }

    @Override
    public void run() {
        long frameStartTime;
        long frameTime;

        try {
            while (drawingActive) {
                if (holder == null) {
                    return;
                }

                frameStartTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(color, android.graphics.PorterDuff.Mode.CLEAR);
                canvas.drawColor(color);
                if (canvas != null) {
                    canvas.drawBitmap(fame1, null, new RectF(offsetX, offsetY, offsetX + frameW, offsetY + frameH), null);
                    canvas.drawRect(0, 0, scrollPos + scanerWidth, getHeight(), samplePaintColor2);
                    if (scrollPos >= offsetX)
                        canvas.drawBitmap(fame2, new Rect(0, 0, (int)((scrollPos - offsetX)*(fame2.getWidth()/frameW)), (int)fame2.getHeight()),
                                new RectF(offsetX, offsetY, scrollPos, offsetY + frameH), null);
                    try {
                        canvas.drawRect(scrollPos, 0, scrollPos + scanerWidth, getHeight(), samplePaint);
                    } finally {

                        holder.unlockCanvasAndPost(canvas);
                    }
                }

                // calculate the time required to draw the frame in ms
                frameTime = (System.nanoTime() - frameStartTime) / 1000000;
                delta += frameTime;
                if (delta > 25) {
                    scrollPos += 10;
                    delta = 0;
                }

                if(scrollPos >= (width + scanerWidth + 2) && listner != null){
                    listner.animationDone();
                }

                if (frameTime < MAX_FRAME_TIME) // faster than the max fps - limit the FPS
                {
                    try {
                        Thread.sleep(MAX_FRAME_TIME - frameTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            Log.w(LOGTAG, "Exception while locking/unlocking");
        }
        Log.d(LOGTAG, "Draw thread finished");
    }

    public void setListner(SplashAnimationListner listner) {
        this.listner = listner;
    }

    public interface SplashAnimationListner{
        void animationDone();
    }
}