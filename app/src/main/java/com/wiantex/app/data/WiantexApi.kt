package com.wiantex.app.data

import android.content.Context
import com.wiantex.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WiantexApi(context: Context) {
    private val session = SessionStore(context.applicationContext)

    fun hasSession(): Boolean = session.hasSession()
    fun clearSession() = session.clear()

    fun logout() {
        if (session.hasSession()) runCatching { request("api/native/logout", method = "POST") }
        session.clear()
    }

    fun login(login: String, password: String): User {
        val json = request(
            path = "api/native/login",
            method = "POST",
            body = JSONObject().put("login", login).put("password", password),
        )
        rememberCsrf(json)
        return parseUser(json.getJSONObject("user"))
    }

    fun me(): User {
        val json = request("api/native/me")
        rememberCsrf(json)
        return parseUser(json.getJSONObject("user"))
    }

    fun forum(page: Int = 1): ForumFeed {
        val json = request("api/native/forum?page=$page")
        val categories = json.optJSONArray("categories").objects().map { item ->
            ForumCategory(
                id = item.optInt("id"),
                name = firstString(item, "name", "title"),
                slug = firstString(item, "slug"),
            )
        }
        val topics = json.optJSONArray("topics").objects().map { item ->
            ForumTopic(
                id = item.optInt("id"),
                title = firstString(item, "title", "name").ifBlank { "Başlıksız konu" },
                slug = firstString(item, "slug"),
                username = firstString(item, "username", "author_username", "user_name"),
                category = firstString(item, "category_name", "category", "category_title"),
                replyCount = firstInt(item, "reply_count", "replies_count", "post_count", "replies"),
                createdAt = firstString(item, "created_at", "updated_at"),
            )
        }
        return ForumFeed(
            categories = categories,
            topics = topics,
            page = json.optInt("page", page),
            total = json.optInt("total", topics.size),
        )
    }

    fun profile(username: String? = null): Profile {
        val suffix = username?.takeIf { it.isNotBlank() }?.let {
            "?username=${encode(it.removePrefix("@"))}"
        }.orEmpty()
        val json = request("api/native/profile$suffix")
        val p = json.getJSONObject("profile")
        return Profile(
            id = p.optInt("id"),
            username = p.optString("username"),
            email = p.nullableString("email"),
            bio = p.optString("bio"),
            avatarPath = p.nullableString("avatar_path"),
            bannerPath = p.nullableString("banner_path"),
            roleName = p.optString("role_name"),
            roleSlug = p.optString("role_slug"),
            roleColor = p.nullableString("role_color"),
            equippedFramePath = p.nullableString("equipped_frame_path"),
            topicCount = p.optInt("topic_count"),
            postCount = p.optInt("post_count"),
            likeCount = p.optInt("like_count"),
            presenceStatus = p.nullableString("presence_status"),
            createdAt = p.nullableString("created_at"),
        )
    }

    fun messages(username: String): List<DirectMessage> {
        val json = request("api/native/messages?username=${encode(username.removePrefix("@"))}")
        rememberCsrf(json)
        return json.optJSONArray("messages").objects().map { m ->
            DirectMessage(
                id = m.optInt("id"),
                senderId = m.optInt("sender_id"),
                receiverId = m.optInt("receiver_id"),
                content = m.optString("content"),
                createdAt = m.optString("created_at"),
                mine = m.optBoolean("mine"),
                attachmentPath = m.nullableString("attachment_path"),
                attachmentType = m.nullableString("attachment_type"),
                deleted = m.optBoolean("is_deleted"),
            )
        }
    }

    fun sendMessage(username: String, content: String): DirectMessage {
        val json = request(
            path = "api/native/messages",
            method = "POST",
            body = JSONObject()
                .put("username", username.removePrefix("@"))
                .put("content", content)
                .put("csrf_token", session.csrfToken),
        )
        val m = json.getJSONObject("message")
        return DirectMessage(
            id = m.optInt("id"),
            senderId = m.optInt("sender_id"),
            receiverId = m.optInt("receiver_id"),
            content = m.optString("content"),
            createdAt = m.optString("created_at"),
            mine = m.optBoolean("mine", true),
        )
    }

    fun notifications(): NotificationsFeed {
        val json = request("api/native/notifications")
        val items = json.optJSONArray("notifications").objects().map { n ->
            val data = n.optJSONObject("data") ?: JSONObject()
            val type = n.optString("type")
            val actor = n.nullableString("actor_username")
            val text = when (type) {
                "message" -> "${actor ?: "Bir kullanıcı"} sana mesaj gönderdi."
                "like" -> "${actor ?: "Bir kullanıcı"} içeriğini beğendi."
                "follow" -> "${actor ?: "Bir kullanıcı"} seni takip etti."
                else -> firstString(data, "message", "text", "title").ifBlank { type }
            }
            WiantexNotification(
                id = n.optInt("id"),
                type = type,
                actorUsername = actor,
                actorAvatar = n.nullableString("actor_avatar"),
                createdAt = n.optString("created_at"),
                read = !n.nullableString("read_at").isNullOrBlank(),
                text = text,
            )
        }
        return NotificationsFeed(json.optInt("unread"), items)
    }

    fun pulses(): List<Pulse> {
        val json = request("api/native/pulses")
        rememberCsrf(json)
        return json.optJSONArray("pulses").objects().map { p ->
            Pulse(
                id = p.optInt("id"),
                username = p.optString("username"),
                avatarPath = p.nullableString("avatar_path"),
                videoPath = p.nullableString("video_url") ?: p.nullableString("video_path"),
                caption = firstString(p, "caption", "body", "description", "title"),
                likesCount = firstInt(p, "likes_count", "like_count"),
                commentsCount = firstInt(p, "comments_count", "comment_count"),
                sharesCount = firstInt(p, "shares_count", "share_count"),
                liked = p.optBoolean("liked"),
                createdAt = p.optString("created_at"),
            )
        }
    }

    fun togglePulseLike(id: Int): Pair<Boolean, Int> {
        val json = request(
            path = "api/native/pulses",
            method = "POST",
            body = JSONObject()
                .put("action", "toggle_like")
                .put("pulse_id", id)
                .put("csrf_token", session.csrfToken),
        )
        return json.optBoolean("liked") to json.optInt("likes_count")
    }

    fun market(): MarketFeed {
        val json = request("api/native/market")
        rememberCsrf(json)
        val products = json.optJSONArray("products").objects().map { p ->
            MarketProduct(
                id = p.optInt("id"),
                sellerId = p.optInt("seller_id"),
                username = p.optString("username"),
                title = p.optString("title"),
                description = p.optString("description"),
                priceWoin = p.optInt("price_woin"),
                imagePath = p.nullableString("image_url") ?: p.nullableString("image_path"),
                sold = p.optBoolean("is_sold"),
            )
        }
        return MarketFeed(balance = json.optInt("woin_balance"), products = products)
    }

    fun buyMarketProduct(productId: Int): String {
        val json = request(
            path = "api/native/market",
            method = "POST",
            body = JSONObject()
                .put("action", "buy_product")
                .put("product_id", productId)
                .put("csrf_token", session.csrfToken),
        )
        return json.optString("message", "Satın alma tamamlandı.")
    }

    fun absoluteUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return try {
            URI(BuildConfig.API_BASE_URL).resolve(path.removePrefix("/")).toString()
        } catch (_: Throwable) {
            BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
    }

    private fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject {
        val url = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/'))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Wiantex-Android/${BuildConfig.VERSION_NAME}")
            session.applyTo(this)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }

            val code = connection.responseCode
            session.captureFrom(connection)
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            }.orEmpty()

            val json = try {
                if (text.isBlank()) JSONObject().put("ok", code in 200..299) else JSONObject(text)
            } catch (_: Throwable) {
                throw ApiException("Sunucu JSON yerine beklenmeyen bir yanıt döndürdü.", code)
            }

            if (code !in 200..299 || !json.optBoolean("ok", true)) {
                throw ApiException(json.optString("message", "Wiantex sunucu hatası ($code)"), code)
            }
            rememberCsrf(json)
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun rememberCsrf(json: JSONObject) {
        json.optString("csrf_token").takeIf { it.isNotBlank() }?.let { session.csrfToken = it }
    }

    private fun parseUser(u: JSONObject) = User(
        id = u.optInt("id"),
        username = u.optString("username"),
        email = u.nullableString("email"),
        bio = u.optString("bio"),
        avatarPath = u.nullableString("avatar_path"),
        bannerPath = u.nullableString("banner_path"),
        roleName = u.nullableString("role_name"),
        roleSlug = u.nullableString("role_slug"),
        roleColor = u.nullableString("role_color"),
        equippedFramePath = u.nullableString("equipped_frame_path"),
        woinBalance = u.optInt("woin_balance"),
        presenceStatus = u.nullableString("presence_status"),
        createdAt = u.nullableString("created_at"),
        lastSeenAt = u.nullableString("last_seen_at"),
    )

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

private fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) optJSONObject(i)?.let(::add)
    }
}

private fun JSONObject.nullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() && it != "null" }
}

private fun firstString(obj: JSONObject, vararg keys: String): String {
    for (key in keys) {
        val value = obj.nullableString(key)
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

private fun firstInt(obj: JSONObject, vararg keys: String): Int {
    for (key in keys) if (obj.has(key) && !obj.isNull(key)) return obj.optInt(key)
    return 0
}
