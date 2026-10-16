package com.lean.cleaner

import android.content.Context

object Store {
    private fun p(c: Context) = c.getSharedPreferences("s", Context.MODE_PRIVATE)
    fun blocked(c: Context): Set<String> = HashSet(p(c).getStringSet("b", emptySet()) ?: emptySet())
    fun addBlocked(c: Context, l: Collection<String>) { p(c).edit().putStringSet("b", HashSet(blocked(c) + l)).apply() }
    fun removeBlocked(c: Context, l: Collection<String>) { p(c).edit().putStringSet("b", HashSet(blocked(c) - l.toSet())).apply() }
}
