package com.cocbot.automation.model

import android.graphics.Point

data class DetectionResult(
    val templateName: String,
    val confidence: Double,
    val location: Point,
    val boundingBox: android.graphics.Rect,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean = confidence > 0.75

    fun getCenterPoint(): Point {
        return Point(
            boundingBox.centerX(),
            boundingBox.centerY()
        )
    }
}

enum class TargetType {
    GOLD_MINE,
    ELIXIR_COLLECTOR,
    UNKNOWN
}

data class Target(
    val type: TargetType,
    val detection: DetectionResult,
    val priority: Int = 1
) {
    companion object {
        fun fromDetection(detection: DetectionResult): Target {
            val type = when {
                detection.templateName.contains("gold") -> TargetType.GOLD_MINE
                detection.templateName.contains("elixir") -> TargetType.ELIXIR_COLLECTOR
                else -> TargetType.UNKNOWN
            }
            return Target(type, detection)
        }
    }
}