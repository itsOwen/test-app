package com.cocbot.automation.model

import android.graphics.Point

sealed class BotAction {
    data class Tap(
        val point: Point,
        val delay: Long = 500L
    ) : BotAction()

    data class Wait(
        val duration: Long
    ) : BotAction()

    data class Swipe(
        val startPoint: Point,
        val endPoint: Point,
        val duration: Long = 300L
    ) : BotAction()

    object Scan : BotAction()
}

data class ActionResult(
    val action: BotAction,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)