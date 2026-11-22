package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.signOutVybe
import com.example.data.firebase.UiState
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.data.repository.VybeRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommentsSheet
import com.example.ui.screens.CreatorProfileSheet
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.MessagingScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.VybeTheme
import com.example.ui.viewmodel.VybeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class VybeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Feed", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    MESSAGES("Messages", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_messages"),
    ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_alerts"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun VybeApp() {
    var darkTheme by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    VybeTheme(darkTheme = darkTheme) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        } else {
            key(user.uid) {
                VybeAuthenticatedShell(
                    user = user,
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme },
                    onSignedOut = { currentUser = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VybeAuthenticatedShell(
    user: FirebaseUser,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: VybeViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VybeViewModel(VybeRepository(db), user.uid)
            }
        }
    )

    LaunchedEffect(user.uid) {
        viewModel.bootstrapUser(user)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val myAccount by viewModel.myAccount.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val feedPostsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(VybeTab.HOME) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var inspectedCreator by remember { mutableStateOf<VybeProfile?>(null) }
    var externalCommentsPost by remember { mutableStateOf<VybePost?>(null) }

    val profiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val allPosts = (feedPostsState as? UiState.Success)?.data.orEmpty()
    val unreadNotificationsCount = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data?.count { !it.seen } ?: 0
    }

    if (currentTab != VybeTab.HOME) {
        BackHandler { currentTab = VybeTab.HOME }
    }

    val openAuthorProfile: (String) -> Unit = { authorId ->
        if (authorId == user.uid) {
            currentTab = VybeTab.PROFILE
        } else {
            val found = profiles.firstOrNull { it.userId == authorId }
            if (found != null) {
                inspectedCreator = found
            }
        }
    }

    val messageAuthorById: (String) -> Unit = { authorId ->
        val found = profiles.firstOrNull { it.userId == authorId }
        if (found != null && found.userId != user.uid) {
            viewModel.startChatWith(found) {
                inspectedCreator = null
                currentTab = VybeTab.MESSAGES
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (darkTheme) "Switch to light mode" else "Switch to dark mode"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentTab == VybeTab.HOME) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreatePostSheet = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(id = R.string.action_new_post)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("drop_vybe_fab")
                    )
                }
            },
            bottomBar = {
                if (!isExpandedWidth) {
                    NavigationBar {
                        VybeTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (tab == VybeTab.ALERTS && unreadNotificationsCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(
                                                        if (unreadNotificationsCount > 9) "9+"
                                                        else unreadNotificationsCount.toString()
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedWidth) {
                    NavigationRail {
                        Spacer(modifier = Modifier.weight(0.1f))
                        VybeTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (tab == VybeTab.ALERTS && unreadNotificationsCount > 0) {
                                        BadgedBox(badge = { Badge { Text(unreadNotificationsCount.toString()) } }) {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentTab) {
                        VybeTab.HOME -> {
                            FeedScreen(
                                currentUserId = user.uid,
                                myProfile = myProfile,
                                postsState = feedPostsState,
                                stories = stories,
                                likedPostIds = likedPostIds,
                                commentsState = activeCommentsState,
                                isBusy = isBusy,
                                showCreatePostSheet = showCreatePostSheet,
                                onDismissCreatePost = { showCreatePostSheet = false },
                                onOpenCreatePost = { showCreatePostSheet = true },
                                onCreatePost = { caption, tag, mediaType, dataUrl, durationSec ->
                                    viewModel.createPost(caption, tag, mediaType, dataUrl, durationSec) {
                                        showCreatePostSheet = false
                                    }
                                },
                                onEditPost = { postId, caption, tag ->
                                    viewModel.editPost(postId, caption, tag) {}
                                },
                                onDeletePost = { postId -> viewModel.deletePost(postId) },
                                onToggleLike = { post -> viewModel.toggleLike(post) },
                                onOpenComments = { post -> viewModel.selectPostForComments(post.id) },
                                onCloseComments = { viewModel.selectPostForComments(null) },
                                onAddComment = { post, text, clear -> viewModel.addComment(post, text, clear) },
                                onDeleteComment = { post, commentId -> viewModel.deleteComment(post, commentId) },
                                onCreateStory = { caption, mood, accent, mediaType, mediaDataUrl, onDone ->
                                    viewModel.createStory(caption, mood, accent, mediaType, mediaDataUrl, onDone)
                                },
                                onDeleteStory = { storyId, onDone -> viewModel.deleteStory(storyId, onDone) },
                                onAuthorClick = openAuthorProfile,
                                onMessageAuthor = messageAuthorById
                            )
                        }
                        VybeTab.EXPLORE -> {
                            ExploreScreen(
                                currentUserId = user.uid,
                                profilesState = profilesState,
                                postsState = feedPostsState,
                                likedPostIds = likedPostIds,
                                followingIds = followingIds,
                                onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                onProfileClick = { profile -> inspectedCreator = profile },
                                onMessageProfile = { peer ->
                                    viewModel.startChatWith(peer) { currentTab = VybeTab.MESSAGES }
                                },
                                onLikePost = { post -> viewModel.toggleLike(post) },
                                onCommentPost = { post ->
                                    externalCommentsPost = post
                                    viewModel.selectPostForComments(post.id)
                                },
                                onMessageAuthor = messageAuthorById
                            )
                        }
                        VybeTab.MESSAGES -> {
                            MessagingScreen(
                                currentUserId = user.uid,
                                isExpandedWidth = isExpandedWidth,
                                conversationsState = conversationsState,
                                profiles = profiles,
                                selectedConversationId = selectedConversationId,
                                messagesState = activeMessagesState,
                                onSelectConversation = { viewModel.selectConversation(it) },
                                onStartChatWith = { peer -> viewModel.startChatWith(peer) {} },
                                onSendMessage = { conv, text, onSent -> viewModel.sendMessage(conv, text, onSent) }
                            )
                        }
                        VybeTab.ALERTS -> {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onMarkSeen = { viewModel.markNotificationSeen(it) },
                                onMarkAllSeen = { viewModel.markAllNotificationsSeen() },
                                onClear = { viewModel.clearNotification(it) },
                                onOpenNotificationTarget = { notif ->
                                    when (notif.type) {
                                        "message" -> {
                                            notif.targetId?.let { viewModel.selectConversation(it) }
                                            currentTab = VybeTab.MESSAGES
                                        }
                                        "follow" -> {
                                            profiles.firstOrNull { it.userId == notif.actorId }?.let {
                                                inspectedCreator = it
                                            }
                                        }
                                        else -> currentTab = VybeTab.HOME
                                    }
                                }
                            )
                        }
                        VybeTab.PROFILE -> {
                            val myPosts = remember(allPosts, user.uid) {
                                allPosts.filter { it.authorId == user.uid }
                            }
                            ProfileScreen(
                                myAccount = myAccount,
                                myProfile = myProfile,
                                myPosts = myPosts,
                                likedPostIds = likedPostIds,
                                isBusy = isBusy,
                                onUpdateProfile = { name, handle, bio, mood, color, discoverable, photoDataUrl, onDone ->
                                    viewModel.updateProfile(
                                        displayName = name,
                                        handle = handle,
                                        bio = bio,
                                        moodTag = mood,
                                        avatarColor = color,
                                        discoverable = discoverable,
                                        photoUrl = myProfile?.photoUrl,
                                        photoDataUrl = photoDataUrl,
                                        onDone = onDone
                                    )
                                },
                                onToggleAlerts = { enabled -> viewModel.updateAlertsEnabled(enabled) },
                                onSignOut = {
                                    signOutVybe(credentialManager, scope) {
                                        onSignedOut()
                                    }
                                },
                                onLikePost = { post -> viewModel.toggleLike(post) },
                                onCommentPost = { post ->
                                    externalCommentsPost = post
                                    viewModel.selectPostForComments(post.id)
                                },
                                onDeletePost = { postId -> viewModel.deletePost(postId) }
                            )
                        }
                    }
                }
            }
        }
    }

    inspectedCreator?.let { creator ->
        val creatorPosts = remember(allPosts, creator.userId) {
            allPosts.filter { it.authorId == creator.userId }
        }
        val liveCreator = profiles.firstOrNull { it.userId == creator.userId } ?: creator
        CreatorProfileSheet(
            creator = liveCreator,
            posts = creatorPosts,
            likedPostIds = likedPostIds,
            isFollowing = liveCreator.userId in followingIds,
            isSelf = liveCreator.userId == user.uid,
            onDismiss = { inspectedCreator = null },
            onToggleFollow = { viewModel.toggleFollow(liveCreator) },
            onMessageClick = {
                viewModel.startChatWith(liveCreator) {
                    inspectedCreator = null
                    currentTab = VybeTab.MESSAGES
                }
            },
            onLikePost = { post -> viewModel.toggleLike(post) },
            onCommentPost = { post ->
                inspectedCreator = null
                externalCommentsPost = post
                viewModel.selectPostForComments(post.id)
            }
        )
    }

    externalCommentsPost?.let { post ->
        CommentsSheet(
            post = post,
            currentUserId = user.uid,
            commentsState = activeCommentsState,
            onDismiss = {
                externalCommentsPost = null
                viewModel.selectPostForComments(null)
            },
            onSendComment = { text, clear -> viewModel.addComment(post, text, clear) },
            onDeleteComment = { commentId -> viewModel.deleteComment(post, commentId) }
        )
    }
}
