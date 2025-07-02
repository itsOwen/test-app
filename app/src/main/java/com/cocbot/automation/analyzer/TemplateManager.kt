package com.cocbot.automation.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cocbot.automation.utils.Logger
import org.opencv.android.Utils
import org.opencv.core.Mat

class TemplateManager(private val context: Context) {

    private val templates = mutableMapOf<String, Mat>()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        try {
            val assetManager = context.assets
            val templateFiles = assetManager.list("templates") ?: return

            for (fileName in templateFiles) {
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                    val templateName = fileName.substringBeforeLast(".")
                    val bitmap = loadBitmapFromAssets("templates/$fileName")

                    if (bitmap != null) {
                        val mat = Mat()
                        Utils.bitmapToMat(bitmap, mat)
                        templates[templateName] = mat
                        Logger.i("Loaded template: $templateName")
                    }
                }
            }

            Logger.i("Total templates loaded: ${templates.size}")
        } catch (e: Exception) {
            Logger.e("Error loading templates", e)
        }
    }

    private fun loadBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(fileName)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Logger.e("Error loading bitmap: $fileName", e)
            null
        }
    }

    fun getTemplate(name: String): Mat? = templates[name]

    fun getAllTemplateNames(): Set<String> = templates.keys

    fun hasTemplate(name: String): Boolean = templates.containsKey(name)
}