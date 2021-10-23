package com.example.camerax

import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.VideoCaptureConfig


class MainActivity : AppCompatActivity() {

    private var defaultCameraFacing: CameraSelector = DEFAULT_BACK_CAMERA

    private lateinit var binding: ActivityMainBinding

    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    private var captureVideo: Boolean = false

    private var videoCapture: VideoCapture? = null

    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS,
            )
        }

        binding.cameraCaptureButton.setOnClickListener {
            if (captureVideo) {
                stopVideo()
            } else {
                takePhoto()
            }
        }
        binding.cameraCaptureButton.setOnLongClickListener {
            if (!captureVideo) {
                takeVideo()
                captureVideo = true
            }
            captureVideo
        }
        binding.changeCamera.setOnClickListener { changeCamera() }


        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    @SuppressLint("RestrictedApi")
    private fun stopVideo() {
        if (videoCapture == null) return
        captureVideo = false
        Toast.makeText(baseContext, "Stop Video", Toast.LENGTH_SHORT).show()
        videoCapture?.stopRecording()
    }

    @SuppressLint("RestrictedApi")
    private fun takeVideo() {
        if (videoCapture == null) return
        if (ActivityCompat.checkSelfPermission(baseContext, RECORD_AUDIO) == PERMISSION_GRANTED) {

            Toast.makeText(baseContext, "Start Video", Toast.LENGTH_SHORT).show()
            val videoFile = File(
                outputDirectory,
                SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
                ).format(System.currentTimeMillis()) + ".mp4"
            )

            val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

            videoCapture?.startRecording(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : VideoCapture.OnVideoSavedCallback {
                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                        Log.e(TAG, "Video capture failed: $message")
                    }

                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(videoFile)
                        val msg = "Video capture succeeded: $savedUri"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    }
                })
        }
    }

    private fun changeCamera() {
        defaultCameraFacing = if (defaultCameraFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
    }

    private fun takePhoto() {

        Toast.makeText(baseContext, "Take Photo", Toast.LENGTH_SHORT).show()
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun getOutputDirectory(): File {
        val path = this.externalMediaDirs.first()
        val mediaDir = File(path, "CameraTest")
        mediaDir.mkdirs()
        Log.d(TAG, "Create Folder: ${mediaDir.exists()}")
        return if (mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            videoCapture = VideoCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    defaultCameraFacing,
                    preview,
                    videoCapture,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
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

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, RECORD_AUDIO)
    }
}