package com.lean.cleaner

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private data class AppItem(val pkg: String, val label: String, val system: Boolean, val enabled: Boolean)
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private val all = ArrayList<AppItem>()
    private val shown = ArrayList<AppItem>()
    private val sel = HashSet<String>()
    private var locked: Set<String> = emptySet()
    private var sysOnly = true
    private var root = false
    private lateinit var status: TextView
    private lateinit var adapter: BaseAdapter
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111418.toInt()); setPadding(dp(8), dp(8), dp(8), dp(8)) }
        status = TextView(this).apply { setTextColor(0xFFB8C0CC.toInt()); textSize = 13f }
        col.addView(status)
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(CheckBox(this).apply { text = "System only"; isChecked = true; setTextColor(Color.WHITE); setOnCheckedChangeListener { _, c -> sysOnly = c; filter() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(btn("All") { shown.filter { !Protect.isProtected(this, it.pkg) }.forEach { sel.add(it.pkg) }; adapter.notifyDataSetChanged(); refreshStatus() })
        bar.addView(btn("None") { sel.clear(); adapter.notifyDataSetChanged(); refreshStatus() })
        col.addView(bar)
        adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int) = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, p: ViewGroup?): View {
                val c = (v as? CheckBox) ?: CheckBox(this@MainActivity).apply { isClickable = false; isFocusable = false; setTextColor(0xFFE6EAF0.toInt()); textSize = 13f }
                val a = shown[i]
                c.text = a.label + (if (!a.enabled) " [OFF]" else "") + (if (a.pkg in locked) " [LOCK]" else "") + "\n" + a.pkg
                c.isChecked = a.pkg in sel
                return c
            }
        }
        val lv = ListView(this).apply {
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, i, _ ->
                val a = shown[i]
                if (Protect.isProtected(this@MainActivity, a.pkg)) { toast("Protected"); return@setOnItemClickListener }
                if (!sel.add(a.pkg)) sel.remove(a.pkg)
                this@MainActivity.adapter.notifyDataSetChanged(); refreshStatus()
            }
        }
        col.addView(lv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val r1 = LinearLayout(this); r1.addView(btn("Boost") { act("boost") }); r1.addView(btn("Cache") { act("cache") }); r1.addView(btn("Force stop") { act("force") })
        val r2 = LinearLayout(this); r2.addView(btn("Clear data") { act("clear") }); r2.addView(btn("Disable") { act("off") }); r2.addView(btn("Enable") { act("on") })
        col.addView(r1); col.addView(r2); setContentView(col)
        Enforcer.schedule(this)
        io.execute { root = Shell.root(); ui.post { refreshStatus() } }
        load()
    }
    override fun onResume() { super.onResume(); refreshStatus() }
    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; textSize = 12f; isAllCaps = false; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { f() } }
    private fun freeMb(): Long { val m = ActivityManager.MemoryInfo(); (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(m); return m.availMem / 1048576 }
    private fun refreshStatus() { status.text = (if (root) "ROOT" else "NO ROOT") + " | RAM free ${freeMb()} MB | ${sel.size} sel | ${locked.size} locked" }
    private fun load() {
        io.execute {
            val pm = packageManager
            val l = pm.getInstalledApplications(PackageManager.GET_META_DATA).map { AppItem(it.packageName, pm.getApplicationLabel(it).toString(), it.flags and ApplicationInfo.FLAG_SYSTEM != 0, it.enabled) }.sortedWith(compareBy({ !it.system }, { it.label.lowercase() }))
            ui.post { all.clear(); all.addAll(l); locked = Store.blocked(this); filter() }
        }
    }
    private fun filter() { shown.clear(); shown.addAll(all.filter { !sysOnly || it.system }); adapter.notifyDataSetChanged(); refreshStatus() }
    private fun act(k: String) {
        when (k) {
            "boost" -> boost()
            "cache" -> cache()
            else -> {
                val t = sel.filter { !Protect.isProtected(this, it) }
                if (t.isEmpty()) { toast("Select apps"); return }
                if (k == "clear" || k == "off") AlertDialog.Builder(this).setMessage("$k: ${t.size} apps?").setPositiveButton("Yes") { _, _ -> exec(k, t) }.setNegativeButton("No", null).show() else exec(k, t)
            }
        }
    }
    private fun exec(k: String, t: List<String>) {
        if (root) {
            val cmd = t.joinToString("; ") { when (k) { "force" -> "am force-stop $it"; "clear" -> "pm clear $it"; "off" -> "pm disable-user --user 0 $it; am force-stop $it"; else -> "pm enable $it" } }
            io.execute { Shell.run(cmd, true); ui.post { if (k == "off") Store.addBlocked(this, t); if (k == "on") Store.removeBlocked(this, t); toast("Done"); load() } }
            return
        }
        when (k) {
            "force", "clear" -> { val s = AutoClickService.inst; if (s == null) { toast("Enable accessibility service"); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } else { toast("Do not touch screen"); s.enqueue(t, if (k == "force") AutoClickService.FORCE else AutoClickService.CLEAR) } }
            else -> { if (k == "off") Store.addBlocked(this, t) else Store.removeBlocked(this, t); locked = Store.blocked(this); adbDialog(k, t) }
        }
    }
    private fun adbDialog(k: String, t: List<String>) {
        val script = t.joinToString("\n") { if (k == "off") "adb shell pm disable-user --user 0 $it" else "adb shell pm enable $it" }
        val tv = TextView(this).apply { text = "USB debugging ON. Connect PC. Run:\n\n$script"; setTextIsSelectable(true); setPadding(dp(16), dp(8), dp(16), dp(8)) }
        AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(tv) }).setPositiveButton("Copy") { _, _ -> (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("adb", script)); toast("Copied") }.setNegativeButton("Close", null).show()
    }
    private fun boost() {
        val before = freeMb()
        io.execute {
            val pk = packageManager.getInstalledApplications(0).map { it.packageName }.filter { !Protect.isProtected(this, it) }
            if (root) Shell.run(pk.joinToString("; ") { "am force-stop $it" }, true) else { val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager; pk.forEach { am.killBackgroundProcesses(it) } }
            Thread.sleep(800); ui.post { toast("RAM $before -> ${freeMb()} MB"); refreshStatus() }
        }
    }
    private fun cache() {
        if (root) { io.execute { Shell.run("pm trim-caches 999999999999", true); ui.post { toast("Cache cleared") } }; return }
        try { startActivity(Intent("android.os.storage.action.CLEAR_APP_CACHE")) } catch (e: Exception) { startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)); toast("Tap Cached data > OK") }
    }
}
