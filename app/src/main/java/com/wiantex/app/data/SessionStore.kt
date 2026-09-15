package com.wiantex.app.data

import android.content.Context
import java.net.HttpURLConnection

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("wiantex_session", Context.MODE_PRIVATE)

    var csrfToken: String
        get() = prefs.getString(KEY_CSRF, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CSRF, value).apply() }

    fun applyTo(connection: HttpURLConnection) {
        val cookie = prefs.getString(KEY_COOKIE, null)
        if (!cookie.isNullOrBlank()) connection.setRequestProperty("Cookie", cookie)
    }

    fun captureFrom(connection: HttpURLConnection) {
        val setCookies = connection.headerFields
            .filterKeys { it != null && it.equals("Set-Cookie", ignoreCase = true) }
            .values
            .flatten()
        if (setCookies.isEmpty()) return

        val merged = linkedMapOf<String, String>()
        prefs.getString(KEY_COOKIE, null)
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.contains('=') }
            ?.forEach { pair ->
                val name = pair.substringBefore('=').trim()
                val value = pair.substringAfter('=', "").trim()
                if (name.isNotBlank() && value.isNotBlank()) merged[name] = value
            }

        setCookies.forEach { raw ->
            val pair = raw.substringBefore(';').trim()
            if (!pair.contains('=')) return@forEach
            val name = pair.substringBefore('=').trim()
            val value = pair.substringAfter('=', "").trim()
            if (name.isBlank()) return@forEach
            if (value.isBlank()) merged.remove(name) else merged[name] = value
        }

        prefs.edit().putString(
            KEY_COOKIE,
            merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
        ).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasSession(): Boolean = !prefs.getString(KEY_COOKIE, null).isNullOrBlank()

    private companion object {
        const val KEY_COOKIE = "cookie"
        const val KEY_CSRF = "csrf"
    }
}
