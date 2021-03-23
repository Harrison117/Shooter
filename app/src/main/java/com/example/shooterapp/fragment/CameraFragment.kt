package com.example.shooterapp.fragment

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation

import com.example.shooterapp.R
import com.example.shooterapp.analyzer.ComponentAnalyzer
import com.example.shooterapp.databinding.FragmentCameraBinding
import com.example.shooterapp.util.Prediction
import com.example.shooterapp.util.PredictionViewModel

import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment: Fragment() {
    private lateinit var binding: FragmentCameraBinding

    private lateinit var previewView: PreviewView
    private lateinit var btnToggleMode: ToggleButton
    private lateinit var btnToggleScan: ToggleButton
    private lateinit var predictionResult: TextView

    private var isScanActive = false
    private var isCameraActive = false

    private lateinit var camera: Camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var preview: Preview? = null
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraExecutor: ExecutorService

    private val predictViewModel: PredictionViewModel by viewModels()
    private lateinit var predictionObserver: Observer<Prediction>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        previewView = binding.previewView
        btnToggleMode = binding.cameraViewToggle
        btnToggleScan = binding.scanToggle
        predictionResult = binding.predictTextView

        isScanActive = btnToggleScan.isChecked

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        predictionObserver = Observer { result ->
            predictionResult.text = result.label
            Log.d(TAG, "TextView Value: ${predictionResult.text}")
        }

        // startCamera()
    }

    override fun onResume() {
        super.onResume()
        if(!PermissionsFragment.allPermissionsGranted(requireContext())){
            Navigation
                .findNavController(requireActivity(), R.id.nav_container)
                .navigate(R.id.action_cameraFragment_to_permissionsFragment)
        }
    }

    override fun onDestroyView() {
        stopCamera()
        super.onDestroyView()
    }

    private fun startCamera() {
        Log.d(TAG, "onCreate: creating ProcessCameraProvider instance...")
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener( Runnable{
            preview = buildPreviewUseCase()

            if (isScanActive)
                imageAnalysis = buildImageAnalysisCase()

            // used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // unbind use cases before rebinding
            cameraProvider.unbindAll()

            try {
                // bind use cases to camera
                camera = when(isScanActive) {
                    false -> cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                    else -> cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                }
                preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            } catch(exc: IllegalArgumentException) {
                Log.d(TAG, "Use case binding failed: unable to resolve camera selection/use cases", exc)
            } catch(exc: Exception) {
                Log.d(TAG, "Use case binding failed: unknown error", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

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
            analysisUseCase.setAnalyzer(cameraExecutor, ComponentAnalyzer(requireContext()){ result: Prediction ->
                predictViewModel.updateData(result)
                Log.d(TAG, "Component: $result")
            })
        }
    }

    // on preview show, start up camera again and
    private fun showPreview(){
        startCamera()
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
        cameraExecutor.shutdown()
        cameraProviderFuture
            .get()
            .unbindAll()
        isCameraActive = false
    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val DEF_ASPECT_RATIO = AspectRatio.RATIO_16_9
    }
}