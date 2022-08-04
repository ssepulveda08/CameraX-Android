package com.example.camera.utils

import android.Manifest

internal const val TAG = "CameraLifecycleObserver"
internal const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
internal const val ERROR_DOES_NOT_HAVE_REQUIRED_PERMITS =
    "You do not have the necessary permits to continue"
internal const val ERROR_USER_CASE_BINDING_FAILED =
    "Use case binding failed"
internal val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.RECORD_AUDIO
)
internal val REQUIRED_PERMISSIONS_PHOTO = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
)