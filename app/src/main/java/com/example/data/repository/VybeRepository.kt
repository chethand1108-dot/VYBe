package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreStreamError
import com.example.data.model.AVATAR_COLOR_TOKENS
import com.example.data.model.VybeAccount
import com.example.data.model.VybeComment
import com.example.data.model.VybeConversation
import com.example.data.model.VybeFollow
import com.example.data.model.VybeLike
import com.example.data.model.VybeMessage
import com.example.data.model.VybeNotification
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.data.model.VybeStory
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VybeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing VYBE data.")

    fun observeAccount(userId: String): Flow<VybeAccount?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "accounts/$userId"
        emitAll(
            db.collection("accounts").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        VybeAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw root
                }
        )
    }

    fun observeProfiles(userId: String): Flow<List<VybeProfile>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "profiles"
        emitAll(
            db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.followersCount + it.postsCount }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeMyProfile(userId: String): Flow<VybeProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "profiles/$userId"
        emitAll(
            db.collection("profiles").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        VybeProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw root
                }
        )
    }

    fun observeFeedPosts(userId: String): Flow<List<VybePost>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybePost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeComments(postId: String, userId: String): Flow<List<VybeComment>> = flow {
        require(postId.isNotBlank()) { "Post ID must not be blank." }
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postVisibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeStories(userId: String): Flow<List<VybeStory>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "stories"
        emitAll(
            db.collection("stories")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(
                        VybeStory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.expiresAtEpochMs > now }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeUserLikes(userId: String): Flow<Set<String>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "likes"
        emitAll(
            db.collection("likes")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeLike::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.postId }.toSet()
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "follows"
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeFollow::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.followedId }.toSet()
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeConversations(userId: String): Flow<List<VybeConversation>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeConversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<VybeMessage>> = flow {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank." }
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<VybeNotification>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VybeNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val root = unwrapFirestoreStreamError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    suspend fun ensureAccountAndProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        return try {
            val accountRef = db.collection("accounts").document(uid)
            val profileRef = db.collection("profiles").document(uid)
            val accountSnap = accountRef.get().await()
            val profileSnap = profileRef.get().await()

            if (accountSnap.exists() && profileSnap.exists()) {
                return Result.success(Unit)
            }

            val rawName = (user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Vybe Creator").trim().take(60)
            val baseHandle = rawName.lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .ifBlank { "vyber" }
            val handle = "${baseHandle.take(24)}_${uid.take(4).lowercase()}"
                .take(32)
                .padEnd(2, 'x')
            val avatarColor = AVATAR_COLOR_TOKENS[kotlin.math.abs(uid.hashCode()) % AVATAR_COLOR_TOKENS.size]

            val batch = db.batch()
            if (!accountSnap.exists()) {
                batch.set(
                    accountRef,
                    mapOf(
                        "userId" to uid,
                        "email" to (user.email ?: "").take(200),
                        "alertsEnabled" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (!profileSnap.exists()) {
                val profileData = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to rawName,
                    "handle" to handle,
                    "bio" to "Sharing moments & soundwaves on VYBE.",
                    "moodTag" to "Vibing",
                    "avatarColor" to avatarColor,
                    "photoUrl" to user.photoUrl?.toString()?.take(500),
                    "discoverable" to true,
                    "postsCount" to 0L,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(profileRef, profileData)

                val welcomeNotifRef = db.collection("users").document(uid)
                    .collection("notifications").document()
                batch.set(
                    welcomeNotifRef,
                    mapOf(
                        "recipientId" to uid,
                        "actorId" to uid,
                        "actorName" to rawName,
                        "type" to "welcome",
                        "title" to "Welcome to VYBE",
                        "body" to "Your profile @$handle is live. Drop your first Vybe, share a Story, or connect in Explore!",
                        "seen" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "accounts/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        moodTag: String,
        avatarColor: String,
        discoverable: Boolean,
        photoUrl: String?,
        photoDataUrl: String?
    ): Result<Unit> {
        var path = "profiles"
        return try {
            val uid = requireUserId()
            path = "profiles/$uid"
            val cleanName = displayName.trim()
            val cleanHandle = handle.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "_")
            val cleanBio = bio.trim()
            val cleanMood = moodTag.trim().ifBlank { "Vibing" }

            require(cleanName.length in 1..60) { "Display name must be 1–60 characters." }
            require(cleanHandle.length in 2..32) { "Handle must be 2–32 characters." }
            require(cleanBio.length <= 240) { "Bio must be at most 240 characters." }
            require(cleanMood.length <= 40) { "Mood tag must be at most 40 characters." }
            require(avatarColor in AVATAR_COLOR_TOKENS) { "Invalid avatar accent color." }
            if (photoDataUrl != null) {
                require(photoDataUrl.length <= 150_000) { "Avatar image exceeds maximum size." }
            }

            val updates = mutableMapOf<String, Any?>(
                "displayName" to cleanName,
                "handle" to cleanHandle,
                "bio" to cleanBio,
                "moodTag" to cleanMood,
                "avatarColor" to avatarColor,
                "discoverable" to discoverable,
                "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
                "photoDataUrl" to photoDataUrl?.takeIf { it.isNotBlank() },
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("profiles").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAlertsEnabled(enabled: Boolean): Result<Unit> {
        var path = "accounts"
        return try {
            val uid = requireUserId()
            path = "accounts/$uid"
            db.collection("accounts").document(uid).update(
                mapOf(
                    "alertsEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        profile: VybeProfile,
        caption: String,
        vibeTag: String,
        mediaType: String = "text",
        photoDataUrl: String? = null,
        mediaDurationSec: Long = 0L
    ): Result<Unit> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Profile mismatch; cannot post for another user." }
            val cleanCaption = caption.trim()
            val cleanTag = vibeTag.trim().ifBlank { "Chill" }
            require(cleanCaption.length in 1..500) { "Caption must be 1–500 characters." }
            require(cleanTag.length in 1..32) { "Vibe tag must be 1–32 characters." }
            require(mediaType in listOf("text", "photo", "video")) { "Unsupported media type." }
            if (photoDataUrl != null) {
                require(photoDataUrl.length <= 220_000) { "Selected media thumbnail is too large." }
            }

            val postRef = db.collection("posts").document()
            path = postRef.path
            val profileRef = db.collection("profiles").document(uid)

            val payload = mutableMapOf<String, Any?>(
                "authorId" to uid,
                "authorName" to profile.displayName.take(60),
                "authorHandle" to profile.handle.take(32),
                "authorAvatarColor" to profile.avatarColor,
                "authorPhotoUrl" to profile.photoUrl?.takeIf { it.isNotBlank() }?.take(500),
                "caption" to cleanCaption,
                "vibeTag" to cleanTag,
                "mediaType" to mediaType,
                "photoDataUrl" to photoDataUrl,
                "mediaDurationSec" to mediaDurationSec.coerceIn(0L, 3600L),
                "visibility" to "public",
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(postRef, payload)
            batch.update(
                profileRef,
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun editPost(postId: String, caption: String, vibeTag: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID must not be blank." }
            val cleanCaption = caption.trim()
            val cleanTag = vibeTag.trim().ifBlank { "Chill" }
            require(cleanCaption.length in 1..500) { "Caption must be 1–500 characters." }
            require(cleanTag.length in 1..32) { "Vibe tag must be 1–32 characters." }

            db.collection("posts").document(postId).update(
                mapOf(
                    "caption" to cleanCaption,
                    "vibeTag" to cleanTag,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, currentPostsCount: Long): Result<Unit> {
        val path = "posts/$postId"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank()) { "Post ID must not be blank." }
            val batch = db.batch()
            batch.delete(db.collection("posts").document(postId))
            if (currentPostsCount > 0L) {
                batch.update(
                    db.collection("profiles").document(uid),
                    mapOf(
                        "postsCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: VybePost,
        actorProfile: VybeProfile,
        text: String
    ): Result<Unit> {
        val path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            check(uid == actorProfile.userId) { "Profile mismatch." }
            require(post.id.isNotBlank()) { "Post ID must not be blank." }
            val cleanText = text.trim()
            require(cleanText.length in 1..300) { "Comment must be 1–300 characters." }

            val postRef = db.collection("posts").document(post.id)
            val commentRef = postRef.collection("comments").document()

            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to post.id,
                    "postVisibility" to "public",
                    "authorId" to uid,
                    "authorName" to actorProfile.displayName.take(60),
                    "authorHandle" to actorProfile.handle.take(32),
                    "authorAvatarColor" to actorProfile.avatarColor,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.authorId != uid && post.authorId.isNotBlank()) {
                val notifRef = db.collection("users").document(post.authorId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to post.authorId,
                        "actorId" to uid,
                        "actorName" to actorProfile.displayName.take(60),
                        "type" to "comment",
                        "title" to "${actorProfile.displayName.take(40)} commented on your Vybe",
                        "body" to cleanText.take(200),
                        "targetId" to post.id,
                        "seen" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String, currentCommentsCount: Long): Result<Unit> {
        val path = "posts/$postId/comments/$commentId"
        return try {
            requireUserId()
            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)
            val batch = db.batch()
            batch.delete(commentRef)
            if (currentCommentsCount > 0L) {
                batch.update(
                    postRef,
                    mapOf(
                        "commentsCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(
        post: VybePost,
        currentlyLiked: Boolean,
        actorProfile: VybeProfile
    ): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID must not be blank." }
            val likeId = "${post.id}_$uid"
            val likeRef = db.collection("likes").document(likeId)
            val postRef = db.collection("posts").document(post.id)

            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "postId" to post.id,
                        "userId" to uid,
                        "postAuthorId" to post.authorId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (post.authorId != uid && post.authorId.isNotBlank()) {
                    val notifRef = db.collection("users").document(post.authorId)
                        .collection("notifications").document()
                    batch.set(
                        notifRef,
                        mapOf(
                            "recipientId" to post.authorId,
                            "actorId" to uid,
                            "actorName" to actorProfile.displayName.take(60),
                            "type" to "like",
                            "title" to "${actorProfile.displayName.take(40)} liked your Vybe",
                            "body" to post.caption.take(160),
                            "targetId" to post.id,
                            "seen" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        myProfile: VybeProfile,
        targetProfile: VybeProfile,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val followId = "${myProfile.userId}_${targetProfile.userId}"
        val path = "follows/$followId"
        return try {
            val uid = requireUserId()
            check(uid == myProfile.userId) { "Profile mismatch." }
            require(targetProfile.userId != uid) { "Cannot follow yourself." }

            val followRef = db.collection("follows").document(followId)
            val myRef = db.collection("profiles").document(uid)
            val targetRef = db.collection("profiles").document(targetProfile.userId)

            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
                if (myProfile.followingCount > 0L) {
                    batch.update(
                        myRef,
                        mapOf(
                            "followingCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (targetProfile.followersCount > 0L) {
                    batch.update(
                        targetRef,
                        mapOf(
                            "followersCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "followedId" to targetProfile.userId,
                        "followerName" to myProfile.displayName.take(60),
                        "followedName" to targetProfile.displayName.take(60),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    myRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val notifRef = db.collection("users").document(targetProfile.userId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to targetProfile.userId,
                        "actorId" to uid,
                        "actorName" to myProfile.displayName.take(60),
                        "type" to "follow",
                        "title" to "${myProfile.displayName.take(40)} started following you",
                        "body" to "@${myProfile.handle} is now following your Vybes.",
                        "targetId" to uid,
                        "seen" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        profile: VybeProfile,
        caption: String,
        moodTag: String,
        accentGradient: String,
        mediaType: String = "text",
        mediaDataUrl: String? = null
    ): Result<Unit> {
        var path = "stories"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Profile mismatch." }
            val cleanCaption = caption.trim()
            val cleanMood = moodTag.trim().ifBlank { "Vibing" }
            require(cleanCaption.length <= 180) { "Story text must be at most 180 characters." }
            require(cleanCaption.isNotBlank() || !mediaDataUrl.isNullOrBlank()) {
                "Add a caption or select a photo/video for your story."
            }
            require(accentGradient in AVATAR_COLOR_TOKENS) { "Invalid gradient." }
            if (mediaDataUrl != null) {
                require(mediaDataUrl.length <= 220_000) { "Story image exceeds max size." }
            }

            val storyRef = db.collection("stories").document()
            path = storyRef.path
            val payload = mutableMapOf<String, Any?>(
                "authorId" to uid,
                "authorName" to profile.displayName.take(60),
                "authorHandle" to profile.handle.take(32),
                "authorAvatarColor" to profile.avatarColor,
                "caption" to cleanCaption,
                "moodTag" to cleanMood.take(32),
                "mediaType" to mediaType,
                "mediaDataUrl" to mediaDataUrl,
                "accentGradient" to accentGradient,
                "visibility" to "public",
                "expiresAtEpochMs" to (System.currentTimeMillis() + 24L * 60L * 60L * 1000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            storyRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        val path = "stories/$storyId"
        return try {
            requireUserId()
            db.collection("stories").document(storyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun startOrGetConversation(
        myProfile: VybeProfile,
        peerProfile: VybeProfile
    ): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            check(uid == myProfile.userId) { "Profile mismatch." }
            require(peerProfile.userId != uid) { "Choose another member to message." }

            val sorted = listOf(myProfile, peerProfile).sortedBy { it.userId }
            val first = sorted[0]
            val second = sorted[1]
            val convId = "${first.userId}_${second.userId}"
            path = "conversations/$convId"

            val convRef = db.collection("conversations").document(convId)
            val existing = convRef.get().await()
            if (!existing.exists()) {
                convRef.set(
                    mapOf(
                        "participantIds" to listOf(first.userId, second.userId),
                        "participantAId" to first.userId,
                        "participantAName" to first.displayName.take(60),
                        "participantAHandle" to first.handle.take(32),
                        "participantBId" to second.userId,
                        "participantBName" to second.displayName.take(60),
                        "participantBHandle" to second.handle.take(32),
                        "lastMessageText" to "",
                        "lastSenderId" to "",
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: VybeConversation,
        senderProfile: VybeProfile,
        text: String
    ): Result<Unit> {
        val path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            check(uid == senderProfile.userId) { "Sender mismatch." }
            val cleanText = text.trim()
            require(cleanText.length in 1..1000) { "Message must be 1–1000 characters." }

            val convRef = db.collection("conversations").document(conversation.id)
            val msgRef = convRef.collection("messages").document()
            val peerId = conversation.peerId(uid)

            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to conversation.id,
                    "senderId" to uid,
                    "senderName" to senderProfile.displayName.take(60),
                    "participantIds" to conversation.participantIds,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to cleanText.take(300),
                    "lastSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (peerId.isNotBlank() && peerId != uid) {
                val notifRef = db.collection("users").document(peerId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to peerId,
                        "actorId" to uid,
                        "actorName" to senderProfile.displayName.take(60),
                        "type" to "message",
                        "title" to "Message from ${senderProfile.displayName.take(40)}",
                        "body" to cleanText.take(200),
                        "targetId" to conversation.id,
                        "seen" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationSeen(notificationId: String): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            require(notificationId.isNotBlank()) { "Notification ID must not be blank." }
            path = "users/$uid/notifications/$notificationId"
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update(mapOf("seen" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsSeen(unreadIds: List<String>): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            if (unreadIds.isEmpty()) return Result.success(Unit)
            path = "users/$uid/notifications"
            val batch = db.batch()
            unreadIds.take(100).forEach { id ->
                val ref = db.collection("users").document(uid)
                    .collection("notifications").document(id)
                batch.update(ref, mapOf("seen" to true))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearNotification(notificationId: String): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            path = "users/$uid/notifications/$notificationId"
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
