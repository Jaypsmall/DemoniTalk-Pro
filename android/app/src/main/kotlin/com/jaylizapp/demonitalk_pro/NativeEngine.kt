package com.jaylizapp.demonitalk_pro

import android.util.Log
import kotlin.jvm.Volatile
import kotlin.jvm.JvmStatic

object NativeEngine {
    private const val TAG = "NativeEngine"

    @Volatile
    private var isLoaded = false

    init {
        try {
            java.lang.System.loadLibrary("demoni_engine")
            isLoaded = true
            Log.i(TAG, "⚡ Motor nativo Rust 'demoni_engine' cargado exitosamente")
        } catch (e: java.lang.Throwable) {
            isLoaded = false
            Log.w(TAG, "Motor nativo Rust no disponible (Fallback a Kotlin): ${e.message}")
        }
    }

    fun isAvailable(): Boolean = isLoaded

    @JvmStatic
    external fun isEngineReady(): Boolean

    @JvmStatic
    external fun normalizeText(input: String): String

    @JvmStatic
    external fun findWakeWord(input: String): String

    @JvmStatic
    external fun processCommand(
        inputText: String,
        commandsJson: String,
        requireWakeWord: Boolean
    ): String

    @JvmStatic
    external fun fuzzyMatch(text: String, trigger: String): Boolean
}
