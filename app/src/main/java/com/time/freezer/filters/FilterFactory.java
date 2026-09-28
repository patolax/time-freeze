package com.time.freezer.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.android.renderscript.Toolkit;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.time.freezer.fragments.Constants;
import com.time.freezer.fragments.ScanSettings;

import java.lang.reflect.Constructor;

public class FilterFactory {

    IImageFilter filter;

    public IImageFilter createFilter(Context context, FilterItem filterItem) {
        try {
            Class<?> clazz = Class.forName(filterItem.getName());
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            filter = (IImageFilter) constructor.newInstance(context);
            filter.setIsFullImageFilter(filterItem.isFullImageFilter());
            return filter;
        } catch (Exception ex) {
            Log.d("myApp", "FilterManager: " + ex.getClass());
            FirebaseCrashlytics.getInstance().recordException(ex);
        }
        return new NoFilter(context);
    }

    public Bitmap applyFilter(Bitmap inputBitmap) {
        if (filter == null) {
            return null;
        }
        return filter.process(inputBitmap);
    }
}

// saturation like in photo edit
class SaturationModifyFilter extends IImageFilter {
    private final float mSaturationFactor;

    public SaturationModifyFilter(Context context) {
        super(context);
        mSaturationFactor = 2f;
    }

    public SaturationModifyFilter(Context context, float saturationFactor) {
        super(context);
        mSaturationFactor = saturationFactor;
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float saturation = mSaturationFactor + 1.0f;
        float negosaturation = 1.0f - saturation;
        float nego1 = negosaturation * 0.2126f;
        float nego2 = nego1 + saturation;
        float nego3 = negosaturation * 0.7152f;
        float nego4 = nego3 + saturation;
        float nego5 = negosaturation * 0.0722f;
        float nego6 = nego5 + saturation;

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float r = ((p >> 16) & 0xFF) / 255f;
            float g = ((p >> 8) & 0xFF) / 255f;
            float b = (p & 0xFF) / 255f;

            float outR = clamp01((r * nego2) + (g * nego3) + (b * nego5));
            float outG = clamp01((r * nego1) + (g * nego4) + (b * nego5));
            float outB = clamp01((r * nego1) + (g * nego3) + (b * nego6));

            pixels[i] = 0xFF000000
                    | (Math.round(outR * 255) << 16)
                    | (Math.round(outG * 255) << 8)
                    | Math.round(outB * 255);
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}

class NoiseFilter extends IImageFilter {
    private static final float INTENSITY = 0.2f;
    private final java.util.Random random = new java.util.Random();

    public NoiseFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float r = ((p >> 16) & 0xFF) / 255f + (random.nextFloat() * 2f - 1f) * INTENSITY;
            float g = ((p >> 8) & 0xFF) / 255f + (random.nextFloat() * 2f - 1f) * INTENSITY;
            float b = (p & 0xFF) / 255f + (random.nextFloat() * 2f - 1f) * INTENSITY;

            pixels[i] = 0xFF000000
                    | (Math.round(clamp01(r) * 255) << 16)
                    | (Math.round(clamp01(g) * 255) << 8)
                    | Math.round(clamp01(b) * 255);
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
};

class BlackWhiteFilter extends IImageFilter {

    public BlackWhiteFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return Toolkit.INSTANCE.colorMatrix(bitmap, Toolkit.INSTANCE.getGreyScaleColorMatrix());
    }
};

// RenderScript's Matrix4f is column-major; Toolkit.colorMatrix expects row-major, so this is the
// transpose of the original Matrix4f array (verified against Toolkit.greyScaleColorMatrix, whose
// documented layout only produces true greyscale under the transposed convention).
class SepiaFilter extends IImageFilter {
    private static final float[] MATRIX = {
            0.189f, 0.168f, 0.131f, 0f,
            0.769f, 0.686f, 0.534f, 0f,
            0.393f, 0.349f, 0.272f, 0f,
            0.000f, 0.000f, 0.000f, 1f
    };

    public SepiaFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return Toolkit.INSTANCE.colorMatrix(bitmap, MATRIX);
    }
};

class OldFilter extends IImageFilter {
    private static final float[] MATRIX = {
            0.393f, 0.769f, 0.189f, 0f,
            0.349f, 0.686f, 0.168f, 0f,
            0.272f, 0.534f, 0.131f, 0f,
            0.000f, 0.000f, 0.000f, 1f
    };

    public OldFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return Toolkit.INSTANCE.colorMatrix(bitmap, MATRIX);
    }
};

class InvertFilter extends IImageFilter {

    public InvertFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = 255 - ((p >> 16) & 0xFF);
            int g = 255 - ((p >> 8) & 0xFF);
            int b = 255 - (p & 0xFF);
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }
};

class LightFilter extends IImageFilter {
    private static final float LIGHT = 150.0f / 255.0f;

    public LightFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float radius = Math.min(halfWidth, halfHeight);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int p = pixels[i];
                float dx = x - halfWidth;
                float dy = y - halfHeight;
                float length = (float) Math.sqrt(dx * dx + dy * dy);

                if (length < radius) {
                    float pixel = LIGHT * (1.0f - length / radius);
                    float r = clamp01(((p >> 16) & 0xFF) / 255f + pixel);
                    float g = clamp01(((p >> 8) & 0xFF) / 255f + pixel);
                    float b = clamp01((p & 0xFF) / 255f + pixel);
                    pixels[i] = 0xFF000000
                            | (Math.round(r * 255) << 16)
                            | (Math.round(g * 255) << 8)
                            | Math.round(b * 255);
                } else {
                    pixels[i] = 0xFF000000 | (p & 0x00FFFFFF);
                }
            }
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
};

// like pixalate
class MosaicFilter extends IImageFilter {
    private static final int MOSAIC_SIZE = 40;

    public MosaicFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] out = new int[pixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (y % MOSAIC_SIZE == 0 && x % MOSAIC_SIZE == 0) {
                    out[i] = 0xFF000000 | (pixels[i] & 0x00FFFFFF);
                } else {
                    int anchorX = MOSAIC_SIZE * (x / MOSAIC_SIZE);
                    int anchorY = MOSAIC_SIZE * (y / MOSAIC_SIZE);
                    int anchorPixel = pixels[anchorY * width + anchorX];
                    out[i] = 0xFF000000 | (anchorPixel & 0x00FFFFFF);
                }
            }
        }

        Bitmap outBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        outBitmap.setPixels(out, 0, width, 0, 0, width, height);
        return outBitmap;
    }
};

/// ghost
class TintFilter extends IImageFilter {

    public TintFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float negR = 1.0f - ((p >> 16) & 0xFF) / 255f;
            float negG = 1.0f - ((p >> 8) & 0xFF) / 255f;
            float negB = 1.0f - (p & 0xFF) / 255f;
            float grey = negR * 0.2126f + negG * 0.7152f + negB * 0.0722f;
            int v = Math.round(grey * 255);
            pixels[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }
};


class OilPaintFilter extends IImageFilter {
    private static final int MODEL = 30;
    private final java.util.Random random = new java.util.Random();

    public OilPaintFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] out = new int[pixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pos = (random.nextInt(9999) + 1) % MODEL;
                int theX = (x + pos) < width ? (x + pos) : ((x - pos) >= 0 ? (x - pos) : x);
                int theY = (y + pos) < height ? (y + pos) : ((y - pos) >= 0 ? (y - pos) : y);
                int sample = pixels[theY * width + theX];
                out[y * width + x] = 0xFF000000 | (sample & 0x00FFFFFF);
            }
        }

        Bitmap outBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        outBitmap.setPixels(out, 0, width, 0, 0, width, height);
        return outBitmap;
    }
};

// looks like photo reel
class ColorQuantizeFilter extends IImageFilter {
    private static final float LEVELS = 5.0f;

    public ColorQuantizeFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = quantize((p >> 16) & 0xFF);
            int g = quantize((p >> 8) & 0xFF);
            int b = quantize(p & 0xFF);
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static int quantize(int channel) {
        float f = channel / 255f;
        int level = (int) (f * LEVELS);
        return Math.round((level / LEVELS) * 255f);
    }
};

// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). FilterFactory only ever
// instantiates the default (Context)-arg constructor via reflection, so gTone/gSaturation are
// always the same fixed constants - HUE/SATURATION/LUM_MULTIPLIER below are the .rs kernel's
// setup() (RGBtoHLS(gTone) + the per-frame luminance-curve multiplier) precomputed once on the
// host in the exact same order of operations, rather than recomputed by every shader invocation.
// The per-pixel root() still needs the real HLStoRGB conversion since L varies per pixel.
class ColorToneFilter extends com.time.freezer.base.gl.GlComputeImageFilter {
    private static final float TONE_R = 0.1294f;
    private static final float TONE_G = 0.6588f;
    private static final float TONE_B = 0.9961f;
    private static final float SATURATION_PARAM = 0.7529f;

    private static final float HUE;
    private static final float SATURATION;
    private static final float LUM_MULTIPLIER;

    static {
        float cmax = Math.max(TONE_R, Math.max(TONE_G, TONE_B));
        float cmin = Math.min(TONE_R, Math.min(TONE_G, TONE_B));
        float l = (cmax + cmin) / 2f;
        float h;
        float s;
        if (cmax == cmin) {
            h = 0f;
            s = 0f;
        } else {
            float delta = cmax - cmin;
            s = (l < 0.5f) ? delta / (cmax + cmin) : delta / (2f - cmax - cmin);
            if (cmax == TONE_R) {
                h = (TONE_G - TONE_B) / delta;
            } else if (cmax == TONE_G) {
                h = 2f + (TONE_B - TONE_R) / delta;
            } else {
                h = 4f + (TONE_R - TONE_G) / delta;
            }
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        HUE = h;
        SATURATION = Math.min(s * (SATURATION_PARAM * SATURATION_PARAM), 1f);
        LUM_MULTIPLIER = 1f + (128f - Math.abs(SATURATION_PARAM * 255f - 128f)) / 128f / 9f;
    }

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 8, local_size_y = 8) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "uniform float uHue;\n" +
            "uniform float uSaturation;\n" +
            "uniform float uLumMultiplier;\n" +
            "float hlsValue(float n1, float n2, float h) {\n" +
            "    if (h > 6.0) h -= 6.0; else if (h < 0.0) h += 6.0;\n" +
            "    if (h < 1.0) return n1 + (n2 - n1) * h;\n" +
            "    if (h < 3.0) return n2;\n" +
            "    if (h < 4.0) return n1 + (n2 - n1) * (4.0 - h);\n" +
            "    return n1;\n" +
            "}\n" +
            "vec3 hlsToRgb(float h, float l, float s) {\n" +
            "    if (s == 0.0) return vec3(l, l, l);\n" +
            "    float m2 = (l > 0.5) ? (l + s - l * s) : (l * (1.0 + s));\n" +
            "    float m1 = 2.0 * l - m2;\n" +
            "    return vec3(hlsValue(m1, m2, h * 6.0 + 2.0), hlsValue(m1, m2, h * 6.0), hlsValue(m1, m2, h * 6.0 - 2.0));\n" +
            "}\n" +
            "void main() {\n" +
            "    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    if (pos.x >= uSize.x || pos.y >= uSize.y) return;\n" +
            "    vec3 rgbIn = imageLoad(uInput, pos).rgb;\n" +
            "    int idx = clamp(int((0.3 * rgbIn.r + 0.59 * rgbIn.g + 0.11 * rgbIn.b) * 255.0), 0, 255);\n" +
            "    float l = min((float(idx) / 255.0) * uLumMultiplier, 1.0);\n" +
            "    imageStore(uOutput, pos, vec4(hlsToRgb(uHue, l, uSaturation), 1.0));\n" +
            "}\n";

    public ColorToneFilter(Context context) {
        super(context);
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
        android.opengl.GLES31.glUniform1f(android.opengl.GLES31.glGetUniformLocation(program, "uHue"), HUE);
        android.opengl.GLES31.glUniform1f(android.opengl.GLES31.glGetUniformLocation(program, "uSaturation"), SATURATION);
        android.opengl.GLES31.glUniform1f(android.opengl.GLES31.glGetUniformLocation(program, "uLumMultiplier"), LUM_MULTIPLIER);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + 7) / 8, (height + 7) / 8};
    }
};


class ThreeDGridFilter extends IImageFilter {
    private final int mSize;
    private final float mDepth;

    public ThreeDGridFilter(Context context) {
        super(context);
        mSize = 16;
        mDepth = 100.0f / 255.0f;
    }

    public ThreeDGridFilter(Context context, int size, float depth) {
        super(context);
        mSize = Math.max(size, 1);
        mDepth = depth;
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int p = pixels[i];
                float d = 0;
                if (Math.floorMod(y - 1, mSize) == 0 && x % mSize > 0 && (x + 1) % mSize > 0) {
                    d = -mDepth; // top
                } else if ((y + 2) % mSize == 0 && x % mSize > 0 && (x + 1) % mSize > 0) {
                    d = mDepth; // bottom
                } else if (Math.floorMod(x - 1, mSize) == 0 && y % mSize > 0 && (y + 1) % mSize > 0) {
                    d = mDepth; // left
                } else if ((x + 2) % mSize == 0 && y % mSize > 0 && (y + 1) % mSize > 0) {
                    d = -mDepth; // right
                }

                float r = clamp01(((p >> 16) & 0xFF) / 255f + d);
                float g = clamp01(((p >> 8) & 0xFF) / 255f + d);
                float b = clamp01((p & 0xFF) / 255f + d);
                pixels[i] = 0xFF000000
                        | (Math.round(r * 255) << 16)
                        | (Math.round(g * 255) << 8)
                        | Math.round(b * 255);
            }
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
};

class BlurFilter extends IImageFilter {

    public BlurFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return Toolkit.INSTANCE.blur(bitmap, 25);
    }
};

// thresholding = black and white
class ThresholdFilter extends IImageFilter {
    private final float mThreshold;

    public ThresholdFilter(Context context) {
        super(context);
        mThreshold = 0.5f;
    }

    public ThresholdFilter(Context context, float threshold) {
        super(context);
        mThreshold = threshold;
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float r = ((p >> 16) & 0xFF) / 255f;
            float g = ((p >> 8) & 0xFF) / 255f;
            float b = (p & 0xFF) / 255f;
            float grey = r * 0.2126f + g * 0.7152f + b * 0.0722f;
            int v = grey > mThreshold ? 255 : 0;
            pixels[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }
};

// swils the middle of photo, face look like alian
// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). Inverse-mapping bilinear
// sample from the source texture; the manual 4-tap blend (rather than GL's native bilinear
// sampler) intentionally matches the original's own hand-rolled clamp-to-edge + truncation
// behavior exactly. `position` is deliberately allowed to go negative near the image center -
// that sign flip is the actual pinch/swirl visual effect, not a bug - so it is NOT clamped.
class RadialDistortionFilter extends com.time.freezer.base.gl.GlComputeImageFilter {

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 8, local_size_y = 8) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "void main() {\n" +
            "    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    if (pos.x >= uSize.x || pos.y >= uSize.y) return;\n" +
            "    float radius = 0.5;\n" +
            "    float distortion = 1.5;\n" +
            "    int realXPos = uSize.x / 2;\n" +
            "    int realYPos = uSize.y / 2;\n" +
            "    float realRadius = float(min(uSize.x, uSize.y)) * radius;\n" +
            "    float dx = float(pos.x - realXPos);\n" +
            "    float dy = float(pos.y - realYPos);\n" +
            "    float position = 1.0 - sqrt(dx * dx + dy * dy) / realRadius;\n" +
            "    vec4 outColor;\n" +
            "    if (position > 0.0) {\n" +
            "        position = 1.0 - distortion * position * position;\n" +
            "        float pos1 = dx * position + float(realXPos);\n" +
            "        int x1 = int(pos1);\n" +
            "        float pos3 = pos1 - float(x1);\n" +
            "        int x2 = (pos3 > 0.0) ? x1 + 1 : x1;\n" +
            "        float pos2 = dy * position + float(realYPos);\n" +
            "        int y1 = int(pos2);\n" +
            "        float pos4 = pos2 - float(y1);\n" +
            "        int y2 = (pos4 > 0.0) ? y1 + 1 : y1;\n" +
            "        x1 = clamp(x1, 0, uSize.x - 1);\n" +
            "        x2 = clamp(x2, 0, uSize.x - 1);\n" +
            "        y1 = clamp(y1, 0, uSize.y - 1);\n" +
            "        y2 = clamp(y2, 0, uSize.y - 1);\n" +
            "        vec3 c1 = imageLoad(uInput, ivec2(x1, y1)).rgb;\n" +
            "        vec3 c2 = imageLoad(uInput, ivec2(x2, y1)).rgb;\n" +
            "        vec3 c3 = imageLoad(uInput, ivec2(x2, y2)).rgb;\n" +
            "        vec3 c4 = imageLoad(uInput, ivec2(x1, y2)).rgb;\n" +
            "        vec3 blended = c1 * (1.0 - pos4) * (1.0 - pos3) + c2 * (1.0 - pos4) * pos3\n" +
            "                     + c3 * pos4 * pos3 + c4 * pos4 * (1.0 - pos3);\n" +
            "        outColor = vec4(blended, 1.0);\n" +
            "    } else {\n" +
            "        outColor = vec4(imageLoad(uInput, pos).rgb, 1.0);\n" +
            "    }\n" +
            "    imageStore(uOutput, pos, outColor);\n" +
            "}\n";

    public RadialDistortionFilter(Context context) {
        super(context);
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + 7) / 8, (height + 7) / 8};
    }
};

class BigBrotherFilter extends IImageFilter {
    private static final int DOT_AREA = 10;
    private static final int[] DITHER = {
            167, 200, 230, 216, 181, 94, 72, 193, 242, 232,
            36, 52, 222, 167, 200, 181, 126, 210, 94, 72,
            232, 153, 111, 36, 52, 167, 200, 230, 216, 181,
            94, 72, 193, 242, 232, 36, 52, 222, 167, 200,
            181, 126, 210, 94, 72, 232, 153, 111, 36, 52,
            167, 200, 230, 216, 181, 94, 72, 193, 242, 232,
            36, 52, 222, 167, 200, 181, 126, 210, 94, 72,
            232, 153, 111, 36, 52, 167, 200, 230, 216, 181,
            94, 72, 193, 242, 232, 36, 52, 222, 167, 200,
            181, 126, 210, 94, 72, 232, 153, 111, 36, 52
    };

    public BigBrotherFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int p = pixels[i];
                float b = (p & 0xFF) / 255f;
                int index = (y % DOT_AREA) * DOT_AREA + (x % DOT_AREA);
                int grayIntensity = (int) ((1.0f - b) * 255);
                int v = grayIntensity > DITHER[index] ? 0 : 255;
                pixels[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }
};

// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). The original .rs kernel used
// invoke_process with nested loops that, worked through algebraically, always copy each pixel to
// its own (x,y) except for a band-fill region - i.e. despite looking like a whole-allocation
// operation, it's actually a pure per-pixel decision with no cross-pixel dependency.
class BannerFilter extends com.time.freezer.base.gl.GlComputeImageFilter {
    private boolean mIsHorizontal;

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 8, local_size_y = 8) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "uniform int uIsHorizontal;\n" +
            "void main() {\n" +
            "    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    if (pos.x >= uSize.x || pos.y >= uSize.y) return;\n" +
            "    vec4 color;\n" +
            "    if (uIsHorizontal == 1) {\n" +
            "        int dh = uSize.y / 10;\n" +
            "        int threshold = int(floor(float(dh - 1) / 1.1));\n" +
            "        if (pos.y >= 10 * dh) {\n" +
            "            color = imageLoad(uInput, pos);\n" +
            "        } else {\n" +
            "            int rowInBand = pos.y % dh;\n" +
            "            color = (rowInBand <= threshold) ? imageLoad(uInput, pos) : vec4(0.7969, 0.7969, 0.7969, 1.0);\n" +
            "        }\n" +
            "    } else {\n" +
            "        int dw = uSize.x / 10;\n" +
            "        int threshold = int(floor(float(dw - 1) / 1.1));\n" +
            "        if (pos.x >= 10 * dw) {\n" +
            "            color = imageLoad(uInput, pos);\n" +
            "        } else {\n" +
            "            int colInBand = pos.x % dw;\n" +
            "            color = (colInBand <= threshold) ? imageLoad(uInput, pos) : vec4(0.7969, 0.7969, 0.7969, 1.0);\n" +
            "        }\n" +
            "    }\n" +
            "    imageStore(uOutput, pos, color);\n" +
            "}\n";

    public BannerFilter(Context context) {
        super(context);
        mIsHorizontal = true;
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        mIsHorizontal = direction != Constants.SCAN_DIRECTION_HORIZONTAL;
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
        android.opengl.GLES31.glUniform1i(android.opengl.GLES31.glGetUniformLocation(program, "uIsHorizontal"), mIsHorizontal ? 1 : 0);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + 7) / 8, (height + 7) / 8};
    }
};

// balck with white edges.
// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). FilterFactory only ever
// instantiates the default (Context)-arg constructor via reflection, so gDoGrayConversion=true,
// gDoInversion=true, and the .rs file's own defaults for gThreshold/gK00/gK01/gK02 (0.25/1/2/1,
// standard Sobel weights - _process() never sets these) are the only path this shader needs to
// implement; the gDoGrayConversion=false (per-channel, no grayscale) branch is dead in practice.
class ParamEdgeDetectFilter extends com.time.freezer.base.gl.GlComputeImageFilter {

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 8, local_size_y = 8) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "float grayscale(vec3 c) {\n" +
            "    return c.r * 0.2126 + c.g * 0.7152 + c.b * 0.0722;\n" +
            "}\n" +
            "ivec2 clampPos(ivec2 p) {\n" +
            "    return clamp(p, ivec2(0), uSize - ivec2(1));\n" +
            "}\n" +
            "void main() {\n" +
            "    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    if (pos.x >= uSize.x || pos.y >= uSize.y) return;\n" +
            "    float g1 = grayscale(imageLoad(uInput, clampPos(pos + ivec2(-1, -1))).rgb);\n" +
            "    float g2 = grayscale(imageLoad(uInput, clampPos(pos + ivec2( 0, -1))).rgb);\n" +
            "    float g3 = grayscale(imageLoad(uInput, clampPos(pos + ivec2( 1, -1))).rgb);\n" +
            "    float g4 = grayscale(imageLoad(uInput, clampPos(pos + ivec2(-1,  0))).rgb);\n" +
            "    float g5 = grayscale(imageLoad(uInput, clampPos(pos + ivec2( 1,  0))).rgb);\n" +
            "    float g6 = grayscale(imageLoad(uInput, clampPos(pos + ivec2(-1,  1))).rgb);\n" +
            "    float g7 = grayscale(imageLoad(uInput, clampPos(pos + ivec2( 0,  1))).rgb);\n" +
            "    float g8 = grayscale(imageLoad(uInput, clampPos(pos + ivec2( 1,  1))).rgb);\n" +
            "    float colorSum1 = g1 + 2.0 * g2 + g3 - g6 - 2.0 * g7 - g8;\n" +
            "    float colorSum2 = g1 - g3 + 2.0 * g4 - 2.0 * g5 + g6 - g8;\n" +
            "    float mag2 = colorSum1 * colorSum1 + colorSum2 * colorSum2;\n" +
            "    float color = (mag2 > 0.25) ? 1.0 : 0.0;\n" +
            "    imageStore(uOutput, pos, vec4(color, color, color, 1.0));\n" +
            "}\n";

    public ParamEdgeDetectFilter(Context context) {
        super(context);
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + 7) / 8, (height + 7) / 8};
    }
};

// like a poster
class PosterizeFilter extends IImageFilter {
    private final int mLevel;
    private final int[] mLut = new int[256];

    public PosterizeFilter(Context context) {
        super(context);
        mLevel = 10;
        buildLut();
    }

    public PosterizeFilter(Context context, int level) {
        super(context);
        mLevel = level;
        buildLut();
    }

    private void buildLut() {
        int level = Math.max(mLevel, 2);
        float d = 255.0f / (level - 1.0f);
        for (int i = 0; i <= 255; i++) {
            int n = (int) (i / d + 0.5f);
            int v = Math.round(d * n);
            mLut[i] = Math.max(0, Math.min(255, v));
        }
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Matches the original kernel's boundary check: the last row/column pass through unfiltered.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int p = pixels[i];
                if (x < width - 1 && y < height - 1) {
                    int r = mLut[(p >> 16) & 0xFF];
                    int g = mLut[(p >> 8) & 0xFF];
                    int b = mLut[p & 0xFF];
                    pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                } else {
                    pixels[i] = 0xFF000000 | (p & 0x00FFFFFF);
                }
            }
        }

        Bitmap out = Bitmap.createBitmap(width, height, bitmap.getConfig());
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }
};

// perfect reflection horizontal
// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). The original .rs kernel's
// mirror-fold, with its always-0.5 gOffset, reduces algebraically to a pure per-pixel coordinate
// remap with no cross-pixel dependency.
class ReflectionFilter extends com.time.freezer.base.gl.GlComputeImageFilter {
    private boolean mIsHorizontal;

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 8, local_size_y = 8) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "uniform int uIsHorizontal;\n" +
            "void main() {\n" +
            "    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    if (pos.x >= uSize.x || pos.y >= uSize.y) return;\n" +
            "    ivec2 srcPos = pos;\n" +
            "    if (uIsHorizontal == 1) {\n" +
            "        int yOffset = uSize.y / 2;\n" +
            "        if (pos.y < yOffset) srcPos.y = 2 * yOffset - 1 - pos.y;\n" +
            "    } else {\n" +
            "        int xOffset = uSize.x / 2;\n" +
            "        if (pos.x < xOffset) srcPos.x = 2 * xOffset - 1 - pos.x;\n" +
            "    }\n" +
            "    imageStore(uOutput, pos, imageLoad(uInput, srcPos));\n" +
            "}\n";

    public ReflectionFilter(Context context) {
        super(context);
        mIsHorizontal = false;
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        mIsHorizontal = direction != Constants.SCAN_DIRECTION_HORIZONTAL;
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
        android.opengl.GLES31.glUniform1i(android.opengl.GLES31.glGetUniformLocation(program, "uIsHorizontal"), mIsHorizontal ? 1 : 0);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + 7) / 8, (height + 7) / 8};
    }
};

// Ported to a GL ES 3.1 compute shader (see GlComputeImageFilter). The original .rs kernel's
// "average" is actually a strictly-ordered sequential halving recurrence
// (avg = (avg + next) / 2, column-major x-outer/y-inner scan order over each 20x20 block) -
// not a true mean, and not associative, so it can't be computed as a parallel reduction.
// Per an explicit product decision, this is replicated exactly (including the float32
// precision quirk where only roughly the last ~24 of 400 samples meaningfully affect the
// result) rather than "fixed" to a real average, since this migration's job is behavioral
// parity. Dispatched one compute invocation per 20x20 output block (local_size 1x1) rather
// than per-pixel, so each invocation can run the full sequential fold internally before
// filling its own block. Unlike the original kernel, out-of-bounds reads at partial edge
// blocks are skipped rather than read anyway (the original's unconditional rsGetElementAt
// before its bounds check is undefined behavior in RS, not part of the algorithm worth
// preserving).
class PixelateFilter extends com.time.freezer.base.gl.GlComputeImageFilter {
    private static final int SQUARE_SIZE = 20;

    private static final String SHADER =
            "#version 310 es\n" +
            "layout(local_size_x = 1, local_size_y = 1) in;\n" +
            "layout(rgba8, binding = 0) readonly uniform highp image2D uInput;\n" +
            "layout(rgba8, binding = 1) writeonly uniform highp image2D uOutput;\n" +
            "uniform ivec2 uSize;\n" +
            "uniform int uSquareSize;\n" +
            "void main() {\n" +
            "    ivec2 blockPos = ivec2(gl_GlobalInvocationID.xy);\n" +
            "    int ax = blockPos.x * uSquareSize;\n" +
            "    int ay = blockPos.y * uSquareSize;\n" +
            "    if (ax >= uSize.x || ay >= uSize.y) return;\n" +
            "    float red = -1.0;\n" +
            "    float green = -1.0;\n" +
            "    float blue = -1.0;\n" +
            "    for (int dx = 0; dx < uSquareSize; dx++) {\n" +
            "        int x = ax + dx;\n" +
            "        for (int dy = 0; dy < uSquareSize; dy++) {\n" +
            "            int y = ay + dy;\n" +
            "            if (x < uSize.x && y < uSize.y) {\n" +
            "                vec3 s = imageLoad(uInput, ivec2(x, y)).rgb;\n" +
            "                red   = (red   < 0.0) ? s.r : (red   + s.r) / 2.0;\n" +
            "                green = (green < 0.0) ? s.g : (green + s.g) / 2.0;\n" +
            "                blue  = (blue  < 0.0) ? s.b : (blue  + s.b) / 2.0;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    vec4 color = vec4(red, green, blue, 1.0);\n" +
            "    for (int dx = 0; dx < uSquareSize; dx++) {\n" +
            "        int x = ax + dx;\n" +
            "        if (x >= uSize.x) continue;\n" +
            "        for (int dy = 0; dy < uSquareSize; dy++) {\n" +
            "            int y = ay + dy;\n" +
            "            if (y >= uSize.y) continue;\n" +
            "            imageStore(uOutput, ivec2(x, y), color);\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    public PixelateFilter(Context context) {
        super(context);
    }

    @Override
    protected String getComputeShaderSource() {
        return SHADER;
    }

    @Override
    protected void setUniforms(int program, int width, int height) {
        android.opengl.GLES31.glUniform2i(android.opengl.GLES31.glGetUniformLocation(program, "uSize"), width, height);
        android.opengl.GLES31.glUniform1i(android.opengl.GLES31.glGetUniformLocation(program, "uSquareSize"), SQUARE_SIZE);
    }

    @Override
    protected int[] getWorkGroupCounts(int width, int height) {
        return new int[]{(width + SQUARE_SIZE - 1) / SQUARE_SIZE, (height + SQUARE_SIZE - 1) / SQUARE_SIZE};
    }
};

class FakeFilter extends IImageFilter {

    public FakeFilter(Context context) {
        super(context);
    }

    @Override
    public boolean isFakeFilter() {
        return true;
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        bitmap.eraseColor(Color.TRANSPARENT);
        return bitmap;
    }
};

class NoFilter extends IImageFilter {

    public NoFilter(Context context) {
        super(context);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return bitmap;
    }
};

class BackgroundImageFilter extends NoFilter {

    public BackgroundImageFilter(Context context) {
        super(context);
    }

    public boolean isBackgorundFilter() {
        return true;
    }
};