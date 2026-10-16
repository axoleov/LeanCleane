package com.lean.cleaner

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutoClickService : AccessibilityService() {
    companion object {
        @Volatile var inst: AutoClickService? = null
        const val FORCE = 0
        const val CLEAR = 1
        private val FORCE_W = setOf("force stop", "force close", "强行停止", "强制停止")
        private val CLEAR_W = setOf("clear data", "clear storage", "清除数据", "清除存储空间")
        private val STORAGE_W = setOf("storage", "storage & cache", "storage usage", "存储", "存储空间")
        private val OK_W = setOf("ok", "yes", "clear", "delete", "确定", "确认", "清除", "删除")
    }
    private val h = Handler(Looper.getMainLooper())
    private val q = ArrayDeque<String>()
    private var mode = FORCE
    private var cur: String? = null
    private var tick = 0
    private var confirming = false
    private var running = false
    private val loop = object : Runnable { override fun run() { if (!running) return; if (!step()) h.postDelayed(this, 700) } }
    override fun onServiceConnected() { inst = this }
    override fun onUnbind(intent: Intent?): Boolean { inst = null; running = false; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    fun enqueue(pkgs: List<String>, m: Int) { q.clear(); q.addAll(pkgs); mode = m; running = true; next() }
    private fun next() {
        h.removeCallbacks(loop); cur = q.removeFirstOrNull(); tick = 0; confirming = false
        val p = cur
        if (p == null) { running = false; performGlobalAction(GLOBAL_ACTION_HOME); Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show(); return }
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$p")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        try { startActivity(i) } catch (e: Exception) { next(); return }
        h.postDelayed(loop, 1000)
    }
    private fun step(): Boolean {
        if (++tick > 16) { next(); return true }
        val root = rootInActiveWindow ?: return false
        val pn = root.packageName?.toString() ?: return false
        if (!pn.contains("settings") && pn != "android") return false
        if (confirming) {
            val b = root.findAccessibilityNodeInfosByViewId("android:id/button1").firstOrNull() ?: find(root, OK_W) ?: return false
            clickNode(b); if (mode == CLEAR) performGlobalAction(GLOBAL_ACTION_BACK); next(); return true
        }
        if (mode == FORCE) { val n = find(root, FORCE_W) ?: return false; if (!n.isEnabled) { next(); return true }; clickNode(n); confirming = true }
        else { val n = find(root, CLEAR_W); if (n != null) { if (!n.isEnabled) { next(); return true }; clickNode(n); confirming = true } else find(root, STORAGE_W)?.let { clickNode(it) } }
        return false
    }
    private fun find(n: AccessibilityNodeInfo?, words: Set<String>): AccessibilityNodeInfo? {
        if (n == null) return null
        val t = n.text?.toString()?.trim()?.lowercase()
        if (t != null && t in words) return n
        for (i in 0 until n.childCount) find(n.getChild(i), words)?.let { return it }
        return null
    }
    private fun clickNode(n: AccessibilityNodeInfo) { var x: AccessibilityNodeInfo? = n; while (x != null && !x.isClickable) x = x.parent; x?.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
}
