package com.example.camerax.analyzers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.example.camerax.utils.Draw
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseDetectorAnalyzer(
    private val context: Context,
    private val parentLayout: ConstraintLayout
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = imageProxy.image

        if (image != null) {

            val processImage = InputImage.fromMediaImage(image, rotationDegrees)

            val options = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()

            val poseDetector = PoseDetection.getClient(options)

            poseDetector.process(processImage)
                .addOnSuccessListener {
                    if (it.allPoseLandmarks.isNotEmpty()) {
                        val element = Draw(context, it)
                        parentLayout.children.forEachIndexed { index, _ ->
                            parentLayout.removeViewAt(index)
                        }
                        parentLayout.addView(element)
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    Log.d(PoseDetectorAnalyzer::class.java.name, "addOnFailureListener ${it.message}")
                    imageProxy.close()
                }
        }
    }
}