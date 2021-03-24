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
    private lateinit var btnToggleScan: ToggleButton
    private lateinit var predictionResult: TextView


    private lateinit var camera: Camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var preview: Preview? = null
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraExecutor: ExecutorService

    private val predictViewModel: PredictionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        // LiveData in ViewModel class ('prediction' variable) is exposed;
        // bind the view model into the data variable of the view ('predict' variable).
        // The view model, together with the view, will automatically respond to the lifecycle of
        //   the lifecycle owner (CameraFragment). No need for Observers!
        binding.predict = predictViewModel
        binding.lifecycleOwner = this

        previewView = binding.previewView
        btnToggleScan = binding.scanToggle
        predictionResult = binding.predictTextView

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    override fun onResume() {
        super.onResume()
        if(!PermissionsFragment.allPermissionsGranted(requireContext())){
            Navigation
                .findNavController(requireActivity(), R.id.nav_container)
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
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
            imageAnalysis = buildImageAnalysisCase()

            // used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // unbind use cases before rebinding
            cameraProvider.unbindAll()

            try {
                // bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

                preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            } catch(exc: IllegalArgumentException) {
                Log.d(TAG, "Use case binding failed: unable to resolve camera selection/use cases", exc)
            } catch(exc: Exception) {
                Log.d(TAG, "Use case binding failed: unknown error", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

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
                // data will be automatically shown in the view
                predictViewModel.updateData(result)
                Log.d(TAG, "Component: $result")
            })
        }
    }

    private fun stopCamera() {
        cameraExecutor.shutdown()
        cameraProviderFuture
            .get()
            .unbindAll()
    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val DEF_ASPECT_RATIO = AspectRatio.RATIO_16_9
    }
}