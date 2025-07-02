package com.cocbot.automation.controller

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import com.cocbot.automation.model.BotAction
import com.cocbot.automation.model.ActionResult
import com.cocbot.automation.service.BotAccessibilityService
import com.cocbot.automation.utils.Logger
import kotlinx.coroutines.delay
import kotlin.random.Random

class ActionExecutor(private val accessibilityService: BotAccessibilityService) {

    companion object {
        private const val GESTURE_DURATION = 100L
        private const val TAP_VARIATION_RADIUS = 10
    }

    suspend fun executeAction(action: BotAction): ActionResult {
        try {
            when (action) {
                is BotAction.Tap -> {
                    val result = performTap(action.point)
                    delay(action.delay)
                    return ActionResult(action, result)
                }

                is BotAction.Wait -> {
                    delay(action.duration)
                    return ActionResult(action, true)
                }

                is BotAction.Swipe -> {
                    val result = performSwipe(action.startPoint, action.endPoint, action.duration)
                    return ActionResult(action, result)
                }

                is BotAction.Scan -> {
                    // Scan action is handled by the analyzer, just return success
                    return ActionResult(action, true)
                }
            }
        } catch (e: Exception) {
            Logger.e("Error executing action: $action", e)
            return ActionResult(action, false, e.message)
        }
    }

    private fun performTap(point: Point): Boolean {
        return try {
            // Add small random variation to make taps look human-like
            val variation = Random.nextInt(-TAP_VARIATION_RADIUS, TAP_VARIATION_RADIUS + 1)
            val tapX = (point.x + variation).toFloat()
            val tapY = (point.y + variation).toFloat()

            val path = Path().apply {
                moveTo(tapX, tapY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, GESTURE_DURATION))
                .build()

            val result = accessibilityService.dispatchGesture(gesture, null, null)
            Logger.d("Tap executed at ($tapX, $tapY) - Success: $result")
            result

        } catch (e: Exception) {
            Logger.e("Error performing tap", e)
            false
        }
    }

    private fun performSwipe(startPoint: Point, endPoint: Point, duration: Long): Boolean {
        return try {
            val path = Path().apply {
                moveTo(startPoint.x.toFloat(), startPoint.y.toFloat())
                lineTo(endPoint.x.toFloat(), endPoint.y.toFloat())
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val result = accessibilityService.dispatchGesture(gesture, null, null)
            Logger.d("Swipe executed from $startPoint to $endPoint - Success: $result")
            result

        } catch (e: Exception) {
            Logger.e("Error performing swipe", e)
            false
        }
    }
}