package com.example.shooterapp.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.*
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
import com.example.shooterapp.ar.helpers.SnackbarHelper
import com.example.shooterapp.databinding.FragmentCameraBinding
import com.example.shooterapp.util.Label
import com.example.shooterapp.util.Prediction
import com.example.shooterapp.util.PredictionViewModel

import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment: Fragment() {
    private lateinit var binding: FragmentCameraBinding

    private lateinit var previewView: PreviewView
    private lateinit var btnLockOn: ToggleButton
    private var predictionResult: Label? = null


    private lateinit var camera: Camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var preview: Preview? = null
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraExecutor: ExecutorService



    private val predictViewModel: PredictionViewModel by viewModels()
    private val messageSnackbar = SnackbarHelper()

    private val delayHandler: Handler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        // LiveData in ViewModel class ('prediction' variable) is exposed;
        // bind the view model into the data variable of the view ('predict' variable).
        // The view model, together with the view, will automatically respond to the lifecycle of
        //   the lifecycle owner (CameraFragment). No need for Observers!
        binding.predict = predictViewModel
        binding.lifecycleOwner = this

        previewView = binding.previewView
        btnLockOn = binding.lockToggle

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLockOn.setOnCheckedChangeListener { _, isChecked ->
            val result = predictionResult ?: return@setOnCheckedChangeListener
            if(isChecked) {
                messageSnackbar.showMessage(requireActivity(), "Component Locked On! Visualizing in 2...")
                delayHandler.postDelayed({
                    btnLockOn.isChecked = false
                    Navigation
                        .findNavController(requireActivity(), R.id.nav_container)
                        .navigate(CameraFragmentDirections.actionCameraFragmentToArFragment(result.name, result.name_display))
                }, 2000)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        messageSnackbar.showMessage(requireActivity(), "Center phone to desired part")
    }

    override fun onResume() {
        super.onResume()
        if(!PermissionsFragment.allPermissionsGranted(requireContext())){
            Navigation
                .findNavController(requireActivity(), R.id.nav_container)
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
        }

    }

    override fun onStop() {
        super.onStop()
        stopCamera()
        messageSnackbar.hide(requireActivity())
    }

    private fun startCamera() {
        Log.d(TAG, "onCreate: creating ProcessCameraProvider instance...")
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
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
        return Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }

    private fun buildImageAnalysisCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also{ analysisUseCase: ImageAnalysis ->
            // the parameter in setAnalyzer is the value returned from the analyze() function
            //   the custom analyzer class specified
            analysisUseCase.setAnalyzer(cameraExecutor, ComponentAnalyzer(requireContext()){ result: Prediction ->
                // data will be automatically shown in the view
                predictionResult = result.label
                predictViewModel.updateData(result)
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
        private const val UNKNOWN_ORIENTATION = -1
    }
}