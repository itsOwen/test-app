package com.cocbot.automation.utils

import android.util.Log

object Logger {
    private const val TAG = "CoCBot"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun v(message: String) {
        Log.v(TAG, message)
    }
}