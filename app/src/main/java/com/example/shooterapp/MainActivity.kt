package com.example.shooterapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.*

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.Exception

import com.example.shooterapp.analyzer.ComponentAnalyzer
import com.example.shooterapp.databinding.ActivityMainBinding
import com.example.shooterapp.util.Prediction
import com.example.shooterapp.util.PredictionViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var previewView: PreviewView
    private lateinit var btnToggleMode: ToggleButton
    private lateinit var btnToggleScan: ToggleButton
    private lateinit var predictionResult: TextView

    private var isScanActive = false
    private var isCameraActive = false

    private lateinit var camera: Camera
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(this)
    }

    private var preview: Preview? = null
    private lateinit var imageAnalysis: ImageAnalysis

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val predictViewModel: PredictionViewModel by viewModels()
    private lateinit var predictionObserver: Observer<Prediction>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called!")
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        previewView = binding.previewView
        btnToggleMode = binding.cameraViewToggle
        btnToggleScan = binding.scanToggle
        predictionResult = binding.predictTextView

        isScanActive = btnToggleScan.isChecked

        predictionObserver = Observer { result ->
            predictionResult.text = result.label
            Log.d(TAG, "TextView Value: ${predictionResult.text}")
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        btnToggleMode.setOnCheckedChangeListener { _, isChecked->
            if (isChecked) showPreview() else hidePreview()
        }

        btnToggleScan.setOnCheckedChangeListener {_, isChecked ->
            if(isCameraActive) {
                if (isChecked) {
                    isScanActive = true
                    if (allPermissionsGranted()) {
                        startCamera()
                    } else {
                        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                    }

                    if (!predictViewModel.prediction.hasActiveObservers())
                        predictViewModel.prediction.observe(this, predictionObserver)
                } else {
                    predictViewModel.prediction.removeObserver(predictionObserver)
                    predictionResult.text = ""
                    cameraProviderFuture.get().unbind(imageAnalysis)
                }
            }
        }

    }

    private fun startCamera() {
        Log.d(TAG, "onCreate: creating ProcessCameraProvider instance...")
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProviderFuture.addListener( Runnable{

            preview = buildPreviewUseCase()

            if (isScanActive)
                imageAnalysis = buildImageAnalysisCase()

            // used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // unbind use cases before rebinding
                cameraProvider.unbindAll()

                // bind use cases to camera
                camera = when(isScanActive) {
                    false -> cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                    else -> cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                }
                preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            } catch(exc: IllegalArgumentException) {
                Log.d(TAG, "Use case binding failed: unable to resolve camera selection/use cases not declared", exc)
            } catch(exc: Exception) {
                Log.d(TAG, "Use case binding failed: unknown error", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        isCameraActive = true
    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder().apply{
            setTargetAspectRatio(DEF_ASPECT_RATIO)
            setTargetRotation(binding.previewView.display.rotation)
        }.build()
    }

    private fun buildImageAnalysisCase(): ImageAnalysis {
        return ImageAnalysis.Builder().apply {
            setTargetResolution(Size(150, 150))
            setTargetRotation(binding.previewView.display.rotation)
            setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        }.build().also{ analysisUseCase: ImageAnalysis ->
            // the parameter in setAnalyzer is the value returned from the analyze() function
            //   the custom analyzer class specified
            analysisUseCase.setAnalyzer(cameraExecutor, ComponentAnalyzer(this){ result: Prediction ->
                predictViewModel.updateData(result)
                Log.d(TAG, "Component: $result")
             })

        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted())
                startCamera()
            else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    // on preview show, start up camera again and
    private fun showPreview(){
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        previewView.visibility = View.VISIBLE
        btnToggleScan.visibility = View.VISIBLE
        predictionResult.visibility = View.VISIBLE
        // TODO: once AR component is introduced, change this section of code(2)
    }

    // on preview hide, shutdown camera features and adjust view layout visibility
    private fun hidePreview() {
        stopCamera()
        previewView.visibility = View.INVISIBLE
        btnToggleScan.visibility = View.INVISIBLE
        predictionResult.visibility = View.INVISIBLE
        // TODO: once AR component is introduced, change this section of code
    }

    private fun stopCamera() {
        cameraProviderFuture
            .get()
            .unbindAll()
        isCameraActive = false
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val DEF_ASPECT_RATIO = AspectRatio.RATIO_16_9
    }
}



/* TODO: 10/09/2020 create a camera app that can:
 *  - hides the camera preview
 *  - switches between photo shoot mode and ar mode (activity OR camera preview)
 *  - display ARCore module in camera preview (possible???)
 */

/* TODO: 02/17/2021 create a camera app that can:
 *  - do ar mode
 *  - add nodes and anchors
 *  - switch between ar and camera mode
 */
// TODO: create ARCore view
// TODO: display ARCore view
// TODO: switch between camera view and AR view
// TODO: connect tensorflow lite inferencing functionality to camerax (ImageAnalysis use case)

// OPTIONAL BUT HELPFUL DEBUG FEATURES
// TODO: create debug menu (toast the details needed to be shown)
