package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.UiState
import com.example.data.firebase.unwrapFirestoreStreamError
import com.example.data.model.VybeAccount
import com.example.data.model.VybeComment
import com.example.data.model.VybeConversation
import com.example.data.model.VybeMessage
import com.example.data.model.VybeNotification
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.data.model.VybeStory
import com.example.data.repository.VybeRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VybeViewModel(
    private val repository: VybeRepository,
    val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
    val selectedPostIdForComments: StateFlow<String?> = _selectedPostIdForComments.asStateFlow()

    val myAccount: StateFlow<VybeAccount?> = repository.observeAccount(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing account", root)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val myProfile: StateFlow<VybeProfile?> = repository.observeMyProfile(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing my profile", root)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val profilesState: StateFlow<UiState<List<VybeProfile>>> = repository.observeProfiles(currentUserId)
        .map<List<VybeProfile>, UiState<List<VybeProfile>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing profiles", root)
            emit(UiState.Error(root.message ?: "Failed to load creators"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val feedPostsState: StateFlow<UiState<List<VybePost>>> = repository.observeFeedPosts(currentUserId)
        .map<List<VybePost>, UiState<List<VybePost>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing posts", root)
            emit(UiState.Error(root.message ?: "Failed to load feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val stories: StateFlow<List<VybeStory>> = repository.observeStories(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing stories", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val likedPostIds: StateFlow<Set<String>> = repository.observeUserLikes(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing likes", root)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val followingIds: StateFlow<Set<String>> = repository.observeFollowingIds(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing follows", root)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val conversationsState: StateFlow<UiState<List<VybeConversation>>> = repository.observeConversations(currentUserId)
        .map<List<VybeConversation>, UiState<List<VybeConversation>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing conversations", root)
            emit(UiState.Error(root.message ?: "Failed to load conversations"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<VybeNotification>>> = repository.observeNotifications(currentUserId)
        .map<List<VybeNotification>, UiState<List<VybeNotification>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing notifications", root)
            emit(UiState.Error(root.message ?: "Failed to load notifications"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val activeMessagesState: StateFlow<UiState<List<VybeMessage>>> = _selectedConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf<UiState<List<VybeMessage>>>(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(convId, currentUserId)
                    .map<List<VybeMessage>, UiState<List<VybeMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreStreamError(error)
                        Log.w(TAG, "Error observing messages", root)
                        emit(UiState.Error(root.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))

    val activeCommentsState: StateFlow<UiState<List<VybeComment>>> = _selectedPostIdForComments
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf<UiState<List<VybeComment>>>(UiState.Success(emptyList()))
            } else {
                repository.observeComments(postId, currentUserId)
                    .map<List<VybeComment>, UiState<List<VybeComment>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreStreamError(error)
                        Log.w(TAG, "Error observing comments", root)
                        emit(UiState.Error(root.message ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))

    fun bootstrapUser(user: FirebaseUser) {
        viewModelScope.launch {
            repository.ensureAccountAndProfile(user).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to initialize profile")
            }
        }
    }

    fun selectConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun selectPostForComments(postId: String?) {
        _selectedPostIdForComments.value = postId
    }

    fun createPost(
        caption: String,
        vibeTag: String,
        mediaType: String = "text",
        photoDataUrl: String? = null,
        mediaDurationSec: Long = 0L,
        onDone: () -> Unit
    ) {
        val profile = myProfile.value ?: run {
            _events.tryEmit("Profile is still syncing. Please try again.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.createPost(profile, caption, vibeTag, mediaType, photoDataUrl, mediaDurationSec)
                .onSuccess {
                    _events.tryEmit("Vybe posted!")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not publish post.")
                }
            _isBusy.value = false
        }
    }

    fun editPost(postId: String, caption: String, vibeTag: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.editPost(postId, caption, vibeTag)
                .onSuccess {
                    _events.tryEmit("Post updated")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not update post.")
                }
            _isBusy.value = false
        }
    }

    fun deletePost(postId: String) {
        val count = myProfile.value?.postsCount ?: 0L
        viewModelScope.launch {
            repository.deletePost(postId, count)
                .onSuccess { _events.tryEmit("Post deleted") }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Failed to delete post.") }
        }
    }

    fun toggleLike(post: VybePost) {
        val profile = myProfile.value ?: return
        val liked = post.id in likedPostIds.value
        viewModelScope.launch {
            repository.toggleLike(post, liked, profile).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update like.")
            }
        }
    }

    fun addComment(post: VybePost, text: String, onDone: () -> Unit) {
        val profile = myProfile.value ?: return
        viewModelScope.launch {
            repository.addComment(post, profile, text)
                .onSuccess { onDone() }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Failed to add comment.") }
        }
    }

    fun deleteComment(post: VybePost, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(post.id, commentId, post.commentsCount).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete comment.")
            }
        }
    }

    fun toggleFollow(targetProfile: VybeProfile) {
        val profile = myProfile.value ?: return
        if (targetProfile.userId == profile.userId) return
        val currentlyFollowing = targetProfile.userId in followingIds.value
        viewModelScope.launch {
            repository.toggleFollow(profile, targetProfile, currentlyFollowing)
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not update follow status.")
                }
        }
    }

    fun createStory(
        caption: String,
        moodTag: String,
        accentGradient: String,
        mediaType: String = "text",
        mediaDataUrl: String? = null,
        onDone: () -> Unit
    ) {
        val profile = myProfile.value ?: run {
            _events.tryEmit("Profile is still syncing.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.createStory(profile, caption, moodTag, accentGradient, mediaType, mediaDataUrl)
                .onSuccess {
                    _events.tryEmit("Story shared for 24h!")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to share story.")
                }
            _isBusy.value = false
        }
    }

    fun deleteStory(storyId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
                .onSuccess {
                    _events.tryEmit("Story removed")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to remove story.")
                }
        }
    }

    fun startChatWith(peer: VybeProfile, onReady: (String) -> Unit) {
        val profile = myProfile.value ?: run {
            _events.tryEmit("Profile is still syncing.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.startOrGetConversation(profile, peer)
                .onSuccess { convId ->
                    _selectedConversationId.value = convId
                    onReady(convId)
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not open chat.")
                }
            _isBusy.value = false
        }
    }

    fun sendMessage(conversation: VybeConversation, text: String, onSent: () -> Unit) {
        val profile = myProfile.value ?: return
        viewModelScope.launch {
            repository.sendMessage(conversation, profile, text)
                .onSuccess { onSent() }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Failed to send message.") }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        moodTag: String,
        avatarColor: String,
        discoverable: Boolean,
        photoUrl: String?,
        photoDataUrl: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProfile(
                displayName = displayName,
                handle = handle,
                bio = bio,
                moodTag = moodTag,
                avatarColor = avatarColor,
                discoverable = discoverable,
                photoUrl = photoUrl,
                photoDataUrl = photoDataUrl
            ).onSuccess {
                _events.tryEmit("Profile saved!")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save profile.")
            }
            _isBusy.value = false
        }
    }

    fun updateAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAlertsEnabled(enabled).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update notification settings.")
            }
        }
    }

    fun markNotificationSeen(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationSeen(notificationId)
        }
    }

    fun markAllNotificationsSeen() {
        val current = (notificationsState.value as? UiState.Success)?.data ?: return
        val unreadIds = current.filter { !it.seen }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsSeen(unreadIds)
                .onSuccess { _events.tryEmit("All notifications marked as read") }
        }
    }

    fun clearNotification(notificationId: String) {
        viewModelScope.launch {
            repository.clearNotification(notificationId)
        }
    }

    private companion object {
        const val TAG = "VybeViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
