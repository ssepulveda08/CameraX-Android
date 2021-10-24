package com.example.camerax.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyCamera(
    private val context: Activity,
    private val preview: Preview.SurfaceProvider,
    private val lifecycleOwner: LifecycleOwner,
    private val path: File,
    private val listener: CameraListener
) : LifecycleObserver, CameraXInterface {

    private var defaultCameraFacing: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    private var captureVideo: Boolean = false

    private var videoCapture: VideoCapture? = null

    override fun initCamera() {
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (isAllPermissionsGranted()) {
            startCamera()
        } else {
            onCameraError(Throwable(ERROR_DOES_NOT_HAVE_REQUIRED_PERMITS))
        }
    }

    override fun onTakePhoto() {
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
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    val error = "Photo capture failed: ${exc.message}"
                    Log.e(TAG, error, exc)
                    onCameraError(Throwable(error))
                    captureVideo = false
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    listener.onPhotoTakeSuccessfully(output)
                }
            })
    }

    @SuppressLint("RestrictedApi")
    override fun onTakeVideo() {
        if (videoCapture == null) return

        captureVideo = true
        val videoFile = File(
            outputDirectory,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
            ).format(System.currentTimeMillis()) + ".mp4"
        )

        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture?.startRecording(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    val error = "Video capture failed: $message"
                    Log.e(TAG, error)
                    onCameraError(Throwable(error))
                }

                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(videoFile)
                    val msg = "Video capture succeeded: $savedUri"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    listener.onVideoTakeSuccessfully(outputFileResults)
                }
            })
    }

    @SuppressLint("RestrictedApi")
    override fun onStopVideo() {
        if (videoCapture == null) return
        captureVideo = false
        videoCapture?.stopRecording()
    }

    override fun onSwitchCamera() {
        defaultCameraFacing = if (defaultCameraFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
    }

    override fun onCameraError(throwable: Throwable) {
        listener.onCameraError(throwable)
    }

    override fun isTakingVideoNow(): Boolean = captureVideo

    /**
     * internal Camera
     */

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(preview)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            videoCapture = VideoCapture.Builder()
                .build()

            // Select back camera as a default
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    defaultCameraFacing,
                    preview,
                    videoCapture,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, ERROR_USER_CASE_BINDING_FAILED, exc)
                onCameraError(Throwable(ERROR_USER_CASE_BINDING_FAILED))
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun getOutputDirectory(): File {
        val mediaDir = File(path, "filesDir")
        mediaDir.mkdirs()
        Log.d(TAG, " Create Folder: ${mediaDir.exists()}")
        return if (mediaDir.exists())
            mediaDir else context.filesDir
    }

    private fun isAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Lifecycle
     * */

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreateCamera() {
        captureVideo = false
        initCamera()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyCamera() {
        captureVideo = false
        cameraExecutor.shutdown()
    }

    /**
     * CONST
     */

    companion object {
        private const val TAG = "CameraLifecycleObserver"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val ERROR_DOES_NOT_HAVE_REQUIRED_PERMITS =
            "You do not have the necessary permits to continue"
        private const val ERROR_USER_CASE_BINDING_FAILED =
            "Use case binding failed"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }

}

interface CameraXInterface {
    fun initCamera()
    fun onTakePhoto()
    fun onTakeVideo()
    fun onStopVideo()
    fun onSwitchCamera()
    fun onCameraError(throwable: Throwable)
    fun isTakingVideoNow(): Boolean
}

interface CameraListener {
    fun onPhotoTakeSuccessfully(results: ImageCapture.OutputFileResults)
    fun onVideoTakeSuccessfully(results: VideoCapture.OutputFileResults)
    fun onCameraError(throwable: Throwable)
}