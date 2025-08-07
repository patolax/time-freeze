package com.time.freezer.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Float3;
import android.renderscript.Matrix4f;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;

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

class BlendMode {
    public final static int Normal = 0;

    public final static int Additive = 1;

    public final static int Subractive = 2;

    public final static int Multiply = 3;

    public final static int Overlay = 4;

    public final static int ColorDodge = 5;

    public final static int ColorBurn = 6;

    public final static int Lighten = 7;

    public final static int Darken = 8;

    public final static int Reflect = 9;

    public final static int Glow = 10;

    public final static int LinearLight = 11;

    public final static int Frame = 12;/* photo frame */

    public final static int Luminosity = 11;

}

// saturation like in photo edit
class SaturationModifyFilter extends IImageFilter {
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
class NoiseFilter extends IImageFilter {
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
    ScriptIntrinsicColorMatrix script;

    public BlackWhiteFilter(Context context) {
        super(context);
        script =
                ScriptIntrinsicColorMatrix.create(mRS,
                        Element.U8_4(mRS));
    }

    @Override
    protected final void _process() {
        script.setGreyscale();
        script.forEach(mInAllocation, mOutAllocation);
    }
};

class SepiaFilter extends IImageFilter {
    ScriptIntrinsicColorMatrix script;
    final Matrix4f mSepia = new Matrix4f(new float[]{
            0.189f, 0.769f, 0.393f, 0f,
            0.168f, 0.686f, 0.349f, 0f,
            0.131f, 0.534f, 0.272f, 0f,
            0.000f, 0.000f, 0.000f, 1f
    });

    public SepiaFilter(Context context) {
        super(context);
        script =
                ScriptIntrinsicColorMatrix.create(mRS,
                        Element.U8_4(mRS));
    }

    @Override
    protected final void _process() {
        script.setColorMatrix(mSepia);
        script.forEach(mInAllocation, mOutAllocation);
    }
};

class OldFilter extends IImageFilter {
    ScriptIntrinsicColorMatrix script;
    final Matrix4f mSepia = new Matrix4f(new float[]{
            0.393f, 0.349f, 0.272f, 0f,
            0.769f, 0.686f, 0.534f, 0f,
            0.189f, 0.168f, 0.131f, 0f,
            0.000f, 0.000f, 0.000f, 1f
    });

    public OldFilter(Context context) {
        super(context);
        script =
                ScriptIntrinsicColorMatrix.create(mRS,
                        Element.U8_4(mRS));
    }

    @Override
    protected final void _process() {
        script.setColorMatrix(mSepia);
        script.forEach(mInAllocation, mOutAllocation);
    }
};

// this one is like black and white. removed
class BrickFilter extends IImageFilter {

    ScriptC_BlackWhiteFilter script;

    public BrickFilter(Context context) {
        super(context);
        script = new ScriptC_BlackWhiteFilter(mRS);
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

class FeatherFilter extends IImageFilter {

    ScriptC_FeatherFilter script;

    public FeatherFilter(Context context) {
        super(context);
        script = new ScriptC_FeatherFilter(mRS);
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

class InvertFilter extends IImageFilter {

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

class IllusionFilter extends IImageFilter {

    ScriptC_IllusionFilter script;

    public IllusionFilter(Context context) {
        super(context);
        script = new ScriptC_IllusionFilter(mRS);
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

class LightFilter extends IImageFilter {

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
class MosaicFilter extends IImageFilter {

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
class TintFilter extends IImageFilter {

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
class MistFilter extends IImageFilter {

    ScriptC_MistFilter script;

    public MistFilter(Context context) {
        super(context);
        script = new ScriptC_MistFilter(mRS);
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


// not working and heavey
class CleanGlassFilter extends IImageFilter {

    ScriptC_CleanGlassFilter script;

    public CleanGlassFilter(Context context) {
        super(context);
        script = new ScriptC_CleanGlassFilter(mRS);
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
class OilPaintFilter extends IImageFilter {

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

// nice photo frame, has to fix the top boarder issue.
class RaiseFrameFilter extends IImageFilter {

    ScriptC_RaiseFrameFilter script;

    public RaiseFrameFilter(Context context) {
        super(context);
        script = new ScriptC_RaiseFrameFilter(mRS);
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

// not working
class EdgeFilter extends IImageFilter {

    float[] filterArray = new float[]{
            0f, 1f, 0f,
            1f, -4f, 1f,
            0f, 1f, 0f};
    ScriptIntrinsicConvolve3x3 script;

    public EdgeFilter(Context context) {
        super(context);
        script =
                ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
    }

    @Override
    protected final void _process() {
        script.setCoefficients(filterArray);
        script.setInput(mInAllocation);
        script.forEach(mOutAllocation);
    }

};


// looks like photo reel
class ColorQuantizeFilter extends IImageFilter {

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

class ColorToneFilter extends IImageFilter {
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


class ThreeDGridFilter extends IImageFilter {
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

/*
class FillPatternFilter extends IImageFilter {
    private Bitmap patternBitmap;

    private Allocation patternAllocation;

    @Override
    protected void _preProcess() {
        patternBitmap = loadBitmap(R.drawable.image1);
        patternAllocation = Allocation.createFromBitmap(mRS, patternBitmap, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
    }

    @Override
    protected void _postProcess() {
        patternAllocation.destroy();
        patternAllocation = null;
        patternBitmap.recycle();
        patternBitmap = null;
    }

    @Override
    protected void _process() {
        ScriptC_FillPatternFilter script = new ScriptC_FillPatternFilter(mRS, getResources(),
                R.raw.fillpatternfilter);

        script.set_gIn(mInAllocation);
        script.set_gPattern(patternAllocation);
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
*/

// its like noise filter
class SharpFilter extends IImageFilter {

    private final float mStep;
    ScriptC_SharpFilter script;

    public SharpFilter(Context context) {
        super(context);
        script = new ScriptC_SharpFilter(mRS);
        mStep = 1f;
    }

    public SharpFilter(Context context, float step) {
        super(context);
        script = new ScriptC_SharpFilter(mRS);
        mStep = step;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gStep(mStep);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class GrayscaleFilter extends IImageFilter {

    ScriptIntrinsicColorMatrix script;

    public GrayscaleFilter(Context context) {
        super(context);
        script =
                ScriptIntrinsicColorMatrix.create(mRS,
                        Element.U8_4(mRS));
    }

    @Override
    protected void _process() {
        script.setGreyscale();
        script.forEach(mInAllocation, mOutAllocation);
    }

};

// like black and white
class HistogramEqualFilter extends IImageFilter {
    private final float mContrastIntensity;

    private Allocation pixelGrayscaleArrayAllocation;
    ScriptC_HistogramEqualFilter script;

    public HistogramEqualFilter(Context context) {
        super(context);
        mContrastIntensity = 0.25f;
        script = new ScriptC_HistogramEqualFilter(mRS);
    }

    public HistogramEqualFilter(Context context, float contrastIntensity) {
        super(context);
        mContrastIntensity = contrastIntensity;
        script = new ScriptC_HistogramEqualFilter(mRS);
    }

    @Override
    protected void preProcess(Bitmap mBitmapIn) {
        super.preProcess(mBitmapIn);
        if (pixelGrayscaleArrayAllocation == null) {
            pixelGrayscaleArrayAllocation = Allocation.createSized(mRS, Element.U8(mRS), mBitmapIn.getWidth()
                    * mBitmapIn.getHeight());
        }
    }

    @Override
    protected void _process() {
        script.bind_pixelGrayscaleArray(pixelGrayscaleArrayAllocation);
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gContrastIntensity(mContrastIntensity);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }

    @Override
    protected void destory() {
        super.destory();
        pixelGrayscaleArrayAllocation.destroy();
        pixelGrayscaleArrayAllocation = null;
    }
};

// like gray scale
class HslModifyFilter extends IImageFilter {

    private final float mHueFactor;
    ScriptC_HslModifyFilter script;

    public HslModifyFilter(Context context) {
        super(context);
        mHueFactor = 20.0f;
        script = new ScriptC_HslModifyFilter(mRS);
    }

    public HslModifyFilter(Context context, float hueFactor) {
        super(context);
        mHueFactor = hueFactor;
        script = new ScriptC_HslModifyFilter(mRS);
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);
        script.set_gHueFactor(mHueFactor);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class BlurFilter extends IImageFilter {

    ScriptIntrinsicBlur script;

    public BlurFilter(Context context) {
        super(context);
        script = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
    }

    @Override
    protected void _process() {
        script.setRadius(25f);
        script.setInput(mInAllocation);
    }

    @Override
    protected void _postProcess() {
        script.forEach(mOutAllocation);
    }
};

// thresholding = black and white
class ThresholdFilter extends IImageFilter {
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
class RadialDistortionFilter extends IImageFilter {

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

//removed
class BlockPrintFilter extends IImageFilter {

    private Allocation mTmpOutputAllocation;
    private ScriptC_ParamEdgeDetectFilter script;
    private ScriptC_ImageBlender mBlenderScript;

    public BlockPrintFilter(Context context) {
        super(context);
        script = new ScriptC_ParamEdgeDetectFilter(mRS);
        mBlenderScript = new ScriptC_ImageBlender(mRS);
    }

    @Override
    protected void _preProcess() {
        mTmpOutputAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
    }

    @Override
    protected void destory() {
        super.destory();
        mTmpOutputAllocation.destroy();
        mTmpOutputAllocation = null;
        mBlenderScript.destroy();
        mBlenderScript = null;
    }

    @Override
    protected void _process() {
        ScriptC_ParamEdgeDetectFilter script = new ScriptC_ParamEdgeDetectFilter(mRS);

        script.set_gIn(mInAllocation);
        script.set_gOut(mTmpOutputAllocation);
        script.set_gThreshold(0.25f);
        script.set_gK00(1.0f);
        script.set_gK01(2.0f);
        script.set_gK02(1.0f);
        script.set_gDoGrayConversion(0);
        script.set_gDoInversion(0);
        script.set_gScript(script);

        script.invoke_filter();
        script.forEach_root(mInAllocation, mOutAllocation);
        mScript = script;

        mBlenderScript.set_gIn1(mInAllocation);
        mBlenderScript.set_gIn2(mTmpOutputAllocation);
        mBlenderScript.set_gOut(mOutAllocation);
        mBlenderScript.set_gBlendMode(BlendMode.Multiply);
        mBlenderScript.set_gScript(mBlenderScript);

        mBlenderScript.invoke_filter();
        mBlenderScript.forEach_root(mInAllocation, mOutAllocation);
    }

};

// not working black sctreen
class SmashColorFilter extends IImageFilter {
    private Allocation mTmpOutputAllocation;

    ScriptC_ParamEdgeDetectFilter script;
    private ScriptC_ImageBlender mBlenderScript;

    public SmashColorFilter(Context context) {
        super(context);
        script = new ScriptC_ParamEdgeDetectFilter(mRS);
        mBlenderScript = new ScriptC_ImageBlender(mRS);
    }

    @Override
    protected void _preProcess() {
        mTmpOutputAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
    }

    @Override
    protected void destory() {
        super.destory();
        mTmpOutputAllocation.destroy();
        mTmpOutputAllocation = null;
        mBlenderScript.destroy();
        mBlenderScript = null;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mTmpOutputAllocation);
        script.set_gThreshold(0.25f);
        script.set_gK00(1.0f);
        script.set_gK01(2.0f);
        script.set_gK02(1.0f);
        script.set_gDoGrayConversion(0);
        script.set_gDoInversion(0);
        script.set_gScript(script);

        script.invoke_filter();
        script.forEach_root(mInAllocation, mOutAllocation);
        mScript = script;


        mBlenderScript.set_gIn1(mInAllocation);
        mBlenderScript.set_gIn2(mTmpOutputAllocation);
        mBlenderScript.set_gOut(mOutAllocation);
        mBlenderScript.set_gBlendMode(BlendMode.LinearLight);
        mBlenderScript.set_gMixture(2.5f);
        mBlenderScript.set_gScript(mBlenderScript);

        mBlenderScript.invoke_filter();
        mBlenderScript.forEach_root(mInAllocation, mOutAllocation);
    }
};

class BigBrotherFilter extends IImageFilter {

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

class BannerFilter extends IImageFilter {

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
class ParamEdgeDetectFilter extends IImageFilter {
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

// not working
class ComicFilter extends IImageFilter {
    // For GaussianBlurFilter
    private final int mPadding = 3;

    private Allocation mImageWithPaddingBufferAllocation;

    private Allocation mTempBufferAllocation;

    private Allocation mEdgedAllocation;

    private Allocation mSaturatedAllocation;

    private ScriptC_SaturationModifyFilter mSaturationModifyFilter;

    private ScriptIntrinsicBlur mGaussianBlurFilterScript;

    private ScriptC_ImageBlender mBlenderScript;

    private ScriptC_ParamEdgeDetectFilter mParamEdgeDetectFilter;

    private ScriptC_ImageBlender script;

    public ComicFilter(Context context) {
        super(context);
        mSaturationModifyFilter = new ScriptC_SaturationModifyFilter(mRS);
        mGaussianBlurFilterScript = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        mBlenderScript = new ScriptC_ImageBlender(mRS);
        mParamEdgeDetectFilter = new ScriptC_ParamEdgeDetectFilter(mRS);
        script = new ScriptC_ImageBlender(mRS);
    }

    @Override
    protected void _preProcess() {
        int heightWithPadding = mBitmapOut.getHeight() + mPadding * 2;
        int widthWithPadding = mBitmapOut.getWidth() + mPadding * 2;
        int bufferSize = widthWithPadding * heightWithPadding * 3;
        mImageWithPaddingBufferAllocation = Allocation.createSized(mRS, Element.F32(mRS), bufferSize);
        mTempBufferAllocation = Allocation.createSized(mRS, Element.F32(mRS), bufferSize);

        mEdgedAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
        mSaturatedAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
    }

    @Override
    protected void _process() {
        mSaturationModifyFilter.set_gIn(mInAllocation);
        mSaturationModifyFilter.set_gOut(mSaturatedAllocation);
        mSaturationModifyFilter.set_gSaturationFactor(1.0f);
        mSaturationModifyFilter.set_gScript(mSaturationModifyFilter);
        mSaturationModifyFilter.invoke_filter();
        mSaturationModifyFilter.forEach_root(mInAllocation, mOutAllocation);

        mGaussianBlurFilterScript.setRadius(1f);
        mGaussianBlurFilterScript.setInput(mInAllocation);
        mGaussianBlurFilterScript.forEach(mOutAllocation);

        mBlenderScript.set_gIn1(mSaturatedAllocation);
        mBlenderScript.set_gIn2(mSaturatedAllocation);
        mBlenderScript.set_gOut(mOutAllocation);
        mBlenderScript.set_gBlendMode(BlendMode.Lighten);
        mBlenderScript.set_gMixture(1.0f);
        mBlenderScript.set_gScript(mBlenderScript);
        mBlenderScript.invoke_filter();
        mBlenderScript.forEach_root(mInAllocation, mOutAllocation);

        mParamEdgeDetectFilter.set_gIn(mOutAllocation);
        mParamEdgeDetectFilter.set_gOut(mEdgedAllocation);
        mParamEdgeDetectFilter.set_gScript(mParamEdgeDetectFilter);
        mParamEdgeDetectFilter.invoke_filter();
        mParamEdgeDetectFilter.forEach_root(mInAllocation, mOutAllocation);

        script.set_gIn1(mOutAllocation);
        script.set_gIn2(mEdgedAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gBlendMode(BlendMode.Lighten);
        script.set_gMixture(0.8f);
        script.set_gScript(script);
        script.invoke_filter();
        script.forEach_root(mInAllocation, mOutAllocation);
        mScript = script;
    }

    @Override
    protected void destory() {
        super.destory();
        mImageWithPaddingBufferAllocation.destroy();
        mImageWithPaddingBufferAllocation = null;
        mTempBufferAllocation.destroy();
        mTempBufferAllocation = null;

        mEdgedAllocation.destroy();
        mEdgedAllocation = null;
        mSaturatedAllocation.destroy();
        mSaturatedAllocation = null;

        mSaturationModifyFilter.destroy();
        mSaturationModifyFilter = null;
        mGaussianBlurFilterScript.destroy();
        mGaussianBlurFilterScript = null;
        mBlenderScript.destroy();
        mBlenderScript = null;
        mParamEdgeDetectFilter.destroy();
        mParamEdgeDetectFilter = null;
    }

};

class GammaFilter extends IImageFilter {
    private final int mGamma;
    ScriptC_GammaFilter script;

    public GammaFilter(Context context) {
        super(context);
        script = new ScriptC_GammaFilter(mRS);
        mGamma = 50;
    }

    public GammaFilter(Context context, int gamma) {
        super(context);
        script = new ScriptC_GammaFilter(mRS);
        mGamma = gamma;
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gGamma(mGamma);
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
class PosterizeFilter extends IImageFilter {
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
class ReflectionFilter extends IImageFilter {

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

class PixelateFilter extends IImageFilter {

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

// not sure what this is
class NightVisionFilter extends IImageFilter {
    // BrightContrastFilter factors
    private final float mBrightness;
    private final float mContrast;

    private Allocation mTmpOutputAllocation;

    private ScriptC_BrightContrastFilter mBrightContrastFilterScript;
    private ScriptC_SoftGlowFilter script;

    public NightVisionFilter(Context context) {
        super(context);
        mBrightness = 0.4f;
        mContrast = 1.1f;
        script = new ScriptC_SoftGlowFilter(mRS);
        mBrightContrastFilterScript = new ScriptC_BrightContrastFilter(mRS);
    }

    @Override
    protected void _preProcess() {
        mTmpOutputAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
    }

    @Override
    protected void _process() {

        mBrightContrastFilterScript.set_gIn(mInAllocation);
        mBrightContrastFilterScript.set_gOut(mTmpOutputAllocation);
        mBrightContrastFilterScript.set_gScript(mBrightContrastFilterScript);
        mBrightContrastFilterScript.set_gBrightnessFactor(mBrightness);
        mBrightContrastFilterScript.set_gContrastFactor(mContrast);
        mBrightContrastFilterScript.invoke_filter();
        mBrightContrastFilterScript.forEach_root(mInAllocation, mOutAllocation);

        script.set_gIn(mInAllocation);
        script.set_gProcessedIn(mTmpOutputAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gScript(script);
        script.invoke_filter();
        script.forEach_root(mInAllocation, mOutAllocation);
        mScript = script;
    }

    @Override
    protected void destory() {
        super.destory();
        mTmpOutputAllocation.destroy();
        mTmpOutputAllocation = null;
        mBrightContrastFilterScript.destroy();
        mBrightContrastFilterScript = null;
    }
};

class EmbossFilter extends IImageFilter {
    private final float mIntensity;
    private final float mOffset;
    private final int mUseBrightness;

    ScriptC_ConvolutionFilter script;

    public EmbossFilter(Context context) {
        super(context);
        mIntensity = 1;
        mOffset = 1;
        mUseBrightness = 1;
        script = new ScriptC_ConvolutionFilter(mRS);
    }

    @Override
    protected void _process() {

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gKernel1stLine(new Float3(-2.0f * mIntensity, -mIntensity, 0.0f));
        script.set_gKernel2ndLine(new Float3(-mIntensity, 1.0f, mIntensity));
        script.set_gKernel3rdLine(new Float3(0.0f, mIntensity, 2.0f * mIntensity));
        script.set_gFactor(1.0f);
        script.set_gOffset(mOffset);
        script.set_gUseBrightness(mUseBrightness);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class EdgeDetectionConvolutionFilter extends IImageFilter {
    private final int mUseBrightness;
    private final float mOffset;
    ScriptC_ConvolutionFilter script;

    public EdgeDetectionConvolutionFilter(Context context) {
        super(context);
        mOffset = 1;
        mUseBrightness = 1;
        script = new ScriptC_ConvolutionFilter(mRS);
    }

    @Override
    protected void _process() {
        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gKernel1stLine(new Float3(-1.0f, 0.0f, -1.0f));
        script.set_gKernel2ndLine(new Float3(0.0f, 4.0f, 0.0f));
        script.set_gKernel3rdLine(new Float3(-1.0f, 0.0f, -1.0f));
        script.set_gFactor(1.0f);
        script.set_gOffset(mOffset);
        script.set_gUseBrightness(mUseBrightness);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

// not working and heavey removed
class CartoonFilter extends IImageFilter {

    ScriptC_CleanGlassFilter script;

    public CartoonFilter(Context context) {
        super(context);
        script = new ScriptC_CleanGlassFilter(mRS);
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

// perfect emboss
class SobelFilter extends IImageFilter {
    ScriptIntrinsicBlur scriptIntrinsicBlur;
    float[] kernelVertical;
    float[] kernelHorizontal;
    float amount = 1;
    boolean vertical;
    ScriptC_ConvolutionFilter script;
    float mOffset = 1;
    int mUseBrightness = 0;

    public SobelFilter(Context context) {
        super(context);
        float v = amount + 1;
        kernelHorizontal = new float[]{
                -v, -2 * v, -v,
                0, 0, 0,
                v, 2 * v, v
        };

        kernelVertical = new float[]{
                -v, 0, v,
                -2 * v, 0, 2 * v,
                -v, 0, v
        };
        vertical = false;
        scriptIntrinsicBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        script = new ScriptC_ConvolutionFilter(mRS);
    }


    @Override
    protected final void _process() {
        if (amount > 0) {
            scriptIntrinsicBlur.setRadius(1f);
            scriptIntrinsicBlur.setInput(mInAllocation);
            scriptIntrinsicBlur.forEach(mOutAllocation);
        }
        float v = amount + 1;

        float[] kernel = (vertical) ? kernelHorizontal : kernelVertical;

        script.set_gIn(mInAllocation);
        script.set_gOut(mOutAllocation);
        script.set_gKernel1stLine(new Float3(kernel[0], kernel[1], kernel[2]));
        script.set_gKernel2ndLine(new Float3(kernel[3], kernel[4], kernel[5]));
        script.set_gKernel3rdLine(new Float3(kernel[6], kernel[7], kernel[8]));
        script.set_gFactor(1.0f);
        script.set_gOffset(mOffset);
        script.set_gUseBrightness(mUseBrightness);
        script.set_gScript(script);

        script.invoke_filter();
        mScript = script;
    }

    @Override
    protected void _postProcess() {
        script.forEach_root(mInAllocation, mOutAllocation);
    }
};

class FakeFilter extends IImageFilter {

    public FakeFilter(Context context) {
        super(context);
    }

    protected void preProcess(Bitmap mBitmapIn) {

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


    @Override
    protected final void _process() {

    }

    @Override
    protected void _postProcess() {

    }
};

class NoFilter extends IImageFilter {

    public NoFilter(Context context) {
        super(context);
    }

    protected void preProcess(Bitmap mBitmapIn) {

    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return bitmap;
    }


    @Override
    protected final void _process() {

    }

    @Override
    protected void _postProcess() {

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