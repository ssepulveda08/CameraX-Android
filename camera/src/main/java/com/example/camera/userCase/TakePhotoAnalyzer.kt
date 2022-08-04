package com.example.camera.userCase

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera.core.CameraCore
import com.example.camera.utils.FILENAME_FORMAT
import com.example.camera.utils.REQUIRED_PERMISSIONS_PHOTO
import com.example.camera.utils.TAG
import java.text.SimpleDateFormat
import java.util.*

class TakePhotoAnalyzer(
    private val context: Context,
    preview: Preview.SurfaceProvider,
    lifecycleOwner: LifecycleOwner,
    private val analyzer: UseCase? = null,
    private val completeProcess: () -> Unit = {},
    private val errorProcess: (Throwable) -> Unit = {}
) : CameraCore(context, preview, lifecycleOwner) {

    private var imageCapture: ImageCapture? = null

    override fun initCamera() {
        super.initCamera()
        imageCapture = ImageCapture.Builder()
            .build()
    }

    override fun hasAllPermitsGranted(): Boolean = REQUIRED_PERMISSIONS_PHOTO.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun getUseCaseGroup(): UseCaseGroup.Builder {
        return UseCaseGroup.Builder().addUseCase(preview).apply {
            analyzer?.let {
                addUseCase(it)
            }
            imageCapture?.let { image ->
                addUseCase(image)
            }
        }
    }

    override fun onCameraError(throwable: Throwable) {
        super.onCameraError(throwable)
        errorProcess.invoke(throwable)
    }

    /**
     * functions
     */

    fun onTakePhoto() {
        if (imageCapture == null) return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    onCameraError(Throwable(exc.message))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    completeProcess.invoke()
                }
            }
        )
    }

}
