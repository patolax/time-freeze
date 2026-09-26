package com.time.freezer.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.Float3;
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
class SaturationModifyFilter extends RenderScriptImageFilter {
    private final float mSaturationFactor;
    ScriptC_SaturationModifyFilter script;

    public SaturationModifyFilter(Context context) {
        super(context);
        mSaturationFactor = 2f;
        script = new ScriptC_SaturationModifyFilter(mRS);
    }

    public SaturationModifyFilter(Context context, float saturationFactor) {
        super(context);
        script = new ScriptC_SaturationModifyFilter(mRS);
        mSaturationFactor = saturationFactor;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }

    @Override
    public void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gSaturationFactor(mSaturationFactor);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }
}

// super slow
class NoiseFilter extends RenderScriptImageFilter {
    private ScriptC_NoiseFilter script;

    public NoiseFilter(Context context) {
        super(context);
        script = new ScriptC_NoiseFilter(mRS);
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
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

class InvertFilter extends RenderScriptImageFilter {

    ScriptC_InvertFilter script;

    public InvertFilter(Context context) {
        super(context);
        script = new ScriptC_InvertFilter(mRS);
    }

    @Override
    protected final void _process() {

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class LightFilter extends RenderScriptImageFilter {

    ScriptC_LightFilter script;

    public LightFilter(Context context) {
        super(context);
        script = new ScriptC_LightFilter(mRS);
    }

    @Override
    protected final void _process() {

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// like pixalate
class MosaicFilter extends RenderScriptImageFilter {

    ScriptC_MosaicFilter script;

    public MosaicFilter(Context context) {
        super(context);
        script = new ScriptC_MosaicFilter(mRS);
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

/// ghost
class TintFilter extends RenderScriptImageFilter {

    ScriptC_TintFilter script;

    public TintFilter(Context context) {
        super(context);
        script = new ScriptC_TintFilter(mRS);
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};


// super slow
class OilPaintFilter extends RenderScriptImageFilter {

    ScriptC_OilPaintFilter script;

    public OilPaintFilter(Context context) {
        super(context);
        script = new ScriptC_OilPaintFilter(mRS);
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// looks like photo reel
class ColorQuantizeFilter extends RenderScriptImageFilter {

    ScriptC_ColorQuantizeFilter script;

    public ColorQuantizeFilter(Context context) {
        super(context);
        script = new ScriptC_ColorQuantizeFilter(mRS);
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class ColorToneFilter extends RenderScriptImageFilter {
    private final Float3 mRGB;

    private final float mSaturation;
    ScriptC_ColorToneFilter script;

    public ColorToneFilter(Context context) {
        super(context);
        script = new ScriptC_ColorToneFilter(mRS);
        mRGB = new Float3(0.1294f, 0.6588f, 0.9961f);
        mSaturation = 0.7529f;

    }

    public ColorToneFilter(Context context, Float3 rgb, float saturation) {
        super(context);
        script = new ScriptC_ColorToneFilter(mRS);
        mRGB = rgb;
        mSaturation = saturation;
    }

    @Override
    protected final void _process() {

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);
        script.set_gTone(mRGB);
        script.set_gSaturation(mSaturation);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};


class ThreeDGridFilter extends RenderScriptImageFilter {
    private final int mSize;
    private final float mDepth;
    ScriptC_ThreeDGridFilter script;

    public ThreeDGridFilter(Context context) {
        super(context);
        script = new ScriptC_ThreeDGridFilter(mRS);
        mSize = 16;
        mDepth = 100.0f / 255.0f;
    }

    public ThreeDGridFilter(Context context, int size, float depth) {
        super(context);
        script = new ScriptC_ThreeDGridFilter(mRS);
        mSize = size;
        mDepth = depth;
    }

    @Override
    protected final void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);
        script.set_gSize(mSize);
        script.set_gDepth(mDepth);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
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
class ThresholdFilter extends RenderScriptImageFilter {
    private final float mThreshold;
    ScriptC_ThresholdFilter script;

    public ThresholdFilter(Context context) {
        super(context);
        script = new ScriptC_ThresholdFilter(mRS);
        mThreshold = 0.5f;
    }

    public ThresholdFilter(Context context, float threshold) {
        super(context);
        script = new ScriptC_ThresholdFilter(mRS);
        mThreshold = threshold;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);
        script.set_gThreshold(mThreshold);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// swils the middle of photo, face look like alian
class RadialDistortionFilter extends RenderScriptImageFilter {

    ScriptC_RadialDistortionFilter script;

    public RadialDistortionFilter(Context context) {
        super(context);
        script = new ScriptC_RadialDistortionFilter(mRS);
        ;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class BigBrotherFilter extends RenderScriptImageFilter {

    ScriptC_BigBrotherFilter script;

    public BigBrotherFilter(Context context) {
        super(context);
        script = new ScriptC_BigBrotherFilter(mRS);
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class BannerFilter extends RenderScriptImageFilter {

    private boolean mIsHorizontal;
    ScriptC_BannerFilter script;

    public BannerFilter(Context context) {
        super(context);
        mIsHorizontal = true;
        script = new ScriptC_BannerFilter(mRS);
    }

    public BannerFilter(Context context, boolean isHorizontal) {
        super(context);
        mIsHorizontal = isHorizontal;
        script = new ScriptC_BannerFilter(mRS);
    }

    @Override
    protected void _process() {
        mIsHorizontal = direction != Constants.SCAN_DIRECTION_HORIZONTAL;
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gIsHorizontal(mIsHorizontal ? 1 : 0);
        script.invoke_process();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
    }
};

// balck with white edges.
class ParamEdgeDetectFilter extends RenderScriptImageFilter {
    private final boolean DoGrayConversion;

    private final boolean DoInversion;

    ScriptC_ParamEdgeDetectFilter script;

    public ParamEdgeDetectFilter(Context context) {
        super(context);
        script = new ScriptC_ParamEdgeDetectFilter(mRS);
        DoGrayConversion = true;
        DoInversion = true;
    }

    public ParamEdgeDetectFilter(Context context, boolean doGrayConversion, boolean doInversion) {
        super(context);
        script = new ScriptC_ParamEdgeDetectFilter(mRS);
        DoGrayConversion = doGrayConversion;
        DoInversion = doInversion;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gDoGrayConversion(DoGrayConversion ? 1 : 0);
        script.set_gDoInversion(DoInversion ? 1 : 0);
        script.set_gScript(script);
        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// like a poster
class PosterizeFilter extends RenderScriptImageFilter {
    private final int mLevel;
    ScriptC_PosterizeFilter script;

    public PosterizeFilter(Context context) {
        super(context);
        script = new ScriptC_PosterizeFilter(mRS);
        mLevel = 10;
    }

    public PosterizeFilter(Context context, int _level) {
        super(context);
        script = new ScriptC_PosterizeFilter(mRS);
        mLevel = _level;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gLevel(mLevel);
        script.set_gScript(script);
        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// perfect reflection horizontal
class ReflectionFilter extends RenderScriptImageFilter {

    private boolean mIsHorizontal;
    ScriptC_ReflectionFilter script;

    public ReflectionFilter(Context context) {
        super(context);
        script = new ScriptC_ReflectionFilter(mRS);
        mIsHorizontal = false;
    }

    @Override
    protected void _process() {
        mIsHorizontal = direction != Constants.SCAN_DIRECTION_HORIZONTAL;
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gIsHorizontal(mIsHorizontal ? 1 : 0);
        script.invoke_process();
        mScript = script;
    }
};

class PixelateFilter extends RenderScriptImageFilter {

    private final int mSquareSize;
    ScriptC_PixelateFilter script;

    public PixelateFilter(Context context) {
        super(context);
        script = new ScriptC_PixelateFilter(mRS);
        mSquareSize = 20;
    }

    @Override
    protected void _process() {

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gSquareSize(mSquareSize);
        script.invoke_process();
        mScript = script;
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