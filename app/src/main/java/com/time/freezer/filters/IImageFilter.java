package com.time.freezer.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;
import android.util.Log;

public abstract class IImageFilter {
    private long startTime;

    private boolean isFullImageFilter;

    protected RenderScript mRS;

    protected Allocation mInAllocation;

    protected Allocation mOutAllocation;

    protected ScriptC mScript;

    protected Bitmap mBitmapOut;

    protected int direction;

    public IImageFilter(Context context) {
        mRS = RenderScript.create(context);
    }

    protected void preProcess(Bitmap mBitmapIn) {
        mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        if (mOutAllocation == null) {
            mOutAllocation = Allocation.createTyped(mRS,
                    mInAllocation.getType());
            mBitmapOut = mBitmapIn.copy(mBitmapIn.getConfig(), true);
        }
    }

    protected void _preProcess() {
    }

    protected abstract void _process();

    protected void _postProcess() {

    }

    public Bitmap process(Bitmap bitmap) {
        preProcess(bitmap);
        startTime = System.currentTimeMillis();
        _preProcess();
        _process();
        Log.d("myApp", getClass().getSimpleName() + " use " + (System.currentTimeMillis() - startTime));
        _postProcess();
        postProcess();
        return mBitmapOut;
    }

    protected void postProcess() {
        mOutAllocation.copyTo(mBitmapOut);
    }

    protected void destory() {
        mScript.destroy();
        mScript = null;
        mInAllocation.destroy();
        mInAllocation = null;
        mOutAllocation.destroy();
        mOutAllocation = null;
        mRS.destroy();
        mRS = null;
        System.gc();
    }

    public boolean isFullImageFilter() {
        return isFullImageFilter;
    }
    public boolean isFakeFilter() {
        return false;
    }

    public boolean isBackgorundFilter() {
        return false;
    }

    public void setIsFullImageFilter(boolean isFullImageFilter) {
        this.isFullImageFilter = isFullImageFilter;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

};