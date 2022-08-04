package com.example.camera.userCase

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera.core.CameraCore
import com.example.camera.utils.REQUIRED_PERMISSIONS

class TakeVideoAnalyzer(
    private val context: Context,
    preview: Preview.SurfaceProvider,
    lifecycleOwner: LifecycleOwner,
    private val analyzers: Array<ImageAnalysis.Analyzer>? = null,
    private val errorProcess: (Throwable) -> Unit = {}
) : CameraCore(context, preview, lifecycleOwner) {

    private var recorder: Recorder? = null

    private val qualitySelector = QualitySelector.from(Quality.FHD)

    private var videoAnalyzer: ImageAnalysis? = null

    override fun initCamera() {
        super.initCamera()

        recorder = Recorder.Builder()
            .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
            .build()

        videoAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(1080, 1920))
            .build()

        analyzers?.forEach {
            videoAnalyzer?.setAnalyzer(cameraExecutor, it)
        }
    }

    override fun getUseCaseGroup(): UseCaseGroup.Builder {
        return UseCaseGroup.Builder().addUseCase(preview).apply {
            videoAnalyzer?.let {
                addUseCase(it)
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
}
