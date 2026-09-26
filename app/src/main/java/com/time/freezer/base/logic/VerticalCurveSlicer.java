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

import com.time.freezer.filters.FilterManager;
import com.time.freezer.filters.IImageFilter;

public class VerticalCurveSlicer extends Slicer {
    Path path;
    Rect pathBounds;
    Path scannerPath;
    Matrix matrix;
    Paint fillPaint;
    Paint porterDuffPaint;
    Bitmap curveBitmap;
    Bitmap sliceBitmap;


    public VerticalCurveSlicer(int width, int height, int speedMultiplier, int scannerColor) {
        super(width, height, speedMultiplier, scannerColor);
        pathBounds = new Rect();
        matrix = new Matrix();
        fillPaint = new Paint();
        fillPaint.setColor(Color.BLACK);
        fillPaint.setStyle(Paint.Style.FILL);

        porterDuffPaint = new Paint();
        porterDuffPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        createPath(width * 1.0f / 2);
    }

    @Override
    public void drawSlice(Bitmap input, Bitmap output, IImageFilter filter) {
        if (curveBitmap == null) {
            curveBitmap = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
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
        return (scrollingSliceRect.left) >= screenWidth;
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
        if (currentPos.x >= screenWidth) {
            currentPos.x = screenWidth;
        } else {
            currentPos.x += increment;
        }
        matrix.reset();
        matrix.setTranslate(increment, 0);
        scannerPath.transform(matrix);

        pathBounds.set(pathBounds.left + increment, pathBounds.top, pathBounds.right + increment, pathBounds.bottom);
        scrollingSliceRect.set(scrollingSliceRect.left+ increment, scrollingSliceRect.top, scrollingSliceRect.right + increment, scrollingSliceRect.bottom);
        return currentPos;
    }

    protected void createPath(float archWidth) {
        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.lineTo(increment, 0);
        path.arcTo(new RectF(-archWidth / 2 + increment, 0, archWidth / 2 + increment, screenHeight), 270, 180);
        path.lineTo(0, screenHeight);
        path.moveTo(0, 0);
        path.arcTo(new RectF(-archWidth / 2, 0, archWidth / 2, screenHeight), 270, 180);

        RectF bounds = new RectF();
        path.computeBounds(bounds, false); // fills rect with bounds
        bounds.roundOut(pathBounds);
        rawSliceRect = new Rect(pathBounds);
        sliceBitmap = Bitmap.createBitmap(pathBounds.width(), pathBounds.height(), Bitmap.Config.ARGB_8888);
        scrollingSliceRect = new Rect(pathBounds.left - pathBounds.right, pathBounds.top, pathBounds.right- pathBounds.right, pathBounds.bottom);

        matrix.setTranslate(-pathBounds.right, 0);
        scannerPath = new Path(path);
        scannerPath.transform(matrix);
    }
}
