package com.wiantex.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wiantex.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WiantexViewModel(application: Application) : AndroidViewModel(application) {
    private val api = WiantexApi(application)

    var authChecked by mutableStateOf(false)
        private set
    var user by mutableStateOf<User?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    var forumFeed by mutableStateOf(ForumFeed())
        private set
    var profile by mutableStateOf<Profile?>(null)
        private set
    var notifications by mutableStateOf(NotificationsFeed())
        private set
    var pulses by mutableStateOf<List<Pulse>>(emptyList())
        private set
    var market by mutableStateOf(MarketFeed())
        private set

    var activeChatUsername by mutableStateOf("")
        private set
    var messages by mutableStateOf<List<DirectMessage>>(emptyList())
        private set

    init {
        restoreSession()
    }

    fun dismissError() { error = null }
    fun dismissInfo() { info = null }

    fun login(login: String, password: String) = runBusy {
        val loggedIn = io { api.login(login.trim(), password) }
        user = loggedIn
        refreshAllCore()
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { api.logout() }
            user = null
            profile = null
            messages = emptyList()
            activeChatUsername = ""
            forumFeed = ForumFeed()
            pulses = emptyList()
            market = MarketFeed()
            notifications = NotificationsFeed()
        }
    }

    fun refreshMe() = runBusy {
        user = io { api.me() }
    }

    fun loadForum(page: Int = 1) = runBusy {
        forumFeed = io { api.forum(page) }
    }

    fun loadProfile(username: String? = null) = runBusy {
        profile = io { api.profile(username) }
    }

    fun loadNotifications() = runBusy {
        notifications = io { api.notifications() }
    }

    fun loadPulses() = runBusy {
        pulses = io { api.pulses() }
    }

    fun togglePulseLike(pulseId: Int) = runBusy {
        val (liked, count) = io { api.togglePulseLike(pulseId) }
        pulses = pulses.map { pulse ->
            if (pulse.id == pulseId) pulse.copy(liked = liked, likesCount = count) else pulse
        }
    }

    fun loadMarket() = runBusy {
        market = io { api.market() }
    }

    fun buyProduct(productId: Int) = runBusy {
        info = io { api.buyMarketProduct(productId) }
        market = io { api.market() }
        user = io { api.me() }
    }

    fun openConversation(username: String) = runBusy {
        activeChatUsername = username.trim().removePrefix("@")
        if (activeChatUsername.isBlank()) {
            messages = emptyList()
        } else {
            messages = io { api.messages(activeChatUsername) }
        }
    }

    fun refreshConversation() {
        val username = activeChatUsername
        if (username.isNotBlank()) openConversation(username)
    }

    fun sendMessage(content: String, onSent: (() -> Unit)? = null) {
        if (activeChatUsername.isBlank() || content.isBlank()) return
        runBusy {
            val sent = io { api.sendMessage(activeChatUsername, content.trim()) }
            messages = messages + sent
            onSent?.invoke()
        }
    }

    fun absoluteUrl(path: String?): String? = api.absoluteUrl(path)

    private fun restoreSession() {
        viewModelScope.launch {
            if (!api.hasSession()) {
                authChecked = true
                return@launch
            }
            try {
                user = withContext(Dispatchers.IO) { api.me() }
                refreshAllCore()
            } catch (_: Throwable) {
                api.clearSession()
                user = null
            } finally {
                authChecked = true
            }
        }
    }

    private suspend fun refreshAllCore() {
        runCatching { io { api.forum() } }.onSuccess { forumFeed = it }
        runCatching { io { api.profile() } }.onSuccess { profile = it }
        runCatching { io { api.notifications() } }.onSuccess { notifications = it }
        runCatching { io { api.pulses() } }.onSuccess { pulses = it }
        runCatching { io { api.market() } }.onSuccess { market = it }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (t: Throwable) {
                error = t.message ?: "Beklenmeyen bir hata oluştu."
                if (t is ApiException && t.statusCode == 401) {
                    api.clearSession()
                    user = null
                }
            } finally {
                busy = false
                authChecked = true
            }
        }
    }
}
