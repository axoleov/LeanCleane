package com.lean.cleaner

import android.content.Context
import android.content.Intent
import android.provider.Settings

object Protect {
    private val fixed = setOf("android", "com.android.systemui", "com.android.settings", "com.android.phone", "com.android.server.telecom", "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.bluetooth", "com.android.nfc", "com.android.shell", "com.android.keychain", "com.android.certinstaller", "com.android.inputdevices")
    fun isProtected(c: Context, pkg: String): Boolean {
        if (pkg == c.packageName || pkg in fixed || pkg.startsWith("com.android.providers.")) return true
        val home = c.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        if (pkg == home) return true
        val ime = Settings.Secure.getString(c.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
        return pkg == ime
    }
}
