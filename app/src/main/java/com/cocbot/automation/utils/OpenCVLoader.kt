package com.cocbot.automation.utils

import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

object OpenCVLoaderManager {
    private var isInitialized = false

    private val loaderCallback = object : BaseLoaderCallback(null) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Logger.i("OpenCV loaded successfully")
                    isInitialized = true
                }
                else -> {
                    Logger.e("OpenCV initialization failed")
                    super.onManagerConnected(status)
                }
            }
        }
    }

    fun initializeOpenCV(context: Context): Boolean {
        return if (!OpenCVLoader.initDebug()) {
            Logger.d("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)
            false
        } else {
            Logger.d("OpenCV library found inside package. Using it!")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
            true
        }
    }

    fun isOpenCVInitialized(): Boolean = isInitialized
}