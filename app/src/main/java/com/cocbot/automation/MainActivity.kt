package com.cocbot.automation

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.cocbot.automation.controller.BotController
import com.cocbot.automation.model.BotState
import com.cocbot.automation.service.BotAccessibilityService
import com.cocbot.automation.service.OverlayService
import com.cocbot.automation.service.ScreenCaptureService
import com.cocbot.automation.utils.Logger
import com.cocbot.automation.utils.OpenCVLoaderManager
import com.cocbot.automation.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 1000
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }

    private lateinit var startStopButton: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var permissionStatus: TextView

    private var botController: BotController? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    private var pendingScreenCaptureData: Intent? = null
    private var screenCaptureResultCode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeOpenCV()
        checkPermissions()

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        Logger.i("MainActivity created")
    }

    private fun initializeViews() {
        startStopButton = findViewById(R.id.startStopButton)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        permissionStatus = findViewById(R.id.permissionStatus)

        startStopButton.setOnClickListener {
            toggleBot()
        }

        // Disable button initially
        startStopButton.isEnabled = false
    }

    private fun initializeOpenCV() {
        if (!OpenCVLoaderManager.initializeOpenCV(this)) {
            Logger.e("Failed to initialize OpenCV")
            appendLog("ERROR: Failed to initialize OpenCV")
        } else {
            Logger.i("OpenCV initialized successfully")
            appendLog("OpenCV initialized successfully")
        }
    }

    private fun checkPermissions() {
        val accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(this)
        val overlayEnabled = PermissionHelper.canDrawOverlays(this)

        when {
            !accessibilityEnabled -> {
                permissionStatus.text = "❌ Accessibility service required"
                permissionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                permissionStatus.setOnClickListener {
                    PermissionHelper.openAccessibilitySettings(this)
                }
            }
            !overlayEnabled -> {
                permissionStatus.text = "❌ Overlay permission required"
                permissionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                permissionStatus.setOnClickListener {
                    PermissionHelper.openOverlaySettings(this)
                }
            }
            else -> {
                permissionStatus.text = "✅ All permissions granted"
                permissionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                startStopButton.isEnabled = true
                setupBotController()
            }
        }
    }

    private fun setupBotController() {
        val accessibilityService = BotAccessibilityService.getInstance()
        if (accessibilityService != null) {
            val screenCaptureService = ScreenCaptureService.getInstance()

            botController = BotController(this, accessibilityService, screenCaptureService)

            // Observe bot status
            botController?.botStatus?.observe(this, Observer { status ->
                updateUI(status.state, status.message)

                // Update overlay if running
                if (status.state == BotState.RUNNING) {
                    val overlayIntent = Intent(this, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_UPDATE
                        putExtra(OverlayService.EXTRA_STATUS, "Bot: ${status.state}")
                        putExtra(OverlayService.EXTRA_INFO, status.message)
                    }
                    startService(overlayIntent)
                }
            })

            // Observe log messages
            botController?.logMessages?.observe(this, Observer { message ->
                appendLog(message)
            })
        } else {
            appendLog("ERROR: Accessibility service not available")
        }
    }

    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logText.append("\n$timestamp: $message")

        // Auto-scroll to bottom
        logText.post {
            val layout = logText.layout
            if (layout != null) {
                val scrollAmount = layout.getLineTop(logText.lineCount) - logText.height
                if (scrollAmount > 0) {
                    logText.scrollTo(0, scrollAmount)
                }
            }
        }
    }

    private fun toggleBot() {
        val controller = botController
        if (controller == null) {
            requestScreenCapture()
            return
        }

        val currentStatus = controller.getBotStatistics()
        when (currentStatus.state) {
            BotState.STOPPED, BotState.ERROR -> {
                if (pendingScreenCaptureData != null) {
                    startScreenCaptureService()
                    controller.startBot()
                } else {
                    requestScreenCapture()
                }
            }
            BotState.RUNNING -> {
                controller.stopBot()
                stopScreenCaptureService()
                hideOverlay()
            }
            else -> {
                // Bot is starting or in transition, ignore
            }
        }
    }

    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
        if (captureIntent != null) {
            startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE)
        }
    }

    private fun startScreenCaptureService() {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
            putExtra(ScreenCaptureService.RESULT_CODE_KEY, screenCaptureResultCode)
            putExtra(ScreenCaptureService.DATA_KEY, pendingScreenCaptureData)
        }
        startService(serviceIntent)

        // Show overlay
        showOverlay()

        // Setup bot controller if not already done
        if (botController == null) {
            setupBotController()
        }
    }

    private fun stopScreenCaptureService() {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        startService(serviceIntent)
    }

    private fun showOverlay() {
        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
        }
        startService(overlayIntent)
    }

    private fun hideOverlay() {
        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        startService(overlayIntent)
    }

    private fun updateUI(state: BotState, message: String) {
        statusText.text = "Status: ${state.name}"

        when (state) {
            BotState.STOPPED, BotState.ERROR -> {
                startStopButton.text = getString(R.string.start_bot)
                startStopButton.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            }
            BotState.RUNNING -> {
                startStopButton.text = getString(R.string.stop_bot)
                startStopButton.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            }
            BotState.STARTING -> {
                startStopButton.text = "Starting..."
                startStopButton.isEnabled = false
            }
            BotState.PAUSED -> {
                startStopButton.text = "Resume"
            }
        }

        if (state != BotState.STARTING) {
            startStopButton.isEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    screenCaptureResultCode = resultCode
                    pendingScreenCaptureData = data
                    appendLog("Screen capture permission granted")

                    // If bot controller is ready, start the bot
                    if (botController != null) {
                        startScreenCaptureService()
                        botController?.startBot()
                    }
                } else {
                    appendLog("Screen capture permission denied")
                }
            }
            REQUEST_OVERLAY_PERMISSION -> {
                checkPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        botController?.stopBot()
        stopScreenCaptureService()
        hideOverlay()
    }
}