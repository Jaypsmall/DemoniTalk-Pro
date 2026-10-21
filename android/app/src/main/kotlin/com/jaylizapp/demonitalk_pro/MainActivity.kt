package com.jaylizapp.demonitalk_pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.jaylizapp.demonitalk_pro/channel"
    private val PERMISSION_REQUEST_CODE = 101
    private lateinit var repository: CommandRepository

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        repository = CommandRepository(this)

        // Request audio recording permission on startup if not already granted
        checkAndRequestAudioPermission()

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "checkAndRequestAudioPermission" -> {
                    val granted = checkAndRequestAudioPermission()
                    result.success(granted)
                }

                "hasAudioPermission" -> {
                    val granted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    result.success(granted)
                }

                "startFloatingService" -> {
                    if (!hasAudioPermission()) {
                        checkAndRequestAudioPermission()
                    }
                    startFloatingService()
                    result.success(true)
                }

                "stopFloatingService" -> {
                    sendControlIntent("ACTION_STOP_SERVICE")
                    result.success(true)
                }

                "sendControlIntent" -> {
                    if (!hasAudioPermission()) {
                        checkAndRequestAudioPermission()
                    }
                    val action = call.argument<String>("action") ?: ""
                    sendControlIntent(action)
                    result.success(true)
                }

                "isAccessibilityEnabled" -> {
                    result.success(isAccessibilityEnabled())
                }

                "openAccessibilitySettings" -> {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("ERROR", e.message, null)
                    }
                }

                "canDrawOverlays" -> {
                    result.success(Settings.canDrawOverlays(this))
                }

                "openOverlaySettings" -> {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("ERROR", e.message, null)
                    }
                }

                "requestRoot" -> {
                    Thread {
                        ShellUtils.resetRootCache()
                        val available = ShellUtils.isRootAvailable()
                        runOnUiThread {
                            result.success(available)
                        }
                    }.start()
                }

                "getCommands" -> {
                    val list = repository.loadCommands().map { cmd ->
                        mapOf(
                            "trigger" to cmd.trigger,
                            "action" to cmd.action,
                            "isRoot" to cmd.isRoot
                        )
                    }
                    result.success(list)
                }

                "saveCommands" -> {
                    val rawList = call.argument<List<Map<String, Any>>>("commands") ?: emptyList()
                    val voiceCommands = rawList.map { item ->
                        VoiceCommand(
                            trigger = item["trigger"] as? String ?: "",
                            action = item["action"] as? String ?: "",
                            isRoot = item["isRoot"] as? Boolean ?: false
                        )
                    }
                    repository.saveCommands(voiceCommands)
                    result.success(true)
                }

                "getAiSettings" -> {
                    result.success(
                        mapOf(
                            "aiEnabled" to repository.isAiEnabled(),
                            "instructions" to repository.getAiInstructions(),
                            "memory" to repository.getAiMemory(),
                            "wakeResponses" to repository.getWakeWordResponses()
                        )
                    )
                }

                "saveAiSettings" -> {
                    val aiEnabled = call.argument<Boolean>("aiEnabled") ?: true
                    val instructions = call.argument<String>("instructions") ?: ""
                    val memory = call.argument<String>("memory") ?: ""
                    val rawWakeList = call.argument<List<Any>>("wakeResponses")
                    repository.saveAiEnabled(aiEnabled)
                    repository.saveAiInstructions(instructions)
                    repository.saveAiMemory(memory)
                    if (rawWakeList != null) {
                        val stringList = rawWakeList.map { x: Any -> x.toString() }
                        repository.saveWakeWordResponses(stringList)
                    }
                    result.success(true)
                }

                "getSystemSettings" -> {
                    result.success(
                        mapOf(
                            "executionMode" to repository.getExecutionMode(),
                            "exportPath" to repository.getExportPath(),
                            "isDarkMode" to repository.isDarkMode()
                        )
                    )
                }

                "saveSystemSettings" -> {
                    val executionMode = call.argument<String>("executionMode") ?: "HYBRID"
                    val exportPath = call.argument<String>("exportPath") ?: ""
                    val isDarkMode = call.argument<Boolean>("isDarkMode") ?: true
                    repository.saveExecutionMode(executionMode)
                    repository.saveExportPath(exportPath)
                    repository.saveDarkMode(isDarkMode)
                    result.success(true)
                }

                "clearCache" -> {
                    repository.clearCache()
                    result.success(true)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestAudioPermission(): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasAudioPermission()) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    private fun startFloatingService() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun sendControlIntent(action: String) {
        val intent = Intent(this, FloatingButtonService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "$packageName/${DemoniAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled == 1) {
            val settingValue = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return settingValue?.contains(service) == true
        }
        return false
    }
}
