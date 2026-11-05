package com.jaylizapp.demonitalk_pro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import java.util.Locale

class DemoniAccessibilityService : AccessibilityService() {

    private lateinit var dualSpeechManager: DualSpeechManager
    private lateinit var commandHandler: CommandHandler
    private lateinit var repository: CommandRepository
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    private var windowManager: WindowManager? = null
    private var gridOverlay: FrameLayout? = null
    private var numbersOverlay: FrameLayout? = null
    private var gridVisible = false
    private var numbersVisible = false

    private val numberedNodes = mutableMapOf<Int, AccessibilityNodeInfo>()
    private val numberedBounds = mutableMapOf<Int, Rect>()

    companion object {
        private const val TAG = "DemoniTalk"
        var instance: DemoniAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        commandHandler = CommandHandler(this)
        repository = CommandRepository(this)
        dualSpeechManager = DualSpeechManager(
            context = this,
            repository = repository,
            onReady = { },
            onSpeechResults = { text ->
                if (text.isNotBlank()) {
                    commandHandler.execute(text, repository.loadCommands())
                }
            },
            onErrorRestart = { },
            isModeActive = { isListening },
            onPartialSpeechResults = null
        )
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "ACTION_START_LISTENING" -> startListening()
            "ACTION_STOP_LISTENING" -> stopListening()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startListening() {
        mainHandler.post {
            if (isListening) return@post
            try {
                isListening = true
                dualSpeechManager.startListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting accessibility listening", e)
            }
        }
    }

    private fun stopListening() {
        mainHandler.post {
            isListening = false
            try { dualSpeechManager.stopListening() } catch (_: Exception) {}
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hideOverlays()
        try { dualSpeechManager.destroy() } catch (_: Exception) {}
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (numbersVisible || gridVisible) {
            mainHandler.removeCallbacksAndMessages(NUMBER_REFRESH_TOKEN)
            mainHandler.postDelayed({ refreshOverlays() }, 50L)
        }
    }

    override fun onInterrupt() {}

    fun typeText(text: String): Boolean {
        if (text.isBlank()) {
            Log.w(TAG, "Se ignoró typeText porque el contenido está vacío.")
            return false
        }

        try {
            val rootNode = rootInActiveWindow ?: return false
            var targetNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

            if (targetNode == null) {
                targetNode = findEditableNode(rootNode)
            }

            if (targetNode == null) {
                Log.w(TAG, "No se encontró ningún campo de texto editable.")
                return false
            }

            if (!targetNode.isFocused) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }

            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                Log.d(TAG, "Texto ingresado exitosamente via accesibilidad: $text")
            } else {
                Log.w(TAG, "Falló ACTION_SET_TEXT via accesibilidad en el nodo.")
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error en typeText: ${e.message}", e)
            return false
        }
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            if (node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val editable = findEditableNode(child)
                if (editable != null) return editable
            }
        } catch (_: Exception) {}
        return null
    }

    fun clickSendButton(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val composer = findMessageComposer(rootNode)
        val draft = composer?.text?.toString()?.trim().orEmpty()
        if (draft.isEmpty()) {
            Log.w(TAG, "No se pulsó Enviar: el campo de mensaje está vacío o no se encontró.")
            return false
        }

        val commonIds = listOf(
            "com.whatsapp:id/send",
            "com.google.android.apps.messaging:id/send_message_button_container"
        )

        for (id in commonIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (node.isClickable || node.parent?.isClickable == true) {
                    val target = if (node.isClickable) node else node.parent
                    if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
                }
            }
        }

        val sendWords = listOf("enviar", "send", "mandar", "enviar mensaje", "post", "publicar")
        return findAndClickByTextOrDesc(rootNode, sendWords)
    }

    private fun findMessageComposer(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val composerIds = listOf(
            "com.whatsapp:id/entry",
            "com.google.android.apps.messaging:id/compose_message_text"
        )

        for (id in composerIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            val composer = nodes.firstOrNull { it.isEditable } ?: nodes.firstOrNull()
            if (composer != null) return composer
        }

        return findEditableNode(rootNode)
    }

    private fun findAndClickByTextOrDesc(node: AccessibilityNodeInfo, words: List<String>): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""

        for (word in words) {
            if ((desc.contains(word) || text.contains(word)) && (node.isClickable || node.parent?.isClickable == true)) {
                val target = if (node.isClickable) node else node.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findAndClickByTextOrDesc(child, words)) return true
        }

        return false
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow
        if (root != null && performScrollOnNode(root, scrollForward = false)) {
            Log.d(TAG, "Scroll arriba realizado en nodo scrollable")
            return true
        }
        return performGestureScroll(up = true)
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow
        if (root != null && performScrollOnNode(root, scrollForward = true)) {
            Log.d(TAG, "Scroll abajo realizado en nodo scrollable")
            return true
        }
        return performGestureScroll(up = false)
    }

    private fun performScrollOnNode(node: AccessibilityNodeInfo, scrollForward: Boolean): Boolean {
        try {
            val className = node.className?.toString()?.lowercase() ?: ""
            val isScrollableList = className.contains("scroll") ||
                    className.contains("list") ||
                    className.contains("recycler") ||
                    className.contains("pager") ||
                    className.contains("grid")

            if (isScrollableList) {
                val action = if (scrollForward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (node.performAction(action)) return true
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (performScrollOnNode(child, scrollForward)) return true
            }
        } catch (_: Exception) {}
        return false
    }

    private fun performGestureScroll(up: Boolean): Boolean {
        try {
            val metrics = resources.displayMetrics
            val cx = metrics.widthPixels / 2f
            val startY = if (up) metrics.heightPixels * 0.3f else metrics.heightPixels * 0.7f
            val endY = if (up) metrics.heightPixels * 0.7f else metrics.heightPixels * 0.3f

            val path = Path().apply {
                moveTo(cx, startY)
                lineTo(cx, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 250)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error en performGestureScroll: ${e.message}", e)
            return false
        }
    }

    fun clickFirstConversation(): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        val listIds = listOf(
            "com.whatsapp:id/conversations_list",
            "com.whatsapp:id/conversations_row",
            "android:id/list",
            "org.telegram.messenger:id/chats_list"
        )

        for (id in listIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (clickFirstChild(node)) return true
            }
        }

        if (findAndClickFirstListElement(rootNode)) {
            return true
        }

        val clickableCandidates = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(rootNode, clickableCandidates)

        val topChatCandidate = clickableCandidates.firstOrNull { node ->
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.top > 120 && rect.width() > 300 && rect.height() > 80
        }

        if (topChatCandidate != null) {
            clickNodeOrParent(topChatCandidate)
            return true
        }

        return false
    }

    private fun findAndClickFirstListElement(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""
        if (className.contains("ListView") || className.contains("RecyclerView")) {
            if (clickFirstChild(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickFirstListElement(child)) return true
        }
        return false
    }

    private fun clickFirstChild(listNode: AccessibilityNodeInfo): Boolean {
        if (listNode.childCount <= 0) return false
        for (i in 0 until listNode.childCount) {
            val child = listNode.getChild(i) ?: continue
            if (child.isClickable || child.parent?.isClickable == true) {
                clickNodeOrParent(child)
                return true
            }
        }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo) {
        val target = if (node.isClickable) node else node.parent
        target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun clickText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)

        for (node in nodes) {
            if (node.isClickable || node.parent?.isClickable == true) {
                clickNodeOrParent(node)
                return true
            }
        }
        return findAndClickByTextOrDesc(rootNode, listOf(targetText.lowercase(Locale.getDefault())))
    }

    fun clickNumber(number: String): Boolean {
        val num = number.trim().toIntOrNull() ?: return false

        val storedNode = numberedNodes[num]
        if (storedNode != null) {
            try {
                val target = if (storedNode.isClickable) storedNode else storedNode.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    hideOverlays()
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pulsando nodo número $num", e)
            }
        }

        val bounds = numberedBounds[num]
        if (bounds != null) {
            val cx = bounds.centerX()
            val cy = bounds.centerY()

            if (ShellUtils.isRootAvailable()) {
                if (ShellUtils.executeCommand("input tap $cx $cy")) {
                    hideOverlays()
                    return true
                }
            }

            try {
                val path = Path().apply { moveTo(cx.toFloat(), cy.toFloat()) }
                val stroke = GestureDescription.StrokeDescription(path, 0, 50)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                if (dispatchGesture(gesture, null, null)) {
                    hideOverlays()
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en dispatchGesture para número $num", e)
            }
        }

        val textSuccess = clickText(num.toString())
        if (textSuccess) hideOverlays()
        return textSuccess
    }

    fun showGrid() {
        mainHandler.post {
            gridVisible = true
            ensureGridOverlay()
            gridOverlay?.visibility = FrameLayout.VISIBLE
            refreshOverlays()
        }
    }

    fun showNumbers() {
        mainHandler.post {
            numbersVisible = true
            rebuildNumberMap()
            ensureNumbersOverlay()
            numbersOverlay?.visibility = FrameLayout.VISIBLE
            refreshOverlays()
        }
    }

    fun hideOverlays() {
        mainHandler.post {
            gridVisible = false
            numbersVisible = false
            numberedNodes.clear()
            numberedBounds.clear()
            removeGridOverlay()
            removeNumbersOverlay()
        }
    }

    private fun refreshOverlays() {
        mainHandler.post {
            if (numbersVisible) {
                rebuildNumberMap()
                ensureNumbersOverlay()
                drawNumbers()
            }
            if (gridVisible) {
                ensureGridOverlay()
                drawGrid()
            }
        }
    }

    private fun rebuildNumberMap() {
        numberedNodes.clear()
        numberedBounds.clear()

        val root = rootInActiveWindow ?: return
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, candidates)

        val valid = candidates.filter { node ->
            if (!node.isVisibleToUser) return@filter false
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.width() > 10 && rect.height() > 10
        }

        val sorted = valid.sortedWith(
            compareBy<AccessibilityNodeInfo> {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                rect.top
            }.thenBy {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                rect.left
            }
        )

        var number = 1
        for (node in sorted) {
            val rect = Rect()
            node.getBoundsInScreen(rect)

            val duplicate = numberedBounds.values.any { existing ->
                val overlapX = Math.max(0, Math.min(rect.right, existing.right) - Math.max(rect.left, existing.left))
                val overlapY = Math.max(0, Math.min(rect.bottom, existing.bottom) - Math.max(rect.top, existing.top))
                val overlapArea = overlapX * overlapY
                val minArea = Math.min(rect.width() * rect.height(), existing.width() * existing.height())
                minArea > 0 && (overlapArea.toFloat() / minArea.toFloat()) > 0.7f
            }

            if (duplicate) continue

            numberedNodes[number] = node
            numberedBounds[number] = Rect(rect)

            number++
            if (number > 99) break
        }
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        try {
            if (node.isVisibleToUser && (node.isClickable || node.isLongClickable)) {
                result.add(node)
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectClickableNodes(child, result)
            }
        } catch (_: Exception) {}
    }

    private fun ensureGridOverlay() {
        if (gridOverlay != null) return
        val wm = windowManager ?: return

        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(Color.TRANSPARENT)
        val params = createOverlayParams()

        try {
            wm.addView(overlay, params)
            gridOverlay = overlay
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo crear overlay de cuadrícula", e)
        }
    }

    private fun drawGrid() {
        val overlay = gridOverlay ?: return
        overlay.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val columns = 4
        val rows = 4
        val cellWidth = screenWidth / columns
        val cellHeight = screenHeight / rows

        for (column in 1 until columns) {
            val line = ViewLine(this, Color.argb(150, 255, 0, 0))
            val params = FrameLayout.LayoutParams(dp(1), screenHeight)
            params.leftMargin = column * cellWidth
            overlay.addView(line, params)
        }

        for (row in 1 until rows) {
            val line = ViewLine(this, Color.argb(150, 255, 0, 0))
            val params = FrameLayout.LayoutParams(screenWidth, dp(1))
            params.topMargin = row * cellHeight
            overlay.addView(line, params)
        }

        val border = ViewLine(this, Color.argb(180, 255, 0, 0))
        val borderParams = FrameLayout.LayoutParams(screenWidth, dp(2))
        overlay.addView(border, borderParams)

        val bottomBorder = ViewLine(this, Color.argb(180, 255, 0, 0))
        val bottomParams = FrameLayout.LayoutParams(screenWidth, dp(2))
        bottomParams.topMargin = screenHeight - dp(2)
        overlay.addView(bottomBorder, bottomParams)
    }

    private fun ensureNumbersOverlay() {
        if (numbersOverlay != null) return
        val wm = windowManager ?: return

        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(Color.TRANSPARENT)
        val params = createOverlayParams()

        try {
            wm.addView(overlay, params)
            numbersOverlay = overlay
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo crear overlay de números", e)
        }
    }

    private fun drawNumbers() {
        val overlay = numbersOverlay ?: return
        overlay.removeAllViews()

        val density = resources.displayMetrics.density

        for ((number, bounds) in numberedBounds) {
            val label = TextView(this).apply {
                text = number.toString()
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding((3 * density).toInt(), 0, (3 * density).toInt(), 0)

                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 10f * density
                    setColor(Color.argb(215, 18, 18, 22))
                    setStroke((1.5f * density).toInt(), Color.argb(240, 255, 6, 0))
                }
            }

            val badgeHeight = (22f * density).toInt()
            val badgeWidth = if (number > 9) (26f * density).toInt() else (22f * density).toInt()

            val params = FrameLayout.LayoutParams(badgeWidth, badgeHeight).apply {
                leftMargin = (bounds.left + (2 * density).toInt()).coerceIn(0, (resources.displayMetrics.widthPixels - badgeWidth).coerceAtLeast(0))
                topMargin = (bounds.top + (2 * density).toInt()).coerceIn(0, (resources.displayMetrics.heightPixels - badgeHeight).coerceAtLeast(0))
            }

            overlay.addView(label, params)
        }
    }

    private fun createOverlayParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun removeGridOverlay() {
        val overlay = gridOverlay ?: return
        try { windowManager?.removeView(overlay) } catch (_: Exception) {}
        gridOverlay = null
    }

    private fun removeNumbersOverlay() {
        val overlay = numbersOverlay ?: return
        try { windowManager?.removeView(overlay) } catch (_: Exception) {}
        numbersOverlay = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val NUMBER_REFRESH_TOKEN = Any()

    private class ViewLine(context: Context, color: Int) : android.view.View(context) {
        init { setBackgroundColor(color) }
    }
}
