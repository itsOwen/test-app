package com.cocbot.automation.model

enum class BotState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR
}

data class BotStatus(
    val state: BotState,
    val message: String,
    val goldMinesFound: Int = 0,
    val elixirCollectorsFound: Int = 0,
    val actionsPerformed: Int = 0,
    val lastAction: String = "",
    val timestamp: Long = System.currentTimeMillis()
)