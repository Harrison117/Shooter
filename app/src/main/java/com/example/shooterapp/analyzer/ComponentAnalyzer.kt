package com.example.shooterapp.analyzer

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.shooterapp.util.Label
import com.example.shooterapp.util.Prediction
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream

import com.example.shooterapp.util.YuvToRgbConverter
import org.tensorflow.lite.DataType
import com.example.shooterapp.ml.Model as ComponentModel

import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

typealias  ResultListener = (Prediction) -> Unit

class ComponentAnalyzer(ctx: Context, private val listener: ResultListener) : ImageAnalysis.Analyzer {

    private val labelList = arrayListOf("cpu","hdrive","mboard","power")

    private val yuvToRgbConverter = YuvToRgbConverter()

    private val componentModel: ComponentModel by lazy{

        val compatList = CompatibilityList()

        // Optional GPU acceleration
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

    private var lastTimeStamp: Long = 0L
    private val interval = TimeUnit.MILLISECONDS.toMillis(500)

    private val ctx = ctx

    override fun analyze(image: ImageProxy) {
        // analyze an image every after one second
        val currentTimeStamp = System.currentTimeMillis()
        val deltaTime = currentTimeStamp - lastTimeStamp
        if(deltaTime >= interval) {
            val bmImg = image.toBitmap()

            if (bmImg == null) {
                Log.d(TAG, "Input image is null ")
                image.close()
                return
            }
            // DEBUG
            // saveMediaToStorage(bmImg)

            val cropSize = if (bmImg.width >= bmImg.height)
                bmImg.height
            else
                bmImg.width

            // prepare dimensions of input image to square-like dimensions
            // size of input required for model is 150x150
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(ResizeOp(150, 150, ResizeOp.ResizeMethod.BILINEAR))
                .build()
            var tImage = TensorImage(DataType.FLOAT32)
            tImage.load(bmImg)
            tImage = imageProcessor.process(tImage)

            val probabilityProcessor = TensorProcessor.Builder()
                .add(NormalizeOp(0f, 255f))
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

            val name_display = when (prediction.key.toString()) {
                "cpu" -> "CPU"
                "hdrive" -> "Hard Disk Drive"
                "mboard" -> "Motherboard"
                else -> "Power Supply"
            }
            val result = Prediction(Label(name_display, prediction.key), prediction.value)

            listener(result)

            lastTimeStamp = currentTimeStamp
        }
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

    private fun saveMediaToStorage(bitmap: Bitmap) {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            ctx.contentResolver.also { resolver ->

                //Content resolver will process the content values
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    companion object {
        private const val TAG = "ImageAnalyzer"
    }
}