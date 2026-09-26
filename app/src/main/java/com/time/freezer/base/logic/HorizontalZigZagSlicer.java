package com.time.freezer.base.logic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.time.freezer.filters.IImageFilter;

public class HorizontalZigZagSlicer extends Slicer {
    Path path;
    Path scannerPath;
    Rect pathBounds;
    Matrix matrix;
    Paint fillPaint;
    Paint porterDuffPaint;
    Bitmap curveBitmap;
    Bitmap sliceBitmap;

    public HorizontalZigZagSlicer(int width, int height, int speedMultiplier, int scannerColor) {
        super(width, height, speedMultiplier, scannerColor);
        pathBounds = new Rect();
        matrix = new Matrix();
        fillPaint = new Paint();
        fillPaint.setColor(Color.BLACK);
        fillPaint.setStyle(Paint.Style.FILL);

        porterDuffPaint = new Paint();
        porterDuffPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        createPath(height / 40f);
    }

    @Override
    public void drawSlice(Bitmap input, Bitmap output, IImageFilter filter) {
        if (curveBitmap == null) {
            curveBitmap = Bitmap.createBitmap(pathBounds.width(), pathBounds.height(), Bitmap.Config.ARGB_8888);
        } else {
            curveBitmap.eraseColor(Color.TRANSPARENT);
        }
        if (filter.isFakeFilter()) {
            Canvas canvasF = new Canvas(output);
            canvasF.drawBitmap(curveBitmap, rawSliceRect, scrollingSliceRect, paintAntiAlias);
            currentScannerPoint = getScanPosition(currentScannerPoint);
            return;
        }
        boolean fullImageFilter = filter.isFullImageFilter();
        if (fullImageFilter) {
            input = filter.process(input);
        }
        Canvas canvas = new Canvas(sliceBitmap);
        canvas.drawBitmap(input, scrollingSliceRect, rawSliceRect, paintAntiAlias);
        if (!fullImageFilter) {
            sliceBitmap = filter.process(sliceBitmap);
        }
        canvas = new Canvas(curveBitmap);
        canvas.drawPath(path, fillPaint);
        canvas.drawBitmap(sliceBitmap, 0, 0, porterDuffPaint);

        canvas = new Canvas(output);
        canvas.drawBitmap(curveBitmap, rawSliceRect, scrollingSliceRect, paintAntiAlias);
        currentScannerPoint = getScanPosition(currentScannerPoint);
    }

    @Override
    public void drawScanner(Canvas canvas) {
        canvas.drawPath(scannerPath, scannerPaint);
    }

    @Override
    public boolean isScanDone() {
        return (scrollingSliceRect.top) >= screenHeight;
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        path = null;
        fillPaint = null;
        porterDuffPaint = null;
        if (curveBitmap != null) {
            curveBitmap.recycle();
            curveBitmap = null;
        }
    }

    private Point getScanPosition(Point currentPos) {
        if (currentPos.y >= screenHeight) {
            currentPos.y = screenHeight;
        } else {
            currentPos.y += increment;
        }
        matrix.reset();
        matrix.setTranslate(0, increment);
        scannerPath.transform(matrix);
        pathBounds.set(pathBounds.left, pathBounds.top + increment, pathBounds.right, pathBounds.bottom + increment);
        scrollingSliceRect.set(scrollingSliceRect.left, scrollingSliceRect.top + increment, scrollingSliceRect.right, scrollingSliceRect.bottom + increment);
        return currentPos;
    }

    protected void createPath(float archWidth) {
        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.lineTo(0, increment);
        float tsize = screenWidth * 1.0f / 50;
        float x = 0;
        while (x <= screenWidth) {
            x += tsize;
            path.lineTo(x, archWidth * 2);
            x += tsize;
            path.lineTo(x, increment);
        }
        path.lineTo(screenWidth, 0);
        //path.close();
        path.moveTo(0, 0);
        x = 0;
        while (x <= screenWidth) {
            x += tsize;
            path.lineTo(x, archWidth);
            x += tsize;
            path.lineTo(x, 0);
        }

        RectF bounds = new RectF();
        path.computeBounds(bounds, false); // fills rect with bounds
        bounds.roundOut(pathBounds);
        rawSliceRect = new Rect(pathBounds);
        sliceBitmap = Bitmap.createBitmap(pathBounds.width(), pathBounds.height(), Bitmap.Config.ARGB_8888);
        scrollingSliceRect = new Rect(pathBounds.left, pathBounds.top - pathBounds.bottom, pathBounds.right, pathBounds.bottom - pathBounds.bottom);

        matrix.setTranslate(0, -pathBounds.bottom);
        scannerPath = new Path(path);
        scannerPath.transform(matrix);
    }
}
