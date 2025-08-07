package com.time.freezer.base.logic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

import com.time.freezer.filters.IImageFilter;

public class HorizontalSlicer extends Slicer {

    Bitmap sliceBitmap;
    public HorizontalSlicer(int width, int height, int speedMultiplier, int scannerColor) {
        super(width, height, speedMultiplier, scannerColor);
        sliceBitmap = Bitmap.createBitmap(screenWidth, increment, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void drawSlice(Bitmap input, Bitmap output, IImageFilter filter) {
        scrollingSliceRect = getSlice(currentScannerPoint);
        boolean fullImageFilter = filter.isFullImageFilter();
        if(fullImageFilter) {
            input = filter.process(input);
        }
        Canvas canvas = new Canvas(sliceBitmap);
        canvas.drawBitmap(input, scrollingSliceRect, rawSliceRect, paintAntiAlias);
        if(!fullImageFilter) {
            sliceBitmap = filter.process(sliceBitmap);
        }
        canvas = new Canvas(output);
        canvas.drawBitmap(sliceBitmap, rawSliceRect, scrollingSliceRect, paintAntiAlias);
        currentScannerPoint = getScanPosition(currentScannerPoint);
    }

    @Override
    public void drawScanner(Canvas canvas) {
        scannerRect.set(0, currentScannerPoint.y, screenWidth, currentScannerPoint.y + scannerWidth);
        canvas.drawRect(scannerRect, scannerPaint);
    }

    public boolean isScanDone() {
        return (currentScannerPoint.y >= screenHeight);
    }

    private Rect getSlice(Point currentPos) {
        rawSliceRect.set(0, 0, screenWidth, increment);
        scrollingSliceRect.set(0, currentPos.y, screenWidth, currentPos.y + increment);
        return scrollingSliceRect;
    }

    private Point getScanPosition(Point currentPos) {
        if (currentPos.y >= screenHeight) {
            currentPos.y = screenHeight;
        } else {
            currentPos.y += increment;
        }
        return currentPos;
    }
}