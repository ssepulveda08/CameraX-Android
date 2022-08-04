package com.example.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.camera.userCase.TakeVideoAnalyzer
import com.example.camerax.analyzers.LuminosityAnalyzer
import com.example.camerax.analyzers.PoseDetectorAnalyzer
import com.example.camerax.databinding.ActivityMainBinding

class MainActivity4 : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var camera: TakeVideoAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraCaptureButton.isVisible = false

        if (isAllPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS,
            )
        }
        binding.changeCamera.setOnClickListener {
            binding.containerDrawView.removeAllViews()
            camera.onSwitchCamera()
        }
    }

    private fun startCamera() {
        val poseDetector = PoseDetectorAnalyzer(baseContext, binding.containerDrawView)
        val luminosity = LuminosityAnalyzer() {
            Log.d("LuminosityAnalyzer", "Luminosity: $it")

        }
        camera = TakeVideoAnalyzer(
            context = this,
            preview = binding.previewView.surfaceProvider,
            lifecycleOwner = this,
            arrayOf(poseDetector, luminosity)
        ) {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
        camera.initCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (isAllPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun isAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }
}