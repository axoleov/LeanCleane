package com.lean.cleaner

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/** Periodically re-applies the user's blocked-app policy when root access is available. */
class Enforcer : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            try { runOnce(this) } finally { jobFinished(params, false) }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB_ID = 43127

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val info = JobInfo.Builder(JOB_ID, ComponentName(context, Enforcer::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build()
            scheduler.schedule(info)
        }

        fun runOnce(context: Context) {
            if (!Shell.root()) return
            val packages = Store.blocked(context).filterNot { Protect.isProtected(context, it) }
            if (packages.isNotEmpty()) {
                val commands = packages.joinToString("; ") { "am force-stop $it" }
                Shell.run(commands, true)
            }
        }
    }
}
