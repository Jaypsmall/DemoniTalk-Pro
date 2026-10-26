package com.jaylizapp.demonitalk_pro

import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit

object ShellUtils {

    private const val TAG = "ShellUtils"
    private var rootGranted: Boolean? = null

    fun resetRootCache() {
        rootGranted = null
    }

    fun isRootAvailable(): Boolean {
        rootGranted?.let { return it }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val finished = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(3, TimeUnit.SECONDS)
            } else {
                process.waitFor() == 0
            }

            val isRoot = finished && process.exitValue() == 0
            rootGranted = isRoot
            isRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando disponibilidad de Root: ${e.message}")
            rootGranted = false
            false
        }
    }

    fun executeCommand(command: String): Boolean {
        if (!isRootAvailable()) return false

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val finished = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(5, TimeUnit.SECONDS)
            } else {
                process.waitFor() == 0
            }
            val success = finished && process.exitValue() == 0
            if (!success) {
                Log.w(TAG, "El comando Shell devolvió un código no cero para: $command")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando comando Root [$command]: ${e.message}")
            false
        } finally {
            try { process?.destroy() } catch (_: Exception) {}
        }
    }
}
