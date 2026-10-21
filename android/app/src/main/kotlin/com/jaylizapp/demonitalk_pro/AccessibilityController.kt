package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import java.util.Locale
import androidx.core.net.toUri

class AccessibilityController(
    private val context: Context
) {

    companion object {
        private const val TAG = "AccessibilityController"
    }

    fun execute(action: String): Boolean {
        val cleanAction = action.trim()

        if (cleanAction.isEmpty()) {
            return false
        }

        Log.d(TAG, "Ejecutando acción: $cleanAction")

        if (cleanAction.startsWith("open_app:")) {
            val appName = cleanAction.removePrefix("open_app:").trim()
            if (appName.isEmpty()) return false
            return launchAppByName(appName)
        }

        if (isPackageName(cleanAction)) {
            return launchPackage(cleanAction)
        }

        val service = DemoniAccessibilityService.instance

        if (service == null) {
            Log.w(TAG, "AccessibilityService no está activo para: $cleanAction. Intentando lanzamiento directo.")
            return false
        }

        return try {
            when {
                cleanAction.startsWith("type:") -> {
                    val text = cleanAction.removePrefix("type:")
                    Log.d(TAG, "Escribiendo texto: $text")
                    service.typeText(text)
                }

                cleanAction == "click_send" -> service.clickSendButton()
                cleanAction == "global_back" -> service.performBack()
                cleanAction == "global_home" -> service.performHome()
                cleanAction == "global_recents" -> service.performRecents()
                cleanAction == "click_first_chat" -> service.clickFirstConversation()

                cleanAction.startsWith("click_text:") -> {
                    val targetText = cleanAction.removePrefix("click_text:").trim()
                    if (targetText.isEmpty()) false else service.clickText(targetText)
                }

                cleanAction.startsWith("click_number:") -> {
                    val number = cleanAction.removePrefix("click_number:").trim()
                    if (number.isEmpty()) false else service.clickNumber(number)
                }

                cleanAction.startsWith("search_web:") -> {
                    val query = cleanAction.removePrefix("search_web:").trim()
                    if (query.isEmpty()) false else searchWeb(query)
                }

                cleanAction.startsWith("play_music:") -> {
                    val query = cleanAction.removePrefix("play_music:").trim()
                    if (query.isEmpty()) false else playMusic(query)
                }

                cleanAction == "media_next" -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                cleanAction == "media_previous" -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                cleanAction == "media_pause" -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                cleanAction == "media_play" -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                cleanAction == "show_grid" -> { service.showGrid(); true }
                cleanAction == "show_numbers" -> { service.showNumbers(); true }
                cleanAction == "hide_overlays" -> { service.hideOverlays(); true }

                else -> {
                    Log.w(TAG, "Acción no soportada: $cleanAction")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando acción de accesibilidad: $cleanAction", e)
            false
        }
    }

    private fun launchAppByName(name: String): Boolean {
        val searchName = name.trim().lowercase(Locale.getDefault())
        if (searchName.isEmpty()) return false

        val knownPackages = mapOf<String, String>(
            "whatsapp" to "com.whatsapp",
            "wasap" to "com.whatsapp",
            "wasat" to "com.whatsapp",
            "guatsap" to "com.whatsapp",
            "camara" to "com.android.camera",
            "camera" to "com.android.camera",
            "ajustes" to "com.android.settings",
            "configuracion" to "com.android.settings",
            "settings" to "com.android.settings",
            "youtube" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "tiktok" to "com.zhiliaoapp.musically",
            "spotify" to "com.spotify.music",
            "telegram" to "org.telegram.messenger",
            "calculadora" to "com.google.android.calculator",
            "galeria" to "com.google.android.apps.photos",
            "chrome" to "com.android.chrome"
        )

        val directPackage = knownPackages[searchName]
        if (directPackage != null) {
            val success = launchPackage(directPackage)
            if (success) return true
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = try {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando aplicaciones", e)
            emptyList<android.content.pm.ResolveInfo>()
        }

        val exactMatch = apps.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().trim().lowercase(Locale.getDefault())
            label == searchName
        }

        val foundApp = exactMatch ?: apps.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().trim().lowercase(Locale.getDefault())
            label.contains(searchName) || searchName.contains(label)
        }

        if (foundApp != null) {
            val packageName = foundApp.activityInfo.packageName
            return launchPackage(packageName)
        }

        Log.e(TAG, "Aplicación no encontrada: $name")
        return false
    }

    private fun searchWeb(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$query".toUri())
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun playMusic(query: String): Boolean {
        return try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendMediaKey(keyCode: Int): Boolean {
        if (ShellUtils.isRootAvailable()) {
            ShellUtils.executeCommand("input keyevent $keyCode")
            if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY) {
                ShellUtils.executeCommand("am broadcast -a android.intent.action.MEDIA_BUTTON --ei android.intent.extra.KEY_EVENT $keyCode")
                ShellUtils.executeCommand("input keyevent 85") 
            }
            return true
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val event = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            audioManager.dispatchMediaKeyEvent(event)
            val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventUp)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchPackage(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = try {
            pm.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo LaunchIntent para $packageName", e)
            null
        }

        if (intent == null) {
            Log.e(TAG, "No existe LaunchIntent para: $packageName")
            return false
        }

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            Log.d(TAG, "Aplicación lanzada exitosamente: $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo lanzar $packageName", e)
            false
        }
    }

    private fun isPackageName(value: String): Boolean {
        return value.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$"))
    }
}
