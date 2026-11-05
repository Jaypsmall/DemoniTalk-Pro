package com.jaylizapp.demonitalk_pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.jaylizapp.demonitalk_pro/channel"
    private val PERMISSION_REQUEST_CODE = 101
    private val IMPORT_JSON_REQUEST_CODE = 102
    private val EXPORT_JSON_REQUEST_CODE = 103
    private lateinit var repository: CommandRepository
    private var ttsManager: TtsManager? = null
    private var pendingJsonResult: MethodChannel.Result? = null
    private var pendingJsonExport: String? = null
    private var methodChannel: MethodChannel? = null
    private var pendingSecretModeRequest = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        repository = CommandRepository(this)

        checkAndRequestAudioPermission()

        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel = channel
        activeChannel = channel
        channel.setMethodCallHandler { call, result ->
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

                "pickJsonFile" -> pickJsonFile(result)

                "saveJsonFile" -> {
                    val content = call.argument<String>("content") ?: ""
                    val fileName = call.argument<String>("fileName") ?: "demonitalk_backup.json"
                    saveJsonFile(content, fileName, result)
                }

                "getAiSettings" -> {
                    result.success(
                        mapOf(
                            "aiEnabled" to repository.isAiEnabled(),
                            "instructions" to repository.getAiInstructions(),
                            "memory" to repository.getAiMemory(),
                            "groqApiKey" to repository.getGroqApiKey(),
                            "wakeResponses" to repository.getWakeWordResponses()
                        )
                    )
                }

                "saveAiSettings" -> {
                    val aiEnabled = call.argument<Boolean>("aiEnabled") ?: true
                    val instructions = call.argument<String>("instructions") ?: ""
                    val memory = call.argument<String>("memory") ?: ""
                    val groqApiKey = call.argument<String>("groqApiKey") ?: ""
                    val rawWakeList = call.argument<List<Any>>("wakeResponses")
                    repository.saveAiEnabled(aiEnabled)
                    repository.saveAiInstructions(instructions)
                    repository.saveAiMemory(memory)
                    repository.saveGroqApiKey(groqApiKey)
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
                            "speechEngine" to repository.getSpeechEngine(),
                            "exportPath" to repository.getExportPath(),
                            "isDarkMode" to repository.isDarkMode()
                        )
                    )
                }

                "saveSystemSettings" -> {
                    val executionMode = call.argument<String>("executionMode") ?: "HYBRID"
                    val speechEngine = call.argument<String>("speechEngine") ?: "VOSK"
                    val exportPath = call.argument<String>("exportPath") ?: ""
                    val isDarkMode = call.argument<Boolean>("isDarkMode") ?: true
                    repository.saveExecutionMode(executionMode)
                    repository.saveSpeechEngine(speechEngine)
                    repository.saveExportPath(exportPath)
                    repository.saveDarkMode(isDarkMode)
                    result.success(true)
                }

                "clearCache" -> {
                    repository.clearCache()
                    result.success(true)
                }

                "speakText" -> {
                    val text = call.argument<String>("text") ?: ""
                    if (text.isNotBlank()) {
                        if (ttsManager == null) {
                            ttsManager = TtsManager(this)
                        }
                        ttsManager?.speakText(text)
                    }
                    result.success(true)
                }

                "isSecretRequested" -> {
                    val requested = pendingSecretModeRequest || intent?.action == "ACTION_OPEN_SECRET_MODE"
                    pendingSecretModeRequest = false
                    result.success(requested)
                }

                "saveSecretMode" -> {
                    val active = call.argument<Boolean>("active") ?: false
                    repository.saveSecretMode(active)
                    val action = if (active) "ACTION_START_SECRET_LISTENING" else "ACTION_STOP_SECRET_LISTENING"
                    val intent = Intent(this, FloatingButtonService::class.java).apply {
                        this.action = action
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    result.success(true)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    companion object {
        private var activeChannel: MethodChannel? = null

        fun notifySecretDemonSpeech(input: String, response: String) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                activeChannel?.invokeMethod(
                    "onSecretDemonSpeech",
                    mapOf("input" to input, "response" to response)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == "ACTION_OPEN_SECRET_MODE") {
            methodChannel?.invokeMethod("openSecretMode", null)
                ?: run { pendingSecretModeRequest = true }
        }
    }

    private fun pickJsonFile(result: MethodChannel.Result) {
        if (pendingJsonResult != null) {
            result.error("BUSY", "Ya hay una operación de archivos en curso", null)
            return
        }
        pendingJsonResult = result
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain", "application/octet-stream"))
        }
        try {
            startActivityForResult(intent, IMPORT_JSON_REQUEST_CODE)
        } catch (e: Exception) {
            pendingJsonResult = null
            result.error("PICK_JSON_FAILED", e.message, null)
        }
    }

    private fun saveJsonFile(content: String, fileName: String, result: MethodChannel.Result) {
        if (pendingJsonResult != null) {
            result.error("BUSY", "Ya hay una operación de archivos en curso", null)
            return
        }
        pendingJsonResult = result
        pendingJsonExport = content
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        try {
            startActivityForResult(intent, EXPORT_JSON_REQUEST_CODE)
        } catch (e: Exception) {
            pendingJsonResult = null
            pendingJsonExport = null
            result.error("SAVE_JSON_FAILED", e.message, null)
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != IMPORT_JSON_REQUEST_CODE && requestCode != EXPORT_JSON_REQUEST_CODE) return

        val result = pendingJsonResult ?: return
        pendingJsonResult = null

        if (resultCode != RESULT_OK || data?.data == null) {
            pendingJsonExport = null
            result.success(if (requestCode == IMPORT_JSON_REQUEST_CODE) null else false)
            return
        }

        val uri = data.data!!
        try {
            if (requestCode == IMPORT_JSON_REQUEST_CODE) {
                val fileName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment.orEmpty()
                if (!fileName.endsWith(".json", ignoreCase = true)) {
                    result.error("INVALID_FILE", "Selecciona un documento .json", null)
                    return
                }
                val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("No se pudo leer el archivo")
                result.success(json)
            } else {
                val content = pendingJsonExport ?: ""
                pendingJsonExport = null
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                    ?: throw IllegalStateException("No se pudo guardar el archivo")
                result.success(true)
            }
        } catch (e: Exception) {
            pendingJsonExport = null
            result.error("JSON_FILE_ERROR", e.message, null)
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

    override fun onRestart() {
        try {
            super.onRestart()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error en onRestart: ${e.message}")
        }
    }

    override fun onPostResume() {
        try {
            super.onPostResume()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error en onPostResume: ${e.message}")
        }
    }
}
