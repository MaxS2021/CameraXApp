package com.smastcomm.cameraxapp

import android.Manifest

object Constants {
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-sss"
    const val REQUEST_CODE_PERMISSION = 101
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        //Manifest.permission.WRITE_EXTERNAL_STORAGE,
        //Manifest.permission.READ_EXTERNAL_STORAGE
    )
}