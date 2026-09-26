package com.time.freezer.filters;

import android.content.Context;
import android.graphics.Bitmap;

public abstract class IImageFilter {
    private boolean isFullImageFilter;

    protected int direction;

    public IImageFilter(Context context) {
    }

    public abstract Bitmap process(Bitmap bitmap);

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
}
