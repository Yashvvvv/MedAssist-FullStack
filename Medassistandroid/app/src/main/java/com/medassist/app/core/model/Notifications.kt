package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class InboxItem(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val read: Boolean,
    val createdAt: String,
)

@Serializable
data class UnreadCount(val unread: Long)
