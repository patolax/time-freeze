package com.time.freezer.base.logic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.time.freezer.filters.IImageFilter;
import com.time.freezer.fragments.Constants;
import com.time.freezer.fragments.ScanSettings;

public abstract class Slicer {

    protected int screenWidth;
    protected int screenHeight;
    protected Rect scrollingSliceRect;
    protected Rect rawSliceRect;
    protected Rect scannerRect;
    protected int increment = 5;
    protected int scannerWidth = 6;
    protected Point currentScannerPoint;
    protected Paint paintAntiAlias;
    protected Paint scannerPaint;

    public Slicer(int width, int height, int speedMultiplier, int scannerColor) {
        screenHeight = height;
        screenWidth = width;
        scrollingSliceRect = new Rect();
        rawSliceRect = new Rect();
        scannerRect = new Rect();
        increment = increment * speedMultiplier;
        currentScannerPoint = new Point(0, 0);
        paintAntiAlias = new Paint(Paint.FILTER_BITMAP_FLAG);
        paintAntiAlias.setAntiAlias(true);

        scannerPaint = new Paint();
        scannerPaint.setColor(scannerColor);
        scannerPaint.setStyle(Paint.Style.FILL);
    }

    public abstract void drawSlice(Bitmap input, Bitmap output, IImageFilter filter);

    public abstract void drawScanner(Canvas canvas);

    public int getCurrentScannerY() {
        return currentScannerPoint != null ? currentScannerPoint.y : 0;
    }

    public int getIncrement() { return increment; }

    public int getScannerWidth() { return scannerWidth; }

    public abstract boolean isScanDone();

    public void cleanUp() {
        scrollingSliceRect = null;
        scannerRect = null;
        currentScannerPoint = null;
        paintAntiAlias = null;
        scannerPaint = null;
    }

    public static Slicer createSlicer(ScanSettings settings, int width, int height) {
        switch (settings.getDirection()) {
            case Constants
                    .SCAN_DIRECTION_HORIZONTAL: {
                switch (settings.getShape()) {
                    case Constants
                            .SCAN_SHAPE_LINE:
                        return new HorizontalSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    case Constants
                            .SCAN_SHAPE_CURVE:
                        return new HorizontalCurveSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    case Constants
                            .SCAN_SHAPE_ZIGZAG:
                        return new HorizontalZigZagSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    default:
                        return new HorizontalSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                }
            }
            case Constants
                    .SCAN_DIRECTION_VERTICAL: {
                switch (settings.getShape()) {
                    case Constants
                            .SCAN_SHAPE_LINE:
                        return new VerticalSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    case Constants
                            .SCAN_SHAPE_CURVE:
                        return new VerticalCurveSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    case Constants
                            .SCAN_SHAPE_ZIGZAG:
                        return new VerticalZigZagSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                    default:
                        return new VerticalSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
                }
            }
            default:
                return new HorizontalSlicer(width, height, settings.getSpeed(), settings.getScannerColor());
        }
    }
}
