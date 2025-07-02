package com.cocbot.automation.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.cocbot.automation.R
import com.cocbot.automation.utils.Logger

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "SHOW_OVERLAY"
        const val ACTION_HIDE = "HIDE_OVERLAY"
        const val ACTION_UPDATE = "UPDATE_OVERLAY"
        const val EXTRA_STATUS = "status"
        const val EXTRA_INFO = "info"

        private var instance: OverlayService? = null
        fun getInstance(): OverlayService? = instance
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var statusText: TextView? = null
    private var infoText: TextView? = null
    private var isOverlayVisible = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Logger.i("OverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
            ACTION_UPDATE -> {
                val status = intent.getStringExtra(EXTRA_STATUS)
                val info = intent.getStringExtra(EXTRA_INFO)
                updateOverlay(status, info)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (isOverlayVisible || overlayView != null) return

        try {
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            statusText = overlayView?.findViewById(R.id.overlayStatus)
            infoText = overlayView?.findViewById(R.id.overlayInfo)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 100
            }

            windowManager?.addView(overlayView, params)
            isOverlayVisible = true
            Logger.i("Overlay shown")

        } catch (e: Exception) {
            Logger.e("Error showing overlay", e)
        }
    }

    private fun hideOverlay() {
        if (!isOverlayVisible || overlayView == null) return

        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            statusText = null
            infoText = null
            isOverlayVisible = false
            Logger.i("Overlay hidden")

        } catch (e: Exception) {
            Logger.e("Error hiding overlay", e)
        }
    }

    private fun updateOverlay(status: String?, info: String?) {
        if (!isOverlayVisible) return

        status?.let { statusText?.text = it }
        info?.let { infoText?.text = it }
    }

    fun updateStatus(status: String, info: String) {
        updateOverlay(status, info)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        instance = null
        Logger.i("OverlayService destroyed")
    }
}