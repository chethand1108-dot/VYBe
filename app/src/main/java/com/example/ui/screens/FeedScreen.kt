package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.UiState
import com.example.data.media.MediaEncoder
import com.example.data.model.AVATAR_COLOR_TOKENS
import com.example.data.model.MOOD_TAGS
import com.example.data.model.POST_VIBE_TAGS
import com.example.data.model.VIBE_TAGS
import com.example.data.model.VybeComment
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.data.model.VybeStory
import com.example.ui.components.VybeAvatar
import com.example.ui.components.VybeEmptyState
import com.example.ui.components.VybeMediaPreview
import com.example.ui.components.VybePostCard
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.components.storyGradientBrush
import com.example.ui.theme.VybeCoral
import com.example.ui.theme.VybeViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    currentUserId: String,
    myProfile: VybeProfile?,
    postsState: UiState<List<VybePost>>,
    stories: List<VybeStory>,
    likedPostIds: Set<String>,
    commentsState: UiState<List<VybeComment>>,
    isBusy: Boolean,
    showCreatePostSheet: Boolean,
    onDismissCreatePost: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onCreatePost: (caption: String, vibeTag: String, mediaType: String, photoDataUrl: String?, durationSec: Long) -> Unit,
    onEditPost: (postId: String, caption: String, vibeTag: String) -> Unit,
    onDeletePost: (postId: String) -> Unit,
    onToggleLike: (VybePost) -> Unit,
    onOpenComments: (VybePost) -> Unit,
    onCloseComments: () -> Unit,
    onAddComment: (VybePost, String, () -> Unit) -> Unit,
    onDeleteComment: (VybePost, String) -> Unit,
    onCreateStory: (caption: String, moodTag: String, accent: String, mediaType: String, mediaDataUrl: String?, () -> Unit) -> Unit,
    onDeleteStory: (String, () -> Unit) -> Unit,
    onAuthorClick: (String) -> Unit,
    onMessageAuthor: (String) -> Unit
) {
    var selectedTag by remember { mutableStateOf("All") }
    var editingPost by remember { mutableStateOf<VybePost?>(null) }
    var activeCommentsPost by remember { mutableStateOf<VybePost?>(null) }
    var showCreateStory by remember { mutableStateOf(false) }
    var viewingStoryIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
    ) {
        // Stories Rail
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "add_story") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showCreateStory = true }
                        .padding(vertical = 4.dp)
                        .testTag("add_story_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(2.dp, Brush.linearGradient(listOf(VybeCoral, VybeViolet)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add story",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your Story",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            items(stories.indices.toList(), key = { idx -> stories[idx].id }) { idx ->
                val story = stories[idx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewingStoryIndex = idx }
                        .padding(vertical = 4.dp)
                ) {
                    VybeAvatar(
                        displayName = story.authorName,
                        avatarColor = story.authorAvatarColor,
                        size = 62.dp,
                        showRing = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = story.authorName.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Vibe Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VIBE_TAGS) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = tag },
                    label = { Text(tag) }
                )
            }
        }

        when (postsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                VybeEmptyState(
                    title = "Couldn't load the feed",
                    subtitle = postsState.message
                )
            }
            is UiState.Success -> {
                val filtered = remember(postsState.data, selectedTag) {
                    if (selectedTag == "All") postsState.data
                    else postsState.data.filter { it.vibeTag.equals(selectedTag, ignoreCase = true) }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        VybeEmptyState(
                            title = if (selectedTag == "All") "No Vybes yet" else "No $selectedTag Vybes yet",
                            subtitle = "Be the first to drop a thought, photo, or studio clip.",
                            actionLabel = "Drop a Vybe",
                            onAction = onOpenCreatePost
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { post ->
                            VybePostCard(
                                post = post,
                                isOwner = post.authorId == currentUserId,
                                isLiked = post.id in likedPostIds,
                                onAuthorClick = { onAuthorClick(post.authorId) },
                                onLikeClick = { onToggleLike(post) },
                                onCommentClick = {
                                    activeCommentsPost = post
                                    onOpenComments(post)
                                },
                                onMessageAuthorClick = { onMessageAuthor(post.authorId) },
                                onEditClick = { editingPost = post },
                                onDeleteClick = { onDeletePost(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreatePostSheet) {
        CreatePostSheet(
            isBusy = isBusy,
            initialCaption = "",
            initialTag = POST_VIBE_TAGS.first(),
            isEditing = false,
            onDismiss = onDismissCreatePost,
            onSubmit = { caption, tag, mediaType, dataUrl, durationSec ->
                onCreatePost(caption, tag, mediaType, dataUrl, durationSec)
            }
        )
    }

    editingPost?.let { post ->
        CreatePostSheet(
            isBusy = isBusy,
            initialCaption = post.caption,
            initialTag = post.vibeTag,
            isEditing = true,
            onDismiss = { editingPost = null },
            onSubmit = { caption, tag, _, _, _ ->
                onEditPost(post.id, caption, tag)
                editingPost = null
            }
        )
    }

    activeCommentsPost?.let { post ->
        CommentsSheet(
            post = post,
            currentUserId = currentUserId,
            commentsState = commentsState,
            onDismiss = {
                activeCommentsPost = null
                onCloseComments()
            },
            onSendComment = { text, clearInput ->
                onAddComment(post, text, clearInput)
            },
            onDeleteComment = { commentId ->
                onDeleteComment(post, commentId)
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            isBusy = isBusy,
            onDismiss = { showCreateStory = false },
            onSubmit = { caption, mood, accent, mediaType, dataUrl ->
                onCreateStory(caption, mood, accent, mediaType, dataUrl) {
                    showCreateStory = false
                }
            }
        )
    }

    viewingStoryIndex?.let { index ->
        if (index in stories.indices) {
            StoryViewerDialog(
                stories = stories,
                initialIndex = index,
                currentUserId = currentUserId,
                onDeleteStory = { storyId ->
                    onDeleteStory(storyId) { viewingStoryIndex = null }
                },
                onDismiss = { viewingStoryIndex = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    isBusy: Boolean,
    initialCaption: String,
    initialTag: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (caption: String, tag: String, mediaType: String, photoDataUrl: String?, durationSec: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var caption by remember { mutableStateOf(initialCaption) }
    var selectedTag by remember { mutableStateOf(initialTag) }
    var mediaType by remember { mutableStateOf("text") }
    var photoDataUrl by remember { mutableStateOf<String?>(null) }
    var mediaDurationSec by remember { mutableStateOf(0L) }
    var encodingError by remember { mutableStateOf<String?>(null) }
    var isEncoding by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncoding = true
                encodingError = null
                MediaEncoder.encodeFromUri(context, uri).onSuccess { encoded ->
                    mediaType = encoded.mediaType
                    photoDataUrl = encoded.dataUrl
                    mediaDurationSec = encoded.durationSec
                }.onFailure { err ->
                    encodingError = err.localizedMessage ?: "Failed to attach media."
                }
                isEncoding = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Vybe" else "Drop a Vybe",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 500) caption = it },
                label = { Text("What's your vibe right now?") },
                placeholder = { Text("Share a thought, lyric, or studio update...") },
                minLines = 3,
                maxLines = 6,
                supportingText = { Text("${caption.length}/500") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_caption_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(POST_VIBE_TAGS) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) }
                    )
                }
            }

            if (!isEditing) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        enabled = !isEncoding && !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pick_post_media_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoDataUrl == null) "Add Photo / Video" else "Change Media")
                    }

                    if (photoDataUrl != null) {
                        TextButton(onClick = {
                            photoDataUrl = null
                            mediaType = "text"
                            mediaDurationSec = 0L
                        }) {
                            Text("Remove")
                        }
                    }
                }

                if (isEncoding) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                encodingError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                photoDataUrl?.let { url ->
                    Spacer(modifier = Modifier.height(10.dp))
                    VybeMediaPreview(
                        photoDataUrl = url,
                        mediaType = mediaType,
                        durationSec = mediaDurationSec,
                        contentDescription = "Attached media preview",
                        modifier = Modifier.height(160.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSubmit(caption, selectedTag, mediaType, photoDataUrl, mediaDurationSec) },
                enabled = caption.isNotBlank() && !isBusy && !isEncoding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("publish_post_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEditing) "Save Changes" else "Publish Vybe")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    post: VybePost,
    currentUserId: String,
    commentsState: UiState<List<VybeComment>>,
    onDismiss: () -> Unit,
    onSendComment: (String, () -> Unit) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Comments on @${post.authorHandle}'s Vybe",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (commentsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(commentsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (commentsState.data.isEmpty()) {
                        Text(
                            text = "No comments yet. Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(commentsState.data, key = { it.id }) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        VybeAvatar(
                                            displayName = comment.authorName,
                                            avatarColor = comment.authorAvatarColor,
                                            size = 34.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = comment.authorName,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = formatRelativeTimestamp(comment.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.text,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        if (comment.authorId == currentUserId) {
                                            IconButton(onClick = { onDeleteComment(comment.id) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Delete comment",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 300) commentText = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText) { commentText = "" }
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateStoryDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (caption: String, moodTag: String, accent: String, mediaType: String, mediaDataUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var moodTag by remember { mutableStateOf(MOOD_TAGS.first()) }
    var accent by remember { mutableStateOf(AVATAR_COLOR_TOKENS.first()) }
    var mediaType by remember { mutableStateOf("text") }
    var mediaDataUrl by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaEncoder.encodeFromUri(context, uri).onSuccess { encoded ->
                    mediaType = encoded.mediaType
                    mediaDataUrl = encoded.dataUrl
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Share a 24h Story", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 180) caption = it },
                    label = { Text("Story caption") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MOOD_TAGS) { mood ->
                        FilterChip(
                            selected = moodTag == mood,
                            onClick = { moodTag = mood },
                            label = { Text(mood) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_COLOR_TOKENS.forEach { token ->
                        FilterChip(
                            selected = accent == token,
                            onClick = { accent = token },
                            label = { Text(token.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (mediaDataUrl == null) "Attach Photo / Video (Optional)" else "Media Attached")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(caption, moodTag, accent, mediaType, mediaDataUrl) },
                        enabled = (caption.isNotBlank() || !mediaDataUrl.isNullOrBlank()) && !isBusy
                    ) {
                        Text("Share Story")
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    stories: List<VybeStory>,
    initialIndex: Int,
    currentUserId: String,
    onDeleteStory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val story = stories.getOrNull(currentIndex) ?: return

    LaunchedEffect(currentIndex) {
        delay(5500L)
        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        val bitmap = remember(story.mediaDataUrl) { MediaEncoder.decodeDataUrl(story.mediaDataUrl) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(storyGradientBrush(story.accentGradient))
                .clickable {
                    if (currentIndex < stories.lastIndex) currentIndex += 1 else onDismiss()
                }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Story photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stories.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (idx <= currentIndex) Color.White else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    VybeAvatar(
                        displayName = story.authorName,
                        avatarColor = story.authorAvatarColor,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.authorName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${story.moodTag} • ${formatRelativeTimestamp(story.createdAt)}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (story.authorId == currentUserId) {
                        IconButton(onClick = { onDeleteStory(story.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete story", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (story.caption.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = story.caption,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
