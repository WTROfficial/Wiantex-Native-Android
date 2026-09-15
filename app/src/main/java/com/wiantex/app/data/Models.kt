package com.wiantex.app.data

data class User(
    val id: Int,
    val username: String,
    val email: String? = null,
    val bio: String = "",
    val avatarPath: String? = null,
    val bannerPath: String? = null,
    val roleName: String? = null,
    val roleSlug: String? = null,
    val roleColor: String? = null,
    val equippedFramePath: String? = null,
    val woinBalance: Int = 0,
    val presenceStatus: String? = null,
    val createdAt: String? = null,
    val lastSeenAt: String? = null,
)

data class ForumCategory(
    val id: Int = 0,
    val name: String,
    val slug: String = "",
)

data class ForumTopic(
    val id: Int,
    val title: String,
    val slug: String = "",
    val username: String = "",
    val category: String = "",
    val replyCount: Int = 0,
    val createdAt: String = "",
)

data class ForumFeed(
    val categories: List<ForumCategory> = emptyList(),
    val topics: List<ForumTopic> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
)

data class DirectMessage(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val createdAt: String,
    val mine: Boolean,
    val attachmentPath: String? = null,
    val attachmentType: String? = null,
    val deleted: Boolean = false,
)

data class Profile(
    val id: Int,
    val username: String,
    val email: String? = null,
    val bio: String = "",
    val avatarPath: String? = null,
    val bannerPath: String? = null,
    val roleName: String = "",
    val roleSlug: String = "",
    val roleColor: String? = null,
    val equippedFramePath: String? = null,
    val topicCount: Int = 0,
    val postCount: Int = 0,
    val likeCount: Int = 0,
    val presenceStatus: String? = null,
    val createdAt: String? = null,
)

data class WiantexNotification(
    val id: Int,
    val type: String,
    val actorUsername: String? = null,
    val actorAvatar: String? = null,
    val createdAt: String = "",
    val read: Boolean = false,
    val text: String = "",
)

data class NotificationsFeed(
    val unread: Int = 0,
    val items: List<WiantexNotification> = emptyList(),
)

data class Pulse(
    val id: Int,
    val username: String,
    val avatarPath: String? = null,
    val videoPath: String? = null,
    val caption: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val liked: Boolean = false,
    val createdAt: String = "",
)

data class MarketProduct(
    val id: Int,
    val sellerId: Int,
    val username: String,
    val title: String,
    val description: String,
    val priceWoin: Int,
    val imagePath: String? = null,
    val sold: Boolean = false,
)

data class MarketFeed(
    val balance: Int = 0,
    val products: List<MarketProduct> = emptyList(),
)

class ApiException(message: String, val statusCode: Int = 0) : Exception(message)
