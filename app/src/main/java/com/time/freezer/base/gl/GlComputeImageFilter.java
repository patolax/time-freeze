package com.time.freezer.base.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLUtils;

import com.time.freezer.filters.IImageFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for image filters implemented as an OpenGL ES 3.1 compute shader instead of
 * RenderScript or plain Java pixel loops. Handles the mechanics every such filter needs: getting
 * a current GL ES 3.1 context (see GlComputeContext), uploading the input Bitmap to a texture,
 * binding input/output textures for the compute shader's image2D uniforms, dispatching, and
 * reading the result back into a Bitmap - subclasses only provide the GLSL source, per-frame
 * uniform values, and the work-group dispatch size.
 * <p>
 * No texture/FBO/program state is shared between filter instances - each subclass instance owns
 * its own, created lazily on first use and reused across frames (resized only if the input
 * Bitmap's dimensions change, which in the live-recording path they normally don't).
 */
public abstract class GlComputeImageFilter extends IImageFilter {

    private int program = 0;
    private int inputTexture = 0;
    private int outputTexture = 0;
    private int fbo = 0;
    private int textureWidth = -1;
    private int textureHeight = -1;

    protected GlComputeImageFilter(Context context) {
        super(context);
    }

    /** GLSL ES 3.1 compute shader source (must declare its own local_size layout). */
    protected abstract String getComputeShaderSource();

    /** Set any filter-specific uniforms (image size is not passed automatically). */
    protected abstract void setUniforms(int program, int width, int height);

    /** Number of work groups to dispatch in X and Y (Z is always 1). */
    protected abstract int[] getWorkGroupCounts(int width, int height);

    @Override
    public Bitmap process(Bitmap bitmap) {
        GlComputeContext.ensureCurrent();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        ensureProgram();
        ensureTextures(width, height);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture);
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, bitmap);
        GlUtil.checkGlError("compute filter input upload");

        GLES31.glUseProgram(program);
        GLES31.glBindImageTexture(0, inputTexture, 0, false, 0,
                GLES31.GL_READ_ONLY, GLES30.GL_RGBA8);
        GLES31.glBindImageTexture(1, outputTexture, 0, false, 0,
                GLES31.GL_WRITE_ONLY, GLES30.GL_RGBA8);

        setUniforms(program, width, height);
        GlUtil.checkGlError("compute filter uniforms");

        int[] groups = getWorkGroupCounts(width, height);
        GLES31.glDispatchCompute(groups[0], groups[1], 1);
        GlUtil.checkGlError("compute filter dispatch");
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        return readBack(width, height);
    }

    private Bitmap readBack(int width, int height) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, outputTexture, 0);
        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
            throw new RuntimeException(getClass().getSimpleName()
                    + ": readback framebuffer incomplete, status=0x" + Integer.toHexString(status));
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
        GlUtil.checkGlError("compute filter readback");
        buffer.rewind();

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyPixelsFromBuffer(buffer);
        return out;
    }

    private void ensureProgram() {
        if (program != 0) return;
        int shader = GlUtil.loadShader(GLES31.GL_COMPUTE_SHADER, getComputeShaderSource());
        if (shader == 0) {
            throw new RuntimeException(getClass().getSimpleName() + ": compute shader failed to compile");
        }
        int newProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(newProgram, shader);
        GLES31.glLinkProgram(newProgram);
        int[] linkStatus = new int[1];
        GLES31.glGetProgramiv(newProgram, GLES31.GL_LINK_STATUS, linkStatus, 0);
        GLES31.glDeleteShader(shader);
        if (linkStatus[0] != GLES31.GL_TRUE) {
            String log = GLES31.glGetProgramInfoLog(newProgram);
            GLES31.glDeleteProgram(newProgram);
            throw new RuntimeException(getClass().getSimpleName() + ": program link failed: " + log);
        }
        program = newProgram;
    }

    private void ensureTextures(int width, int height) {
        if (inputTexture != 0 && width == textureWidth && height == textureHeight) {
            return;
        }
        releaseTextures();

        int[] tex = new int[2];
        GLES30.glGenTextures(2, tex, 0);
        inputTexture = tex[0];
        outputTexture = tex[1];

        // glBindImageTexture requires immutable-format storage (glTexStorage2D), not the mutable
        // glTexImage2D/GLUtils.texImage2D allocation path - using the latter causes
        // GL_INVALID_OPERATION when the texture is later bound as an image.
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, width, height);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTexture);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, width, height);
        GlUtil.checkGlError("compute filter texture setup");

        int[] fbos = new int[1];
        GLES30.glGenFramebuffers(1, fbos, 0);
        fbo = fbos[0];

        textureWidth = width;
        textureHeight = height;
    }

    private void releaseTextures() {
        if (inputTexture != 0 || outputTexture != 0) {
            GLES30.glDeleteTextures(2, new int[]{inputTexture, outputTexture}, 0);
            inputTexture = 0;
            outputTexture = 0;
        }
        if (fbo != 0) {
            GLES30.glDeleteFramebuffers(1, new int[]{fbo}, 0);
            fbo = 0;
        }
    }
}
