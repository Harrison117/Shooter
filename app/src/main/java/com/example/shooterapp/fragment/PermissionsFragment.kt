package com.example.shooterapp.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.content.Context

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionsFragment: Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!allPermissionsGranted(requireContext())) {

        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}