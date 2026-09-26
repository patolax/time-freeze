package com.time.freezer.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.time.freezer.R;
import com.time.freezer.base.utils.RecordingStatus;
import com.time.freezer.base.utils.SharedPreferencesManager;
import com.time.freezer.base.view.RecordableSurfaceView;
import com.time.freezer.base.gl.VideoRenderer;
import com.time.freezer.databinding.ActivityRsvBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Fragment for operating the camera, it doesnt have any UI elements, just controllers
 */
public class VideoFragment extends Fragment implements VideoRenderer.OnRendererReadyListener, OnGalleryClickListener {

    private static final String TAG = "VideoFragment";

    private static VideoFragment __instance;
    private VideoRenderer mVideoRenderer;
    private File mOutputFile;
    private boolean mIsRecording = false;
    private boolean mdisableClick = false;
    private CompositeDisposable compositeDisposable;
    private int mPreviewTexture = -1;
    private SurfaceTexture mSurfaceTexture;
    boolean firstLoad;
    boolean analysisLogged = false;

    Observable<RecordingStatus> mIsRecordingObservable;
    BehaviorSubject<RecordingStatus> mIsRecordingSubject;
    RecordingStatus currentState = null;

    public static final int CAMERA_PRIMARY = 0;
    public static final int CAMERA_FORWARD = 1;
    protected int mCameraToUse = CAMERA_FORWARD;
    boolean isFlipped = false;

    private ProcessCameraProvider mCameraProvider;
    private ExecutorService mCameraExecutor;

    public static final String TEST_VIDEO_FILE_NAME = "time_freezer";
    int screenWidth;
    int screenHeight;

    Activity activity;
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
        screenHeight = (int) (size.x * 16.0 / 9); // 16:9 hint for camera resolution selection
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
        if (mCameraProvider == null) return;
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
        return new File(directory, filename);
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
        mCameraExecutor = Executors.newSingleThreadExecutor();
        firstLoad = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraProvider != null) {
            mCameraProvider.unbindAll();
        }
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
            mCameraExecutor = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mIsRecording) {
            stopRecording(false);
        }
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

    public void swapCamera() {
        if (mCameraProvider == null) return;
        mCameraToUse = (mCameraToUse == CAMERA_FORWARD) ? CAMERA_PRIMARY : CAMERA_FORWARD;
        SharedPreferencesManager.setInt(activity, SharedPreferencesManager.CAMERA_ID, mCameraToUse);
        bindCameraUseCases();
    }

    private void bindCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                mCameraProvider = future.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraX provider error", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (mCameraProvider == null || mVideoRenderer == null || mCameraExecutor == null) return;

        CameraSelector cameraSelector = mCameraToUse == CAMERA_FORWARD
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        isFlipped = (mCameraToUse == CAMERA_FORWARD);

        // Bound both use cases to the SAME modest target via one shared ResolutionSelector.
        // Preview and ImageAnalysis negotiate resolutions independently; on legacy-level camera
        // devices they can otherwise diverge wildly (observed: 720x720 preview vs 2448x2448
        // analysis), which both stalls the analyzer (full-res bitmap work per frame) and breaks
        // the scanner's coordinate math (sized off preview, fed frames from analysis).
        int boundedWidth = Math.min(screenWidth, 720);
        int boundedHeight = Math.min(screenHeight, 1280);
        // The render pipeline maps the camera texture onto a fixed square quad (see
        // VideoRenderer's squareCoords/setAspectRatio(1) - unchanged from the original Camera1
        // code, which always passed a constant 1 here too). With no aspect correction happening
        // there, an unmatched camera stream aspect ratio shows up as stretch distortion. The old
        // Camera1 code avoided this via chooseOptimalSize() picking a stream matching the screen's
        // aspect ratio; do the CameraX equivalent by preferring 16:9 (== 9:16 once rotated to
        // portrait), which is what this device's screen ratio actually is.
        // CLOSEST_LOWER_THEN_HIGHER (not HIGHER_THEN_LOWER) - combined with the 16:9 aspect
        // preference, "closest higher" was jumping all the way to 1440x2560 (~4x the intended
        // pixel budget) since no closely-matching smaller 16:9 option existed, which tanked
        // per-frame analysis throughput (rotateAndFlip + filtering cost scales with pixel count).
        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(new ResolutionStrategy(new Size(boundedWidth, boundedHeight),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                .build();

        Preview preview = new Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build();
        preview.setSurfaceProvider(request -> {
            int textureId = mVideoRenderer.getCameraTexture();
            Size resolution = request.getResolution();
            SurfaceTexture surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
            mVideoRenderer.setSurfaceTexture(surfaceTexture);
            mVideoRenderer.setAspectRatio(1);
            Log.d(TAG, "preview res=" + resolution.getWidth() + "x" + resolution.getHeight());
            surfaceTexture.setOnFrameAvailableListener(mVideoRenderer);
            Surface surface = new Surface(surfaceTexture);
            request.provideSurface(surface, ContextCompat.getMainExecutor(requireContext()),
                    result -> surface.release());
        });

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Slicer/finalBitmap must be sized off the ACTUAL analysis frame (the bitmap the scanner
        // captures from), not Preview's resolution - the two use cases can and do end up with
        // different negotiated sizes. Set once, from the first frame.
        boolean[] scanSizeInitialized = {false};

        imageAnalysis.setAnalyzer(mCameraExecutor, imageProxy -> {
            if (!firstLoad && activity != null) {
                activity.runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    mRecordBtn.setVisibility(View.VISIBLE);
                    showToast();
                });
                firstLoad = true;
            }
            if (bitmapImage != null) {
                imageProxy.close();
                if (!scanSizeInitialized[0] && mVideoRenderer != null) {
                    scanSizeInitialized[0] = true;
                    mVideoRenderer.setScreenSize(bitmapImage.getWidth(), bitmapImage.getHeight());
                }
                if (mVideoRenderer != null) mVideoRenderer.setCurrentImage(bitmapImage);
                return;
            }
            Bitmap bitmap = imageProxy.toBitmap();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            imageProxy.close();
            bitmap = rotateAndFlip(bitmap, rotationDegrees, isFlipped);
            if (!analysisLogged) {
                analysisLogged = true;
                Log.d(TAG, "analysis res=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                        + " rot=" + rotationDegrees + " screenWH=" + screenWidth + "x" + screenHeight);
            }
            if (!scanSizeInitialized[0] && mVideoRenderer != null) {
                scanSizeInitialized[0] = true;
                mVideoRenderer.setScreenSize(bitmap.getWidth(), bitmap.getHeight());
            }
            if (mVideoRenderer != null) {
                mVideoRenderer.setCurrentImage(bitmap);
            }
        });

        try {
            mCameraProvider.unbindAll();
            mCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
        }

        mIsRecordingSubject.subscribe(
                state -> currentState = state,
                throwable -> Log.d("myApp", throwable.getMessage()));
    }

    // Rotates/mirrors without RenderScript (broken on Android 13+ / this device)
    private Bitmap rotateAndFlip(Bitmap src, int rotationDegrees, boolean mirror) {
        if (rotationDegrees == 0 && !mirror) return src;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(rotationDegrees);
        if (mirror) {
            matrix.postScale(-1, 1);
        }
        Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        if (out != src) {
            src.recycle();
        }
        return out;
    }

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

    public void setRecordingObservable(Observable<RecordingStatus> recordingObservable) {
        mIsRecordingObservable = recordingObservable;
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
        getActivity().runOnUiThread(() -> bindCamera());
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
