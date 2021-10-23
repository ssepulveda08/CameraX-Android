package com.example.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import com.example.camerax.utils.CameraListener
import com.example.camerax.utils.MyCamera


class MainActivity2 : AppCompatActivity(), CameraListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var camera: MyCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isAllPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS,
            )
        }

        binding.cameraCaptureButton.setOnClickListener {
            if (camera.isTakingVideoNow()) {
                camera.onStopVideo()
            } else {
                camera.onTakePhoto()
            }
        }
        binding.cameraCaptureButton.setOnLongClickListener {
            if (!camera.isTakingVideoNow()) {
                camera.onTakeVideo()
            }
            true
        }
        binding.changeCamera.setOnClickListener { camera.onSwitchCamera() }

    }

    private fun startCamera() {
        camera = MyCamera(
            this,
            binding.previewView.surfaceProvider,
            this,
            this.externalMediaDirs.first(),
            this
        )
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

    override fun onPhotoTakeSuccessfully(results: ImageCapture.OutputFileResults) {

    }

    override fun onVideoTakeSuccessfully(results: VideoCapture.OutputFileResults) {

    }

    override fun onCameraError(throwable: Throwable) {

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