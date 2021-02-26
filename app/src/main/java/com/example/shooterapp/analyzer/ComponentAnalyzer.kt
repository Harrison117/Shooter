package com.example.shooterapp.analyzer

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.shooterapp.util.Prediction
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.example.shooterapp.util.YuvToRgbConverter
import org.tensorflow.lite.DataType
import com.example.shooterapp.ml.Model as ComponentModel

import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

typealias  ResultListener = (Prediction) -> Unit

class ComponentAnalyzer(ctx: Context, private val listener: ResultListener) : ImageAnalysis.Analyzer {

    private final val INPUT_SIZE = intArrayOf(1, 150, 150, 3)
    private final val labelList = arrayListOf("power","mboard","hdrive","cpu")

    private val yuvToRgbConverter = YuvToRgbConverter(ctx)

    private val componentModel: ComponentModel by lazy{

        // TODO 6. Optional GPU acceleration
        val compatList = CompatibilityList()

        val options = if(compatList.isDelegateSupportedOnThisDevice) {
            Log.d(TAG, "This device is GPU Compatible ")
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            Log.d(TAG, "This device is GPU Incompatible ")
            Model.Options.Builder().setNumThreads(4).build()
        }

        // Initialize the Flower Model
        ComponentModel.newInstance(ctx, options)
    }

    override fun analyze(image: ImageProxy) {
        val bmImg = image.toBitmap()

        if(bmImg == null) {
            Log.d(TAG, "Input image is null ")
            image.close()
            return
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(150, 150, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        var tImage = TensorImage(DataType.FLOAT32)
        tImage.load(bmImg)
        tImage = imageProcessor.process(tImage)

        val probabilityProcessor = TensorProcessor.Builder()
            .add(NormalizeOp(0f,255f))
            .build()

        val outputs = componentModel.process(probabilityProcessor.process(tImage.tensorBuffer))
        val outputBuffer = outputs.outputFeature0AsTensorBuffer

        val tensorLabel = TensorLabel(labelList, outputBuffer)

        val prediction = tensorLabel.mapWithFloatValue.maxByOrNull { it.value }

        if (prediction == null) {
            Log.d(TAG, "Output prediction is null ")
            image.close()
            return
        }

        val result = Prediction(prediction.key, prediction.value)

        listener(result)

        // close current input image and move to next image fed on input
        image.close()
    }

    // * START: helper functions for imageProxy to Bitmap conversion modified and taken from:
    // *   https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap/62105972#62105972
    // *
    private fun ImageProxy.toBitmap(): Bitmap? {
        val nv21 = yuvToRgbConverter.yuv420888ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        return yuvImage.toBitmap()
    }

    private fun YuvImage.toBitmap(): Bitmap? {
        val out = ByteArrayOutputStream()
        if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
            return null
        val imageBytes: ByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    // * END: helper functions for imageProxy to Bitmap conversion modified and taken from:
    // *   https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap/62105972#62105972
    // *

    companion object {
        private const val TAG = "ImageAnalyzer"
        private const val INPUT_BUFFER_SIZE = 150*150*3
    }
}