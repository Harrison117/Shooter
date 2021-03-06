package com.example.shooterapp.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.shooterapp.R

class PermissionsFragment: Fragment() {

    private var isAllPermissionGranted = false
    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { requestPermissionGranted ->
        when {
            requestPermissionGranted || isAllPermissionGranted -> {
                // proceed to camera fragment
                Navigation
                    .findNavController(requireActivity(), R.id.nav_container)
                    .navigate(PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment())
            }

            shouldShowRequestPermissionRationale(CAMERA_PERMISSION) -> {
                // request permission again, explaining why you need the camera
                isAllPermissionGranted = false
            }

            else -> {
                // request permissions again
                isAllPermissionGranted = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // pass arguments to next fragment
        isAllPermissionGranted = allPermissionsGranted(requireContext())
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called!")
        cameraPermission.launch(CAMERA_PERMISSION)
    }

    companion object {
        private const val TAG = "PermissionsFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}