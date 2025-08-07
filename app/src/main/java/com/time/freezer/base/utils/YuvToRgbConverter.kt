package com.time.freezer.base.utils;


import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import ua.kulku.rs.ScriptC_ImageRotator
import java.nio.ByteBuffer


/**
 * Helper class used to efficiently convert a [Media.Image] object from
 * YUV_420_888 format to an RGB [Bitmap] object.
 *
 * The [yuvToRgb] method is able to achieve the same FPS as the CameraX image
 * analysis use case at the default analyzer resolution, which is 30 FPS with
 * 640x480 on a Pixel 3 XL device.
 */
public class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private val  sriptRotator = ScriptC_ImageRotator(rs)

    private var pixelCount: Int = -1
    private lateinit var yuvBuffer: ByteBuffer
    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Synchronized
    fun yuvToRgb(image: Image) : Bitmap {

        val output = Bitmap.createBitmap(image.cropRect.width(), image.cropRect.height(), Bitmap.Config.ARGB_8888)

        // Ensure that the intermediate output byte buffer is allocated
        if (!::yuvBuffer.isInitialized) {
            pixelCount = image.cropRect.width() * image.cropRect.height()
            // Bits per pixel is an average for the whole image, so it's useful to compute the size
            // of the full buffer but should not be used to determine pixel offsets
            val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
            yuvBuffer = ByteBuffer.allocateDirect(pixelCount * pixelSizeBits / 8)
        }

        // Rewind the buffer; no need to clear it since it will be filled
        yuvBuffer.rewind()

        // Get the YUV data in byte array form using NV21 format
        imageToByteBuffer(image, yuvBuffer.array())

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.array().size)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvBuffer.array())
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        //scriptBlur.setInput(outputAllocation)
        //scriptBlur.forEach(outputAllocation)
        //ImageFilter.applyFilter(ImageFilter.FILTER_SHARPEN, rs, outputAllocation);


        //rotate(image, output, 270, output2);
        outputAllocation.copyTo(output);
       // rotate(image, output, 270, output2);
        //rotate(image, outputAllocation, output2);
        return output;
    }


    fun rotate(image: Image, bitmap: Bitmap, angleCcw: Int, target: Bitmap): Bitmap? {
        if (angleCcw == 0) return bitmap
        val script = sriptRotator;
        script.set_inWidth(bitmap.width)
        script.set_inHeight(bitmap.height)
        val sourceAllocation = Allocation.createFromBitmap(rs, bitmap)
        bitmap.recycle()
        script.set_inImage(sourceAllocation)
        //val targetHeight: Int = newHeight(image, angleCcw)
        //val targetWidth: Int = newWidth(image, angleCcw)
        val config = bitmap.config
        val targetAllocation = Allocation.createFromBitmap(rs, target)
        when (angleCcw) {
            90 -> script.forEach_rotate_90_clockwise(targetAllocation, targetAllocation)
            180 -> script.forEach_flip_vertically(targetAllocation, targetAllocation)
            270 -> script.forEach_rotate_270_clockwise(targetAllocation, targetAllocation)
        }
        //script.forEach_flip_horizontally(targetAllocation, targetAllocation)
        targetAllocation.copyTo(target)
        //rs.destroy()
        return target
    }

    fun rotate(source: Bitmap, angle: Int) : Bitmap {
        val angleCcw = 360 -angle;
        val sourceAllocation = Allocation.createFromBitmap(rs, source)
        sriptRotator.set_inWidth(source.width)
        sriptRotator.set_inHeight(source.height)
        sriptRotator.set_inImage(sourceAllocation)
        val targetHeight: Int = newHeight(source, angleCcw)
        val targetWidth: Int = newWidth(source, angleCcw)
        val config: Bitmap.Config = source.getConfig() ?: Bitmap.Config.ARGB_8888
        val target = Bitmap.createBitmap(targetWidth, targetHeight, config)
        val targetAllocation = Allocation.createFromBitmap(rs, target)
        when (angleCcw) {
            90 -> sriptRotator.forEach_rotate_90_clockwise(targetAllocation, targetAllocation)
            180 -> sriptRotator.forEach_flip_vertically(targetAllocation, targetAllocation)
            270 -> sriptRotator.forEach_rotate_270_clockwise(targetAllocation, targetAllocation)
        }
        //sriptRotator.set_inWidth(targetWidth)
        //sriptRotator.set_inHeight(targetHeight)
        //sriptRotator.set_inImage(targetAllocation)
        //sriptRotator.forEach_flip_horizontally(targetAllocation, targetAllocation)
        targetAllocation.copyTo(target);
        return target;
    }

    fun flip(source: Bitmap) : Bitmap {
        val sourceAllocation = Allocation.createFromBitmap(rs, source)
        sriptRotator.set_inWidth(source.width)
        sriptRotator.set_inHeight(source.height)
        sriptRotator.set_inImage(sourceAllocation)
        val targetAllocation = Allocation.createTyped(rs, sourceAllocation.getType());
        sriptRotator.forEach_flip_horizontally(targetAllocation, targetAllocation)
        targetAllocation.copyTo(source);
        return source;
    }

    fun newHeight(image: Bitmap, angleCcw: Int): Int {
        return if (angleCcw == 90 || angleCcw == 270) image.width else image.height
    }

    fun newWidth(image: Bitmap, angleCcw: Int): Int {
        return if (angleCcw == 90 || angleCcw == 270) image.height else image.width
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun imageToByteBuffer(image: Image, outputBuffer: ByteArray) {
        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                        imageCrop.left / 2,
                        imageCrop.top / 2,
                        imageCrop.right / 2,
                        imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                        (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride)

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }
}