package com.cocbot.automation.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.cocbot.automation.utils.Logger

class BotAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: BotAccessibilityService? = null

        fun getInstance(): BotAccessibilityService? = instance

        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.i("Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for our bot
        // This service is primarily used for gesture dispatch
    }

    override fun onInterrupt() {
        Logger.w("Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Logger.i("Accessibility service destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Logger.i("Accessibility service unbound")
        return super.onUnbind(intent)
    }
}