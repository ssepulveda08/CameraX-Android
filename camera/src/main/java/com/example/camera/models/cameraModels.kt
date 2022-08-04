package com.example.camera.models

import androidx.camera.core.UseCaseGroup

interface Camera {
    fun initCamera()
    fun getUseCaseGroup(): UseCaseGroup.Builder
    fun hasAllPermitsGranted(): Boolean
    fun onSwitchCamera()
    fun onCameraError(throwable: Throwable)
}