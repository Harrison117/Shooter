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
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.Exception

import com.example.shooterapp.analyzer.ComponentAnalyzer
import com.example.shooterapp.databinding.ActivityMainBinding
import com.example.shooterapp.util.Prediction
import com.example.shooterapp.util.PredictionViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called!")
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil
            .setContentView(this, R.layout.activity_main) as ActivityMainBinding

        val navController = this.findNavController(R.id.nav_container)
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}


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
