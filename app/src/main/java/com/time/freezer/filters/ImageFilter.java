package com.time.freezer.filters;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Float3;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicConvolve3x3;


/**
 * This class manages a RenderScript context and Allocations. It applies
 * a chosen filter algorithm to the Bitmap pixels in the Allocation.
 */
public class ImageFilter {

    /** Supported Image Filter Algorithms */
    public static final int FILTER_NONE = 0;
    public static final int FILTER_BLUR = 2;
    public static final int FILTER_MONO = 3;
    public static final int FILTER_SHARPEN = 4;
    public static final int FILTER_LIGHTEN = 5;
    public static final int FILTER_DARKEN = 6;
    public static final int FILTER_EDGE = 7;
    public static final int FILTER_EMBOSS = 8;
    public static final int FILTER_INVERT = 9;
    public static final int FILTER_RIPPLE = 10;
    public static final int FILTER_BIGBROTHER = 11;
    public static final int FILTER_TINT = 12;
    public static final int FILTER_PIXELATE = 13;
    public static final int FILTER_OIL = 14;
    public static final int FILTER_NOISE = 15;
    public static final int FILTER_THRESHOLD = 16;
    public static final int FILTER_CUSTOM = 17;

    /* Local RenderScript context, should be cached */
    private RenderScript mRSContext;
    /* Cached allocation representing the input image */
    private Allocation mInputAllocation;
    /* Post-filtered result image */
    private Bitmap mFilteredResult;
    public ImageFilter(Context context, Bitmap bitmap) {
        //Create the RenderScript context
        mRSContext = RenderScript.create(context);
        mFilteredResult = bitmap.copy(bitmap.getConfig(), true);
    }

    /**
     * Tear down and release the associated RenderScript context.
     */
    public void destroy() {
        mRSContext.destroy();
    }

    /**
     * Called each time a new original is selected. Caching these
     * items reduces memory copies while the user picks their
     * appropriate filter.
     *
     * @param bitmap New image to be used for subsequent filters
     */
    Allocation output;

    public Bitmap setInputBitmap(Bitmap bitmap, int filter) {
        //Construct an allocation for the new original

        mInputAllocation = Allocation.createFromBitmap(mRSContext,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);

        if(output == null) {
            output = Allocation.createTyped(mRSContext,
                    mInputAllocation.getType());
        }
        applyFilter(filter);

        mInputAllocation.destroy();
        return mFilteredResult;
    }

    /**
     * Set the result bitmap to match the original, removing any applied
     * filters.
     */
    public void clearFilter() {
        if (mInputAllocation == null) return;

        //Set back to the original value
        mInputAllocation.copyTo(mFilteredResult);
    }

    /**
     * Apply the selected filter algorithm with RenderScript.
     *
     * @param filter Filter algorithm constant defined by {@link ImageFilter}
     */
    public static void applyFilter(int filter, RenderScript mRSContext, Allocation mInputAllocation) {
        if (mInputAllocation == null) return;
        switch (filter) {
           /* case FILTER_RIPPLE:
                //Our custom script defined in ripple.rs
                ScriptC_ripple scriptR = null;
                        //new ScriptC_ripple(mRSContext, mResources, R.raw.ripple);

                //Set up ripple control values
                //Center at top-left of the image
                scriptR.set_centerX(0);
                scriptR.set_centerY(0);
                //Optional minimum inner radius
                scriptR.set_minRadius(0f);
                //Wave properties
                scriptR.set_scalar(0.75f);     //0.01f - 1.0f
                scriptR.set_damper(0.002f);    //0.0001f - 0.01f
                scriptR.set_frequency(0.075f); //0.01f - 0.5f

                //Run the script
                scriptR.forEach_root(mInputAllocation, output);
                break;*/
            case FILTER_BLUR:
                ScriptIntrinsicBlur scriptBlur =
                        ScriptIntrinsicBlur.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptBlur.setRadius(25f);
                scriptBlur.setInput(mInputAllocation);
                scriptBlur.forEach(mInputAllocation);
                break;
            case FILTER_MONO:
                ScriptIntrinsicColorMatrix scriptColor =
                        ScriptIntrinsicColorMatrix.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptColor.setGreyscale();
                scriptColor.forEach(mInputAllocation, mInputAllocation);
                break;
            case FILTER_SHARPEN:
            case FILTER_LIGHTEN:
            case FILTER_DARKEN:
            case FILTER_EDGE:
            case FILTER_EMBOSS:
                //Convolution filters
                ScriptIntrinsicConvolve3x3 scriptC =
                        ScriptIntrinsicConvolve3x3.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptC.setCoefficients(
                        ConvolutionFilter.getCoefficients(filter));
                scriptC.setInput(mInputAllocation);
                scriptC.forEach(mInputAllocation);
                break;
            default:
                //Do nothing
                return;
        }

        //output.copyTo(mFilteredResult);
    }

    public void applyFilter(int filter) {
        if (mInputAllocation == null) return;

        switch (filter) {
            case FILTER_INVERT:
                ScriptC_InvertFilter script = new ScriptC_InvertFilter(mRSContext);
                script.forEach_root(mInputAllocation, output);
                break;
            case FILTER_BIGBROTHER:
                ScriptC_BigBrotherFilter scriptC_bigbrotherfilter = new ScriptC_BigBrotherFilter(mRSContext);
                scriptC_bigbrotherfilter.forEach_root(mInputAllocation, output);
                break;
            case FILTER_TINT:
                //ScriptC_TintFilter scriptC_tint = new ScriptC_TintFilter(mRSContext);
                //scriptC_tint.forEach_root(mInputAllocation, output);
                break;
            case FILTER_PIXELATE:
                ScriptC_PixelateFilter scriptPixelateFilter = new ScriptC_PixelateFilter(mRSContext);

                scriptPixelateFilter.set_gIn(mInputAllocation);
                scriptPixelateFilter.set_gOut(output);
                scriptPixelateFilter.set_gSquareSize(5);
                scriptPixelateFilter.invoke_process();
                //scriptPixelateFilter.forEach_root(mInputAllocation, output);
                break;
            case FILTER_THRESHOLD:
                ScriptC_ThresholdFilter scriptC_thresholdFilter = new ScriptC_ThresholdFilter(mRSContext);

                scriptC_thresholdFilter.set_gIn(mInputAllocation);
                scriptC_thresholdFilter.set_gOut(output);
                scriptC_thresholdFilter.set_gScript(scriptC_thresholdFilter);
                scriptC_thresholdFilter.set_gThreshold(0.5f);

                scriptC_thresholdFilter.forEach_root(mInputAllocation, output);

                break;
            case FILTER_CUSTOM:
                ScriptC_ThreeDGridFilter scriptC_c = new ScriptC_ThreeDGridFilter(mRSContext);

                scriptC_c.set_gIn(mInputAllocation);
                scriptC_c.set_gOut(output);
                scriptC_c.set_gSize(16);
                scriptC_c.set_gDepth(100.0f / 255.0f);

                scriptC_c.forEach_root(mInputAllocation, output);

                break;
            case FILTER_NOISE:
                //ScriptC_NoiseFilter scriptC_noise = new ScriptC_NoiseFilter(mRSContext);
                //scriptC_noise.forEach_root(mInputAllocation, output);

                break;
            case FILTER_OIL:
                ScriptC_OilPaintFilter scriptC_oil = new ScriptC_OilPaintFilter(mRSContext);

                scriptC_oil.set_gIn(mInputAllocation);
                scriptC_oil.set_gOut(output);
                scriptC_oil.forEach_root(mInputAllocation, output);

                break;
            case FILTER_RIPPLE:
                //Our custom script defined in ripple.rs
                //Set up ripple control values
                //Center at top-left of the image

                break;
            case FILTER_BLUR:
                ScriptIntrinsicBlur scriptBlur =
                        ScriptIntrinsicBlur.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptBlur.setRadius(25f);
                scriptBlur.setInput(mInputAllocation);
                scriptBlur.forEach(output);
                break;
            case FILTER_MONO:
                ScriptIntrinsicColorMatrix scriptColor =
                        ScriptIntrinsicColorMatrix.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptColor.setGreyscale();
                scriptColor.forEach(mInputAllocation, output);
                break;
            case FILTER_SHARPEN:
            case FILTER_LIGHTEN:
            case FILTER_DARKEN:
            case FILTER_EDGE:
            case FILTER_EMBOSS:
                //Convolution filters
                ScriptIntrinsicConvolve3x3 scriptC =
                        ScriptIntrinsicConvolve3x3.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptC.setCoefficients(
                        ConvolutionFilter.getCoefficients(filter));
                scriptC.setInput(mInputAllocation);
                scriptC.forEach(output);
                break;
            default:
                //Do nothing
                return;
        }

        output.copyTo(mFilteredResult);
    }

    /**
     * Return the result image from the last applied filter.
     */
    public Bitmap getBitmap() {
        return mFilteredResult;
    }
}
