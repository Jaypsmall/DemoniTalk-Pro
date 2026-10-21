package com.jaylizapp.demonitalk_pro

data class VoiceCommand(
    val trigger: String,
    val action: String,
    val isRoot: Boolean = false
)
