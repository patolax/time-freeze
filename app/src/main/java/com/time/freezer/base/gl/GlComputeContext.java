package com.time.freezer.base.gl;

import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;

/**
 * Lazily-created, process-lifetime EGL/GL ES 3.x context used to run compute-shader-based image
 * filters (see com.time.freezer.filters.GlComputeImageFilter). Unrelated to the GL ES 2.0 context
 * RecordableSurfaceView.ARRenderThread owns for the live camera preview - this context is never
 * used for drawing to a window, only for offscreen compute dispatch, so a 1x1 pbuffer surface is
 * enough to satisfy EGL's requirement that a context have a current surface.
 * <p>
 * VideoFragment's CameraX analysis executor is recreated on every onResume() (a fresh
 * single-thread ExecutorService each time), and onPause()'s shutdown() of the old one is
 * graceful, not immediate - so a fast enough pause/resume cycle (e.g. entering the camera screen
 * and hitting record before the old session's executor thread has fully drained) can hand off
 * process() calls to a genuinely different OS thread than the one this context was last made
 * current on. EGL contexts can only be current on one thread at a time, so {@link #ensureCurrent()}
 * calls eglMakeCurrent() on every invocation (cheap/idempotent if already current on the calling
 * thread) rather than only once at construction, to stay correct across that handoff.
 */
public final class GlComputeContext {
    private static final String TAG = "GlComputeContext";

    private static GlComputeContext instance;

    private final EglCore eglCore;
    private final EGLSurface pbufferSurface;

    private GlComputeContext() {
        eglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);
        if (eglCore.getGlVersion() < 3) {
            throw new RuntimeException("GlComputeContext: device/driver does not support GLES 3");
        }
        pbufferSurface = eglCore.createOffscreenSurface(1, 1);
        eglCore.makeCurrent(pbufferSurface);

        String version = GLES30.glGetString(GLES30.GL_VERSION);
        Log.d(TAG, "GL_VERSION=" + version);
        if (!supportsCompute(version)) {
            throw new RuntimeException("GlComputeContext: GLES 3.1+ required for compute shaders, got: " + version);
        }
    }

    /**
     * Parses an "OpenGL ES M.N ..." version string and checks for at least 3.1. Compute shaders
     * are only guaranteed available from GLES 3.1 onward.
     */
    private static boolean supportsCompute(String version) {
        if (version == null) return false;
        try {
            String[] parts = version.trim().split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+")) {
                    String[] mn = part.split("\\.");
                    int major = Integer.parseInt(mn[0]);
                    int minor = Integer.parseInt(mn[1]);
                    return major > 3 || (major == 3 && minor >= 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not parse GL_VERSION string: " + version, e);
        }
        return false;
    }

    /**
     * Ensures the shared compute context exists and is current on the calling thread. Call this
     * at the start of every process() invocation, not just once - see class doc.
     */
    public static synchronized void ensureCurrent() {
        if (instance == null) {
            instance = new GlComputeContext();
        } else {
            instance.eglCore.makeCurrent(instance.pbufferSurface);
        }
    }
}
