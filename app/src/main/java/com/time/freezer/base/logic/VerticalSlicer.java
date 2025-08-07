package com.time.freezer.base.logic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

import com.time.freezer.filters.IImageFilter;

public class VerticalSlicer extends Slicer {

    Bitmap sliceBitmap;

    public VerticalSlicer(int width, int height, int speedMultiplier, int scannerColor) {
        super(width, height, speedMultiplier, scannerColor);
        sliceBitmap = Bitmap.createBitmap(increment, screenHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void drawSlice(Bitmap input, Bitmap output, IImageFilter filter) {
        scrollingSliceRect = getSlice(currentScannerPoint);
        boolean fullImageFilter = filter.isFullImageFilter();
        if (fullImageFilter) {
            input = filter.process(input);
        }
        Canvas canvas = new Canvas(sliceBitmap);
        canvas.drawBitmap(input, scrollingSliceRect, rawSliceRect, paintAntiAlias);
        if (!fullImageFilter) {
            sliceBitmap = filter.process(sliceBitmap);
        }
        canvas = new Canvas(output);
        canvas.drawBitmap(sliceBitmap, rawSliceRect, scrollingSliceRect, paintAntiAlias);
        currentScannerPoint = getScanPosition(currentScannerPoint);
    }

    @Override
    public void drawScanner(Canvas canvas) {
        scannerRect.set(currentScannerPoint.x, 0, currentScannerPoint.x + scannerWidth, screenHeight);
        canvas.drawRect(scannerRect, scannerPaint);
    }

    @Override
    public boolean isScanDone() {
        return currentScannerPoint.x >= screenWidth;
    }

    private Rect getSlice(Point currentPos) {
        rawSliceRect.set(0, 0, increment, screenHeight);
        scrollingSliceRect.set(currentPos.x, 0, currentPos.x + increment, screenHeight);
        return scrollingSliceRect;
    }

    private Point getScanPosition(Point currentPos) {
        if (currentPos.x >= screenWidth) {
            currentPos.x = screenWidth;
        } else {
            currentPos.x += increment;
        }
        return currentPos;
    }
}
