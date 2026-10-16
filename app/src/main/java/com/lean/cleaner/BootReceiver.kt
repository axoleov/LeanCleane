package com.lean.cleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val r = goAsync()
        Enforcer.schedule(c)
        Thread { try { Enforcer.runOnce(c) } finally { r.finish() } }.start()
    }
}
