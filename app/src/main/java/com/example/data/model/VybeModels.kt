package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

val VIBE_TAGS = listOf("All", "Chill", "Creative", "Music", "Night Owl", "Hype")
val POST_VIBE_TAGS = listOf("Chill", "Creative", "Music", "Night Owl", "Hype")
val MOOD_TAGS = listOf("Night Owl", "Studio", "Exploring", "Vibing", "Focused", "Chill")
val AVATAR_COLOR_TOKENS = listOf("coral", "violet", "amber", "mint")

@Keep
@IgnoreExtraProperties
data class VybeAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val alertsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val moodTag: String = "Vibing",
    val avatarColor: String = "coral",
    val photoUrl: String? = null,
    val photoDataUrl: String? = null,
    val discoverable: Boolean = true,
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybePost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarColor: String = "coral",
    val authorPhotoUrl: String? = null,
    val caption: String = "",
    val vibeTag: String = "Chill",
    val mediaType: String = "text",
    val photoDataUrl: String? = null,
    val mediaDurationSec: Long = 0L,
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postVisibility: String = "public",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarColor: String = "coral",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeLike(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val postAuthorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeFollow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followedId: String = "",
    val followerName: String = "",
    val followedName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeStory(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarColor: String = "coral",
    val caption: String = "",
    val moodTag: String = "Vibing",
    val mediaType: String = "text",
    val mediaDataUrl: String? = null,
    val accentGradient: String = "coral",
    val visibility: String = "public",
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeConversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantAId: String = "",
    val participantAName: String = "",
    val participantAHandle: String = "",
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBHandle: String = "",
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerId(myUid: String): String =
        if (participantAId == myUid) participantBId else participantAId

    fun peerName(myUid: String): String =
        if (participantAId == myUid) participantBName else participantAName

    fun peerHandle(myUid: String): String =
        if (participantAId == myUid) participantBHandle else participantAHandle
}

@Keep
@IgnoreExtraProperties
data class VybeMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VybeNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "welcome",
    val title: String = "",
    val body: String = "",
    val targetId: String? = null,
    val seen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
