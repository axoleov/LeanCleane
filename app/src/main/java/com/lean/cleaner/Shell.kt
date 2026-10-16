package com.lean.cleaner

object Shell {
    @Volatile private var rootCache: Boolean? = null
    fun root(): Boolean {
        rootCache?.let { return it }
        val ok = run("id", true).first.contains("uid=0")
        rootCache = ok
        return ok
    }
    fun run(cmd: String, su: Boolean): Pair<String, Int> = try {
        val args = if (su) listOf("su", "-c", cmd) else listOf("sh", "-c", cmd)
        val p = ProcessBuilder(args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        Pair(out.trim(), p.waitFor())
    } catch (e: Exception) { Pair(e.message ?: "error", -1) }
}
