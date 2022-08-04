package com.example.camera.userCase

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera.core.CameraCore
import com.example.camera.utils.FILENAME_FORMAT
import com.example.camera.utils.REQUIRED_PERMISSIONS
import com.example.camera.utils.TAG
import java.text.SimpleDateFormat
import java.util.*

class TakePhotoAndVideo(
    private val context: Context,
    preview: Preview.SurfaceProvider,
    lifecycleOwner: LifecycleOwner,
    private val completeProcess: () -> Unit = {},
    private val errorProcess: (Throwable) -> Unit = {}
) : CameraCore(context, preview, lifecycleOwner) {

    private var imageCapture: ImageCapture? = null

    private var recorder: Recorder? = null

    private var videoCapture: VideoCapture<Recorder?>? = null

    private val qualitySelector = QualitySelector.from(Quality.FHD)

    private var recording: Recording? =  null

    private var captureVideo = false

    override fun initCamera() {
        super.initCamera()

        recorder = Recorder.Builder()
            .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
            .build()

        imageCapture = ImageCapture.Builder()
            .build()

        recorder?.let {
            videoCapture = VideoCapture.withOutput(it)
        }
    }

    override fun getUseCaseGroup(): UseCaseGroup.Builder {
        return UseCaseGroup.Builder().addUseCase(preview).apply {
            imageCapture?.let { image ->
                addUseCase(image)
            }
            videoCapture?.let { video ->
                addUseCase(video)
            }
        }
    }

    override fun hasAllPermitsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCameraError(throwable: Throwable) {
        super.onCameraError(throwable)
        errorProcess.invoke(throwable)
    }

    /**
     * functions
     */

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onTakePhoto() {
        if (imageCapture == null) return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    onCameraError(Throwable(exc.message))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }


    fun onTakeVideo() {
        if (videoCapture == null || captureVideo) return

        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        captureVideo = true
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            recording =  videoCapture?.output
                ?.prepareRecording(context, mediaStoreOutput)
                ?.withAudioEnabled()
                ?.start(ContextCompat.getMainExecutor(context)) {
                    when(it) {
                        is VideoRecordEvent.Finalize -> {
                            Log.d(TAG, "Recording is complete")
                            completeProcess.invoke()
                        }
                        is VideoRecordEvent.Start -> {
                            captureVideo = true
                            Log.d(TAG, "Start Recording")
                        }
                    }
                }
        } else {
            onCameraError(Throwable("cannot start recording because it does not have the required permissions"))
        }
    }

    fun onStopVideo() {
        if (videoCapture == null || recording == null) return
        captureVideo = false
        recording?.stop()
    }

    fun isTakingVideoNow(): Boolean = captureVideo
}