package com.example.camera.core

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.camera.models.Camera
import com.example.camera.utils.ERROR_DOES_NOT_HAVE_REQUIRED_PERMITS
import com.example.camera.utils.ERROR_USER_CASE_BINDING_FAILED
import com.example.camera.utils.TAG
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class CameraCore(
    private val context: Context,
    private val previewSurface: Preview.SurfaceProvider,
    val lifecycleOwner: LifecycleOwner,
) : Camera, DefaultLifecycleObserver {

    internal lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    internal lateinit var mainExecutor: Executor

    internal lateinit var preview: Preview

    internal lateinit var cameraExecutor: ExecutorService

    private  var defaultCameraFacing: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        mainExecutor = ContextCompat.getMainExecutor(context)

        if (hasAllPermitsGranted()) {
            buildCamera()
        } else {
            onCameraError(Throwable(ERROR_DOES_NOT_HAVE_REQUIRED_PERMITS))
        }
    }

    private fun buildCamera() {
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            cameraProvider.unbindAll()


            // Preview
            preview = buildPreview()

            initCameraProvider(cameraProvider)
        }, mainExecutor)
    }

    open fun initCameraProvider(cameraProvider: ProcessCameraProvider) {
        try {
            val useCaseGroup = getUseCaseGroup().build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                defaultCameraFacing,
                useCaseGroup
            )

        } catch (exc: Exception) {
            Log.e(TAG, ERROR_USER_CASE_BINDING_FAILED, exc)
            onCameraError(Throwable(ERROR_USER_CASE_BINDING_FAILED))
        }
    }

    internal fun buildPreview(): Preview {
        return Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewSurface)
            }
    }

    override fun getUseCaseGroup(): UseCaseGroup.Builder {
        return UseCaseGroup.Builder().addUseCase(preview)
    }

    override fun onCameraError(throwable: Throwable) {

    }

    override fun onSwitchCamera() {
        defaultCameraFacing = if (defaultCameraFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        buildCamera()
    }

    /**
     * Lifecycle
     * */

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        initCamera()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onCreate(owner)
        cameraExecutor.shutdown()
    }
}