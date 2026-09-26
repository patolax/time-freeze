package com.time.freezer.base.gl;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;

import com.time.freezer.R;
import com.time.freezer.base.logic.HorizontalSlicer;
import com.time.freezer.base.logic.Slicer;
import com.time.freezer.base.utils.FileLogger;
import com.time.freezer.base.utils.RecordingStatus;
import com.time.freezer.filters.FilterFactory;
import com.time.freezer.filters.IImageFilter;
import com.time.freezer.fragments.Constants;
import com.time.freezer.fragments.ScanSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;


public class GlOverlayFilter extends GlFilter implements Disposable {
    private int[] textures = new int[1];
    private Bitmap bitmap = null;
    private Bitmap finalBitmap = null;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    protected Size inputResolution = null;
    Matrix lastTransformationMatrix;
    Path path;
    boolean isScrollring = false;
    CompositeDisposable subsriptions = new CompositeDisposable();
    Context mContext;
    FilterFactory filterManager;
    IImageFilter filter;
    ReentrantLock lock = new ReentrantLock();
    Bitmap result;
    ScanSettings settings;
    Slicer slicer;
    boolean imageSaved = false;
    boolean saveImageSetting = false;
    private final ExecutorService mSaveExecutor = Executors.newSingleThreadExecutor();

    public GlOverlayFilter(Context context) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        path = new Path();
        mContext = context;
        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public void setBitmap(Bitmap newbitmap) {
        this.overlay(newbitmap);
    }

    private final static String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "uniform lowp sampler2D oTexture;\n" +
                    "void main() {\n" +
                    "   lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                    "   lowp vec4 textureColor2 = texture2D(oTexture, vTextureCoord);\n" +
                    "   gl_FragColor = mix(textureColor, textureColor2, textureColor2.a);\n" +
                    "}\n";

    public void setRecordingObservable(Observable<RecordingStatus> recordingObservable) {
        if (recordingObservable != null) {
            Disposable disposable = recordingObservable
                    .subscribe(state -> startStopScroller(state), t -> {
                    });
            subsriptions.add(disposable);
        }
    }

    private void startStopScroller(RecordingStatus state) {
        if (state == RecordingStatus.Start) {
            applySettings();
            isScrollring = true;
        } else if (state == RecordingStatus.Pause) {
            isScrollring = false;
        } else if (state == RecordingStatus.Restart) {
            isScrollring = true;
        } else {
            isScrollring = false;
            result = null;
            reset();
        }
    }

    public void setScanSettings(ScanSettings settings) {
        this.settings = settings;
        createFilter();
    }

    public void setResolution(Size resolution) {
        this.inputResolution = resolution;
    }

    @Override
    public void setFrameSize(int width, int height) {
        super.setFrameSize(width, height);
        mScreenWidth = width;
        mScreenHeight = height;
        releaseBitmap(bitmap);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        slicer = new HorizontalSlicer(mScreenWidth, mScreenHeight, Constants.SCAN_SPEED_1X, Color.RED);
        //FileLogger.appendLog(this.getClass().getName(), "setFrameSize", width + " " + height);
    }

    public void setScreenSize(int width, int height) {
        setResolution(new Size(width, height));
        createBitmap();
        //FileLogger.appendLog(this.getClass().getName(), "setScreenSize", width + " " + height);
    }

    private void createBitmap() {
        mPreviewHeight = inputResolution.getHeight();
        mPreviewWidth = inputResolution.getWidth();
        updateTransformationIfNeeded();
        finalBitmap = Bitmap.createBitmap(mPreviewWidth, mPreviewHeight, Bitmap.Config.ARGB_8888);
        //FileLogger.appendLog(this.getClass().getName(), "createBitmap", mPreviewWidth + " " + mPreviewHeight);
        imageSaved = false;
    }

    @Override
    public void setup() {
        super.setup();// 1
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onDraw() {
        try {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.eraseColor(Color.argb(0, 0, 0, 0));
                Canvas bitmapCanvas = new Canvas(bitmap);
                bitmapCanvas.scale(1, -1, bitmapCanvas.getWidth() / 2, bitmapCanvas.getHeight() / 2);
                bitmapCanvas.concat(lastTransformationMatrix);
                drawCanvas(bitmapCanvas);

                int offsetDepthMapTextureUniform = getHandle("oTexture");// 3

                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);
                GLES20.glUniform1i(offsetDepthMapTextureUniform, 3);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void overlay(Bitmap inputBitmap) {
        if (!isScrollring) return;
        if (inputBitmap == null) return;
        long start = System.currentTimeMillis();
        lock.lock();
        try {
            if (slicer == null) {
                Log.e("myApp", "overlay: slicer is NULL, cannot scan");
                return;
            }
            if (!slicer.isScanDone()) {
                slicer.drawSlice(inputBitmap, finalBitmap, filter);
                result = finalBitmap;
                if (!filter.isBackgorundFilter()) {
                    inputBitmap.recycle();
                    inputBitmap = null;
                }
            } else if (saveImageSetting) {
                saveImage(result);
            }
        } finally {
            lock.unlock();
        }
    }

    protected void drawCanvas(Canvas canvas) {
        if (result != null) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                canvas.drawBitmap(result, 0, 0, null);
                if (!slicer.isScanDone()) slicer.drawScanner(canvas);
            } finally {
                lock.unlock();
            }
        }
    }

    public void reset() {
        bitmap.eraseColor(Color.TRANSPARENT);
        createBitmap();
    }

    private void applySettings() {
        if (settings == null) {
            settings = new ScanSettings();
        }
        if (mPreviewWidth == 0 || mPreviewHeight == 0) {
            return;
        }
        slicer = Slicer.createSlicer(settings, mPreviewWidth, mPreviewHeight);
        filter.setDirection(settings.getDirection());
        saveImageSetting = settings.isSaveImage();
    }

    private void createFilter() {
        if (settings == null) {
            settings = new ScanSettings();
        }
        filterManager = new FilterFactory();
        filter = filterManager.createFilter(mContext, settings.getFilter());
    }

    private void updateTransformationIfNeeded() {
        lastTransformationMatrix = new Matrix();
        lastTransformationMatrix.setScale(mScreenWidth * 1.0f / mPreviewWidth, mScreenHeight * 1.0f / mPreviewHeight);
        //FileLogger.appendLog(this.getClass().getName(), "updateTransformationIfNeeded", mPreviewHeight + " " + mScreenHeight);
    }

    @Override
    public void dispose() {
        if (subsriptions != null) {
            subsriptions.clear();
        }
        releaseBitmap(bitmap);
        releaseBitmap(finalBitmap);
        releaseBitmap(result);
        mSaveExecutor.shutdown();
    }

    public static void releaseBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    private void saveImage(Bitmap bitmap) {
        try {
            if (bitmap == null || bitmap.isRecycled()) return;
            if (imageSaved || filter.isBackgorundFilter() || filter.isFakeFilter()) {
                return;
            }
            imageSaved = true;
            Bitmap bmp2 = bitmap.copy(bitmap.getConfig(), true);
            mSaveExecutor.execute(() -> saveImageInBackground(bmp2));
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    private void saveImageInBackground(Bitmap bitmap) {
        try {
            if (bitmap == null) return;
            ContentResolver resolver = mContext.getContentResolver();
            String fileName = "TF_" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + ".jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + mContext.getString(R.string.app_name));
            } else {
                File directory = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            }

            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream output = resolver.openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                output.flush();
                output.close();
            }

        } catch (IOException e) {
            Log.d("myApp", "Image saved!" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d("myApp", "Image saved!" + e.getMessage());
            e.printStackTrace();
        }
    }
}
