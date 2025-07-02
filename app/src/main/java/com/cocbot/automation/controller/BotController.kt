package com.cocbot.automation.controller

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cocbot.automation.analyzer.ImageAnalyzer
import com.cocbot.automation.analyzer.TemplateManager
import com.cocbot.automation.model.*
import com.cocbot.automation.service.BotAccessibilityService
import com.cocbot.automation.service.ScreenCaptureService
import com.cocbot.automation.utils.Logger
import kotlinx.coroutines.*

class BotController(
    private val context: Context,
    private val accessibilityService: BotAccessibilityService,
    private val screenCaptureService: ScreenCaptureService?
) {

    private val _botStatus = MutableLiveData<BotStatus>()
    val botStatus: LiveData<BotStatus> = _botStatus

    private val _logMessages = MutableLiveData<String>()
    val logMessages: LiveData<String> = _logMessages

    private var botJob: Job? = null
    private var isRunning = false

    private lateinit var templateManager: TemplateManager
    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var actionExecutor: ActionExecutor

    private var statistics = BotStatus(BotState.STOPPED, "Bot initialized")

    init {
        setupComponents()
        updateStatus(BotState.STOPPED, "Bot ready")
    }

    private fun setupComponents() {
        templateManager = TemplateManager(context)
        imageAnalyzer = ImageAnalyzer(templateManager)
        actionExecutor = ActionExecutor(accessibilityService)
    }

    fun startBot() {
        if (isRunning) {
            logMessage("Bot is already running")
            return
        }

        if (screenCaptureService == null) {
            logMessage("Screen capture service not available")
            updateStatus(BotState.ERROR, "Screen capture service not available")
            return
        }

        logMessage("Starting bot...")
        updateStatus(BotState.STARTING, "Initializing bot...")

        botJob = CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            try {
                isRunning = true
                updateStatus(BotState.RUNNING, "Bot started successfully")
                runBotLoop()
            } catch (e: Exception) {
                Logger.e("Bot execution error", e)
                updateStatus(BotState.ERROR, "Bot error: ${e.message}")
                stopBot()
            }
        }
    }

    fun stopBot() {
        if (!isRunning) {
            logMessage("Bot is already stopped")
            return
        }

        logMessage("Stopping bot...")
        isRunning = false
        botJob?.cancel()
        updateStatus(BotState.STOPPED, "Bot stopped")
    }

    private suspend fun runBotLoop() {
        while (isRunning) {
            try {
                // Capture screen
                val screenshot = screenCaptureService?.getCurrentScreenshot()

                if (screenshot != null) {
                    // Analyze for targets
                    val detections = imageAnalyzer.analyzeScreen(screenshot)
                    val targets = detections.filter { it.isValid() }
                        .map { Target.fromDetection(it) }
                        .sortedBy { it.priority }

                    updateStatistics(targets)

                    if (targets.isNotEmpty()) {
                        // Execute actions for found targets
                        for (target in targets.take(3)) { // Limit concurrent actions
                            val tapAction = BotAction.Tap(target.detection.getCenterPoint())
                            val result = actionExecutor.executeAction(tapAction)

                            if (result.success) {
                                val targetType = when (target.type) {
                                    TargetType.GOLD_MINE -> "Gold Mine"
                                    TargetType.ELIXIR_COLLECTOR -> "Elixir Collector"
                                    else -> "Unknown"
                                }
                                logMessage("Tapped $targetType at ${target.detection.getCenterPoint()}")
                                statistics = statistics.copy(
                                    actionsPerformed = statistics.actionsPerformed + 1,
                                    lastAction = "Tapped $targetType"
                                )
                            }
                        }

                        // Wait before next scan
                        delay(2000)
                    } else {
                        updateStatus(BotState.RUNNING, "Scanning... No targets found")
                        delay(1000)
                    }
                } else {
                    logMessage("Failed to capture screenshot")
                    delay(1000)
                }

            } catch (e: Exception) {
                Logger.e("Error in bot loop", e)
                logMessage("Bot loop error: ${e.message}")
                delay(2000)
            }
        }
    }

    private fun updateStatistics(targets: List<Target>) {
        val goldMines = targets.count { it.type == TargetType.GOLD_MINE }
        val elixirCollectors = targets.count { it.type == TargetType.ELIXIR_COLLECTOR }

        statistics = statistics.copy(
            goldMinesFound = statistics.goldMinesFound + goldMines,
            elixirCollectorsFound = statistics.elixirCollectorsFound + elixirCollectors
        )

        updateStatus(
            BotState.RUNNING,
            "Found: ${targets.size} targets (${goldMines} gold, ${elixirCollectors} elixir)"
        )
    }

    private fun updateStatus(state: BotState, message: String) {
        statistics = statistics.copy(state = state, message = message)
        _botStatus.postValue(statistics)
    }

    private fun logMessage(message: String) {
        Logger.i(message)
        _logMessages.postValue(message)
    }

    fun pauseBot() {
        if (isRunning) {
            updateStatus(BotState.PAUSED, "Bot paused")
            // Implementation for pause functionality
        }
    }

    fun resumeBot() {
        if (statistics.state == BotState.PAUSED) {
            updateStatus(BotState.RUNNING, "Bot resumed")
            // Implementation for resume functionality
        }
    }

    fun getBotStatistics(): BotStatus = statistics
}