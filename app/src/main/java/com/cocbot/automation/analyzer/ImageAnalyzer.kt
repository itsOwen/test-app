package com.cocbot.automation.analyzer

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.cocbot.automation.model.DetectionResult
import com.cocbot.automation.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class ImageAnalyzer(private val templateManager: TemplateManager) {

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.75
        private const val MAX_DETECTIONS_PER_TEMPLATE = 5
    }

    suspend fun analyzeScreen(screenshot: Bitmap): List<DetectionResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<DetectionResult>()

        try {
            val screenMat = Mat()
            Utils.bitmapToMat(screenshot, screenMat)

            // Convert to grayscale for better template matching
            val grayScreen = Mat()
            Imgproc.cvtColor(screenMat, grayScreen, Imgproc.COLOR_BGR2GRAY)

            // Check for gold mines
            templateManager.getTemplate("gold_mine")?.let { template ->
                val detections = matchTemplate(grayScreen, template, "gold_mine")
                results.addAll(detections)
            }

            // Check for elixir collectors
            templateManager.getTemplate("elixir_collector")?.let { template ->
                val detections = matchTemplate(grayScreen, template, "elixir_collector")
                results.addAll(detections)
            }

            Logger.d("Found ${results.size} total detections")

        } catch (e: Exception) {
            Logger.e("Error analyzing screen", e)
        }

        results
    }

    private fun matchTemplate(
        screen: Mat,
        template: Mat,
        templateName: String
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        try {
            // Convert template to grayscale if needed
            val grayTemplate = if (template.channels() > 1) {
                val temp = Mat()
                Imgproc.cvtColor(template, temp, Imgproc.COLOR_BGR2GRAY)
                temp
            } else {
                template
            }

            val result = Mat()
            Imgproc.matchTemplate(screen, grayTemplate, result, Imgproc.TM_CCOEFF_NORMED)

            // Find all matches above threshold
            val locations = mutableListOf<Point>()
            val confidences = mutableListOf<Double>()

            for (i in 0 until result.rows()) {
                for (j in 0 until result.cols()) {
                    val confidence = result.get(i, j)[0]
                    if (confidence >= CONFIDENCE_THRESHOLD) {
                        locations.add(Point(j, i))
                        confidences.add(confidence)
                    }
                }
            }

            // Sort by confidence and take top matches
            val sortedIndices = confidences.indices.sortedByDescending { confidences[it] }
            val topMatches = sortedIndices.take(MAX_DETECTIONS_PER_TEMPLATE)

            for (index in topMatches) {
                val location = locations[index]
                val confidence = confidences[index]

                val boundingBox = Rect(
                    location.x,
                    location.y,
                    location.x + grayTemplate.cols(),
                    location.y + grayTemplate.rows()
                )

                results.add(
                    DetectionResult(
                        templateName = templateName,
                        confidence = confidence,
                        location = location,
                        boundingBox = boundingBox
                    )
                )
            }

            Logger.d("Template $templateName: ${results.size} matches found")

        } catch (e: Exception) {
            Logger.e("Error matching template $templateName", e)
        }

        return results
    }
}