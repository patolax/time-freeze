package com.time.freezer.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.time.freezer.R;
import com.time.freezer.base.utils.FileLogger;
import com.time.freezer.base.utils.RecordingStatus;
import com.time.freezer.base.utils.SharedPreferencesManager;
import com.time.freezer.base.utils.YuvToRgbConverter;
import com.time.freezer.base.view.RecordableSurfaceView;
import com.time.freezer.base.gl.VideoRenderer;
import com.time.freezer.databinding.ActivityRsvBinding;
import com.time.freezer.databinding.FragmentLandingBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Fragment for operating the camera, it doesnt have any UI elements, just controllers
 */
public class VideoFragment extends Fragment implements VideoRenderer.OnRendererReadyListener, OnGalleryClickListener {

    private static final String TAG = "VideoFragment";

    private static VideoFragment __instance;
    private ImageReader mImageReader;
    private VideoRenderer mVideoRenderer;
    private CameraCaptureSession mPreviewSession;
    private File mOutputFile;
    private boolean mIsRecording = false;
    private boolean mdisableClick = false;
    private CompositeDisposable compositeDisposable;
    private int mPreviewTexture;
    private Size mPreviewSize;
    YuvToRgbConverter converter;
    private CaptureRequest.Builder mPreviewBuilder;
    boolean firstLoad;

    Observable<RecordingStatus> mIsRecordingObservable;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread2;
    private Handler mBackgroundHandler2;
    private SurfaceTexture mSurfaceTexture;
    public static final int CAMERA_PRIMARY = 0;
    public static final int CAMERA_FORWARD = 1;
    protected int mCameraToUse = CAMERA_FORWARD;
    protected boolean mCameraSetupInProgress = true;
    int mCameraRotation = 0;
    int mDeviceRotation = 0;
    boolean isFlipped = false;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mCameraIsOpen = false;
    private CameraDevice mCameraDevice;
    File currentFile = null;
    public static final String TEST_VIDEO_FILE_NAME = "time_freezer";
    BehaviorSubject<RecordingStatus> mIsRecordingSubject;
    int screenWidth;
    int screenHeight;
    RecordingStatus currentState = null;
    long start = 0;

    Activity activity;
    BlockingQueue<Bitmap> bitmapArrayBlockingQueue;
    RecordableSurfaceView mRecordableSurfaceView;

    RelativeLayout mRecordBtn;

    RelativeLayout loadingLayout;

    LinearLayout mCircle;

    LinearLayout mSquare;

    FloatingActionButton btnSwap;

    FloatingActionButton fabLastVid;

    ScanSettings settings;
    Bitmap bitmapImage = null;

    Uri lastRecordedFile = null;
    private ActivityRsvBinding binding;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        compositeDisposable = new CompositeDisposable();
        if (bundle != null) {
            this.settings = bundle.getParcelable(Constants.FRAGMENT_INPUT_KEY);
        } else {
            this.settings = new ScanSettings();
        }
        activity = getActivity();
        mCameraToUse = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.CAMERA_ID, mCameraToUse);
        binding = ActivityRsvBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        mRecordableSurfaceView = binding.surfaceView;
        mRecordBtn = binding.btnRecord;
        loadingLayout = binding.loadingLayout;
        mCircle = binding.circleShape;
        mSquare = binding.squareButton;
        btnSwap = binding.fabSwap;
        fabLastVid = binding.fabLastVid;

        mRecordBtn.setOnClickListener(v -> onClickRecord());
        btnSwap.setOnClickListener(v -> onSwapCamera());
        fabLastVid.setOnClickListener(v -> onGalleryClick());

        createRxSubject();
        android.graphics.Point size = new android.graphics.Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = (int) (size.x * 16.0 / 9);
        //FileLogger.appendLog(this.getClass().getName(), "onCreateView", screenWidth + " " + screenHeight);
        if (mVideoRenderer == null) {
            mVideoRenderer = new VideoRenderer(activity);
        }
        mRecordableSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState == RecordingStatus.Start || currentState == RecordingStatus.Restart) {
                    mIsRecordingSubject.onNext(RecordingStatus.Pause);
                } else if (currentState == RecordingStatus.Pause) {
                    mIsRecordingSubject.onNext(RecordingStatus.Restart);
                }
            }
        });
        getImage();
        return view;
    }

    private void getImage() {
        String path = settings.getImagePath();
        if (settings.isBackgroundFilter() && path != null && path != "") {
            bitmapImage = null;
            try {
                Bitmap temp = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), Uri.parse(path));
                bitmapImage = Bitmap.createScaledBitmap(temp,
                        screenWidth,
                        screenHeight,
                        true);
                settings.setBackgroundImage(bitmapImage);
            } catch (IOException e) {
                Log.d("myerror", e.getMessage());
            }
        }
    }

    private void createRxSubject() {
        mIsRecordingSubject = BehaviorSubject.create();
        mIsRecordingObservable = mIsRecordingSubject;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //setupRecorder();
    }

    private void setupRecorder() {
        if (mVideoRenderer == null) {
            mVideoRenderer = new VideoRenderer(activity);
        }
        mRecordableSurfaceView.setWidthHeight(screenWidth, screenHeight);
        //FileLogger.appendLog(this.getClass().getName(), "setupRecorder", screenWidth + " " + screenHeight);
        mRecordableSurfaceView.resume();
        mOutputFile = getVideoFile();
        try {
            mRecordableSurfaceView.initRecorder(mOutputFile, screenWidth, screenHeight, null, null);
            //FileLogger.appendLog(this.getClass().getName(), "setupRecorder - initRecorder", screenWidth + " " + screenHeight);
        } catch (Exception ioex) {
            FirebaseCrashlytics.getInstance().recordException(ioex);
        }
        setVideoRenderer(mVideoRenderer);
    }

    public void onClickRecord() {
        toggleRecording(false);
    }

    public void onSwapCamera() {
        if (mCameraSetupInProgress) return;
        swapCamera();
    }

    public void onGalleryClick() {
        if (lastRecordedFile != null) {
            FragmentManager fm = this.getChildFragmentManager();
            VideoPlayerDialogFragment videoPlayerDialogFragment = VideoPlayerDialogFragment.newInstance(lastRecordedFile.toString());
            videoPlayerDialogFragment.setListener(this);
            videoPlayerDialogFragment.show(fm, "fragment_edit_name");
        } else {
            Toast.makeText(activity, "Please record a video first to preview.", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRecording(boolean isVoiceCommand) {
        if (mdisableClick) return;
        mdisableClick = true;
        if (mIsRecording) {
            stopRecording(isVoiceCommand);
        } else {
            startRecording(isVoiceCommand);
        }
    }

    private void startRecording(boolean isVoiceCommand) {
        try {
            boolean status = mRecordableSurfaceView.startRecording();
            if (status) {
                mSquare.setVisibility(View.VISIBLE);
                mCircle.setVisibility(View.INVISIBLE);
                fabLastVid.setVisibility(View.INVISIBLE);
                btnSwap.setVisibility(View.INVISIBLE);
                mIsRecording = true;
                if (!isVoiceCommand) {
                    mIsRecordingSubject.onNext(RecordingStatus.Start);
                }
            } else {
                Toast.makeText(activity, "Oops, device could be incompatible, please try again!", Toast.LENGTH_SHORT).show();
            }
        } finally {
            mdisableClick = false;
        }
    }

    private void stopRecording(boolean isVoiceCommand) {
        try {
            mSquare.setVisibility(View.INVISIBLE);
            mCircle.setVisibility(View.VISIBLE);
            fabLastVid.setVisibility(View.VISIBLE);
            btnSwap.setVisibility(View.VISIBLE);
            mRecordableSurfaceView.stopRecording();
            if (!isVoiceCommand) {
                mIsRecordingSubject.onNext(RecordingStatus.Stop);
            }
            addVideoToGallery(mOutputFile);

            mOutputFile = getVideoFile();
            mRecordableSurfaceView.initRecorder(mOutputFile, screenWidth, screenHeight, null, null);
        } catch (IOException ioex) {
            Log.e(TAG, "Couldn't re-init recording", ioex);
        } finally {
            mdisableClick = false;
            mIsRecording = false;
        }
    }

    private File getVideoFile() {
        String filename = TEST_VIDEO_FILE_NAME + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
        File directory;
        if (isExternalStorageWritable()) {
            directory = activity.getExternalCacheDir();
        } else {
            directory = activity.getFilesDir();
        }
        try {
            directory.mkdirs();
        } catch (Exception ex) {
        }
        currentFile = new File(directory, filename);
        return currentFile;
    }

    private void addVideoToGallery(File videoFile) {
        try {
            Uri uriSavedVideo;
            File createdvideo = null;
            ContentResolver resolver = activity.getContentResolver();

            String path = videoFile.getAbsolutePath();
            String videoFileName = path.substring(path.lastIndexOf("/") + 1);
            ContentValues valuesvideos;
            valuesvideos = new ContentValues();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + activity.getString(R.string.app_name));
                valuesvideos.put(MediaStore.Video.Media.TITLE, videoFileName);
                valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
                valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                uriSavedVideo = resolver.insert(collection, valuesvideos);
            } else {
                String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Environment.DIRECTORY_MOVIES;
                createdvideo = new File(directory, videoFileName);
                valuesvideos.put(MediaStore.Video.Media.TITLE, videoFileName);
                valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
                valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                valuesvideos.put(MediaStore.Video.Media.DATA, createdvideo.getAbsolutePath());
                uriSavedVideo = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
                valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 1);
            }

            ParcelFileDescriptor pfd;
            try {
                pfd = resolver.openFileDescriptor(uriSavedVideo, "w");
                FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());
                FileInputStream in = new FileInputStream(videoFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
                pfd.close();
                lastRecordedFile = uriSavedVideo;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                valuesvideos.clear();
                valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 0);
                resolver.update(uriSavedVideo, valuesvideos, null, null);
            }
        } catch (Exception ec) {
        }
    }
    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        createRxSubject();
        setupRecorder();
        startBackgroundThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        if (mIsRecording) {
            stopRecording(false);
        }
        stopBackgroundThread();
        mRecordableSurfaceView.pause();
        mRecordableSurfaceView.setRendererCallbacks(null);
        mVideoRenderer.onSurfaceDestroyed();
        mVideoRenderer = null;
        compositeDisposable.clear();
    }

    public void setVideoRenderer(VideoRenderer videoRenderer) {
        mVideoRenderer = videoRenderer;
        if (mVideoRenderer == null) {
            return;
        }
        mVideoRenderer.setScanSettings(settings);
        mVideoRenderer.setVideoFragment(this);
        mVideoRenderer.setOnRendererReadyListener(this);
        mRecordableSurfaceView.setRendererCallbacks(mVideoRenderer);
        mVideoRenderer.onSurfaceChanged(screenWidth, screenHeight);
        mVideoRenderer.setRecordingObservable(mIsRecordingObservable);
        //FileLogger.appendLog(this.getClass().getName(), "setVideoRenderer", screenWidth + " " + screenHeight);
    }

    private void startBackgroundThread() {
        bitmapArrayBlockingQueue =
                new ArrayBlockingQueue<Bitmap>(1);
        setupConsumer();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mPreviewSize = new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mDeviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mBackgroundThread2 = new HandlerThread("CameraBackground2");
        mBackgroundThread2.start();
        mBackgroundHandler2 = new Handler(mBackgroundThread2.getLooper());
    }

    private void showToast() {
        int show = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, 0);
        if(show < 3) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("To pause or restart the scanner while recording, tap on the screen.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialoginterface, int i) {
                    dialoginterface.cancel();
                }
            });
            builder.setPositiveButton("Don't show again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialoginterface, int i) {
                    SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, 5);
                    dialoginterface.cancel();
                }
            });
            show++;
            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, show);
            builder.show();
        }
    }

    private void stopBackgroundThread() {
        Log.e(TAG, "RELEASE TEXTURE");
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
            mSurfaces.clear();
        }
    }

    public void swapCamera() {
        mCameraSetupInProgress = true;
        closeCamera();
        if (mCameraToUse == CAMERA_FORWARD) {
            mCameraToUse = CAMERA_PRIMARY;
        } else {
            mCameraToUse = CAMERA_FORWARD;
        }
        SharedPreferencesManager.setInt(activity, SharedPreferencesManager.CAMERA_ID, mCameraToUse);
        openCamera();
    }

    /**
     * Tries to open a CameraDevice. The result is listened by `mStateCallback`.
     */
    @SuppressLint("MissingPermission")
    public void openCamera() {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        //sometimes openCamera gets called multiple times, so lets not get stuck in our semaphore lock
        if (mCameraDevice != null && mCameraIsOpen) {
            return;
        }

        final CameraManager manager = (CameraManager) activity
                .getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String[] cameraList = manager.getCameraIdList();

            //make sure we dont get array out of bounds error, default to primary [0] if thats the case
            if (mCameraToUse >= cameraList.length) {
                mCameraToUse = CAMERA_PRIMARY;
            }
            String cameraId = getFrontFacingCameraId(cameraList, manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            fpsRange = getRange(characteristics);
            mCameraRotation = getJpegOrientation(characteristics, mDeviceRotation);

            manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            e.printStackTrace();
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
            new VideoFragment.ErrorDialog().show(getFragmentManager(), "dialog");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    String getFrontFacingCameraId(String[] cameraList, CameraManager manager) throws CameraAccessException {
        int cam = CameraCharacteristics.LENS_FACING_FRONT;
        isFlipped = true;
        if (mCameraToUse == CAMERA_PRIMARY) {
            cam = CameraCharacteristics.LENS_FACING_BACK;
            isFlipped = false;
        }
        for (final String cameraId : cameraList) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == cam) return cameraId;
        }
        return cameraList[0];
    }

    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback
            = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            mPreviewSession = cameraCaptureSession;
            Log.e(TAG, "CaptureSession Configured: " + cameraCaptureSession);
            updatePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Activity activity = getActivity();
            Log.e(TAG, "config failed: " + cameraCaptureSession);
            if (null != activity) {
                Toast.makeText(activity, "CaptureSession Config Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.e(TAG, "onClosed: " + session);
        }
    };

    private static Size getClosestSupportedSize(List<Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return Collections.min(supportedSizes, new Comparator<Size>() {

            private int diff(final Size size) {
                return Math.abs(requestedWidth - size.getWidth()) + Math.abs(requestedHeight - size.getHeight());
            }

            @Override
            public int compare(final Size lhs, final Size rhs) {
                return diff(lhs) - diff(rhs);
            }
        });
    }

    ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!firstLoad) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        loadingLayout.setVisibility(View.GONE);
                        mRecordBtn.setVisibility(View.VISIBLE);
                        showToast();
                    }
                });
                firstLoad = true;
            }
            Image image = reader.acquireLatestImage();
            Log.d("myApp", "onImageAvailable " + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            if (image == null) {
                return;
            }

            if (bitmapImage != null) {
                image.close();
                mVideoRenderer.setCurrentImage(bitmapImage);
                return;
            }
            start = System.currentTimeMillis();
            Bitmap bitmap = converter.yuvToRgb(image);
            image.close();

            try {
                bitmapArrayBlockingQueue.offer(bitmap, 0, TimeUnit.MICROSECONDS);
            } catch (InterruptedException e) {
                Log.d("myApp", e.getMessage());
            }
            Log.d("speed", "onImageAvailable end " + (System.currentTimeMillis() - start));
            //FilterFunction.mirror(imageReaderBitmap);
        }
    };

    @Override
    public void onClick(Video item) {

    }

    @Override
    public void share(Uri uri) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("video/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }

    class Consumer implements Runnable {
        private final BlockingQueue queue;

        Consumer(BlockingQueue q) {
            queue = q;
        }

        public void run() {
            try {
                while (true) {
                    consume(queue.take());
                }
            } catch (InterruptedException ex) {
            }
        }

        void consume(Object bitmap) {
            Bitmap image = (Bitmap) bitmap;
            long start = System.currentTimeMillis();
            image = converter.rotate(image, mCameraRotation);
            Log.d("flip", "rotate " + (System.currentTimeMillis() - start));
            if (isFlipped) {
                image = converter.flip(image);
            }
            Log.d("flip", "flip " + (System.currentTimeMillis() - start));
            if (mVideoRenderer != null) {
                mVideoRenderer.setCurrentImage(image);
            }
        }
    }

    void setupConsumer() {
        Consumer c = new Consumer(bitmapArrayBlockingQueue);
        Thread thread = new Thread(c);
        thread.start();
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            mCameraIsOpen = true;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraIsOpen = false;
            Log.e(TAG, "DISCONNECTED FROM CAMERA");

        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraIsOpen = false;

            Log.e(TAG, "CameraDevice.StateCallback onError() " + error);

            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * close camera when not in use/pausing/leaving
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                if (mPreviewSession != null) {
                    mPreviewSession.stopRepeating();
                }
                mCameraDevice.close();
                mCameraDevice = null;
                mCameraIsOpen = false;
            }
        } catch (Exception ioex) {
            FirebaseCrashlytics.getInstance().recordException(ioex);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private List<Surface> mSurfaces;
    Range<Integer> fpsRange;

    private void startPreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (mSurfaces == null) {
                mSurfaces = new ArrayList<>();
            }

            if (mPreviewTexture == -1) {
                mPreviewTexture = mVideoRenderer.getCameraTexture();
            }
            assert mPreviewTexture != -1;

            mSurfaceTexture = new SurfaceTexture(mPreviewTexture);
            mVideoRenderer.setSurfaceTexture(mSurfaceTexture);
            if (mSurfaces.size() != 0) {
                for (Surface sf : mSurfaces) {
                    sf.release();
                }
                mSurfaces.clear();
            }

            final CameraManager manager = (CameraManager) activity
                    .getSystemService(Context.CAMERA_SERVICE);

            String[] cameraList = manager.getCameraIdList();

            String cameraId = cameraList[mCameraToUse];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //Size preViewSize = getOptimalPreviewSize(
            //         streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
            //         screenWidth, screenHeight);

            Size preViewSize = chooseOptimalSize(
                    streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                    screenHeight, screenWidth, screenHeight, screenWidth, new Size(screenHeight, screenWidth));

            if (preViewSize == null) {
                Toast.makeText(activity, "Opps, your camera is incompatible!", Toast.LENGTH_LONG).show();
                return;
            }
            mSurfaceTexture.setDefaultBufferSize(preViewSize.getWidth(), preViewSize.getHeight());

            converter = new YuvToRgbConverter(getContext());

            mImageReader = ImageReader.newInstance(preViewSize.getWidth(), preViewSize.getHeight(),
                    ImageFormat.YUV_420_888, /*maxImages*/1);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler2);

            mVideoRenderer.setScreenSize(preViewSize.getHeight(), preViewSize.getWidth());

            float screenAspect = mRecordableSurfaceView.getWidth() * 1.0f / mRecordableSurfaceView.getHeight();
            float previewAspect = preViewSize.getHeight() * 1.0f / preViewSize.getWidth();

            float screenToTextureAspectRatio = screenAspect / previewAspect;
            mVideoRenderer.setAspectRatio(1);

            Surface readerSurface = mImageReader.getSurface();
            mSurfaces.add(readerSurface);
            Surface previewSurface = new Surface(mSurfaceTexture);
            mSurfaces.add(previewSurface);

            mPreviewBuilder.addTarget(readerSurface);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(mSurfaces, mCaptureSessionStateCallback,
                    mBackgroundHandler);

            mIsRecordingSubject.subscribe(state -> {
                currentState = state;
            }, throwable -> {
                Log.d("myApp", throwable.getMessage());
            });
            mCameraSetupInProgress = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);//This line of code is used for adjusting the fps range and fixing the dark preview
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

            mSurfaceTexture.setOnFrameAvailableListener(mVideoRenderer);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Range<Integer> getRange(CameraCharacteristics chars) {
        Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        Range<Integer> result = null;

        for (Range<Integer> range : ranges) {
            int upper = range.getUpper();

            // 10 - min range upper for my needs
            if (upper >= 29) {
                if (result == null || upper < result.getUpper().intValue()) {
                    result = range;
                }
            }
        }
        return result;
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    public void setRecordingObservable(Observable<RecordingStatus> recordingObservable) {
        mIsRecordingObservable = recordingObservable;
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.001;
        double targetRatio = (double) w / h;
        List<Size> allSizes = Arrays.asList(sizes);

        Collections.sort(allSizes, new CompareSizesByArea());

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : allSizes) {
            double ratio = (double) size.getWidth() / size.getHeight();

            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.getWidth() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getWidth() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : allSizes) {
                if (Math.abs(size.getWidth() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getWidth() - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;
            for (Size size : choices) {
                if (Math.abs(size.getWidth() - textureViewHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getWidth() - textureViewHeight);
                }
            }
            return optimalSize == null ? choices[0] : optimalSize;
        }
    }

    /**
     * Get the current camera type. Either {@link #CAMERA_FORWARD} or {@link #CAMERA_PRIMARY}
     *
     * @return current camera type
     */
    public int getCurrentCameraType() {
        return mCameraToUse;
    }

    /**
     * Set which camera to use, defaults to {@link #CAMERA_PRIMARY}.
     *
     * @param camera_id can also be {@link #CAMERA_FORWARD} for forward facing, but use primary if
     *                  that fails.
     */
    public void setCameraToUse(int camera_id) {
        mCameraToUse = camera_id;
    }

    /**
     * Set the texture that we'll be drawing our camera preview to. This is created from our
     * TextureView
     * in our Renderer to be used with our shaders.
     */
    public void setPreviewTexture(int previewSurface) {
        this.mPreviewTexture = previewSurface;
    }


    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    @Override
    public void onRendererReady() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                openCamera();
            }
        });
    }

    @Override
    public void onRendererFinished() {

    }

    public VideoRenderer getVideoRenderer() {
        return mVideoRenderer;
    }

    /**
     * Listener interface that will send back the newly created {@link Size} of our camera output
     */
    public interface OnViewportSizeUpdatedListener {

        void onViewportSizeUpdated(int viewportWidth, int viewportHeight);
    }

    /**
     * Simple ErrorDialog for display
     */
    public static class ErrorDialog extends DialogFragment {

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage("This device doesn't support Camera2 API.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }
}
