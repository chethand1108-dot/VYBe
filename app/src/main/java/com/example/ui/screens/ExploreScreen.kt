package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.ui.components.VybeAvatar
import com.example.ui.components.VybeEmptyState
import com.example.ui.components.VybePostCard

@Composable
fun ExploreScreen(
    currentUserId: String,
    profilesState: UiState<List<VybeProfile>>,
    postsState: UiState<List<VybePost>>,
    likedPostIds: Set<String>,
    followingIds: Set<String>,
    onToggleFollow: (VybeProfile) -> Unit,
    onProfileClick: (VybeProfile) -> Unit,
    onMessageProfile: (VybeProfile) -> Unit,
    onLikePost: (VybePost) -> Unit,
    onCommentPost: (VybePost) -> Unit,
    onMessageAuthor: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search creators, @handles, or vibes...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("explore_search_input")
        )

        val profiles = (profilesState as? UiState.Success)?.data.orEmpty()
        val posts = (postsState as? UiState.Success)?.data.orEmpty()

        val filteredProfiles = remember(profiles, query, currentUserId) {
            val q = query.trim().lowercase()
            profiles.filter { it.userId != currentUserId }
                .filter { p ->
                    q.isEmpty() ||
                        p.displayName.lowercase().contains(q) ||
                        p.handle.lowercase().contains(q) ||
                        p.moodTag.lowercase().contains(q)
                }
        }

        val filteredPosts = remember(posts, query) {
            val q = query.trim().lowercase()
            posts.filter { post ->
                q.isEmpty() ||
                    post.caption.lowercase().contains(q) ||
                    post.vibeTag.lowercase().contains(q) ||
                    post.authorName.lowercase().contains(q) ||
                    post.authorHandle.lowercase().contains(q)
            }.sortedByDescending { it.likesCount + it.commentsCount }
        }

        if (profilesState is UiState.Loading && postsState is UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "creators_header") {
                    Text(
                        text = "Creators to Follow",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (filteredProfiles.isEmpty()) {
                    item(key = "no_creators") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (query.isBlank()) "More creators will appear here as they join VYBE."
                                else "No creators match \"$query\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    item(key = "creators_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProfiles, key = { it.userId }) { creator ->
                                val isFollowing = creator.userId in followingIds
                                Card(
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    modifier = Modifier
                                        .width(196.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable { onProfileClick(creator) }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        VybeAvatar(
                                            displayName = creator.displayName,
                                            avatarColor = creator.avatarColor,
                                            photoUrl = creator.photoUrl,
                                            photoDataUrl = creator.photoDataUrl,
                                            size = 60.dp,
                                            showRing = true
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = creator.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${creator.handle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "${creator.followersCount} followers • ${creator.moodTag}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isFollowing) {
                                                OutlinedButton(
                                                    onClick = { onToggleFollow(creator) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .minimumInteractiveComponentSize()
                                                ) {
                                                    Text("Following", maxLines = 1)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { onToggleFollow(creator) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .minimumInteractiveComponentSize()
                                                ) {
                                                    Text("Follow", maxLines = 1)
                                                }
                                            }
                                            IconButton(
                                                onClick = { onMessageProfile(creator) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                                    contentDescription = "Message ${creator.displayName}",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "trending_header") {
                    Text(
                        text = "Trending Vybes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (filteredPosts.isEmpty()) {
                    item(key = "no_explore_posts") {
                        VybeEmptyState(
                            title = "Nothing discovered yet",
                            subtitle = "Try another keyword or share a new Vybe on the Feed."
                        )
                    }
                } else {
                    items(filteredPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            VybePostCard(
                                post = post,
                                isOwner = post.authorId == currentUserId,
                                isLiked = post.id in likedPostIds,
                                onAuthorClick = {
                                    profiles.firstOrNull { it.userId == post.authorId }?.let(onProfileClick)
                                },
                                onLikeClick = { onLikePost(post) },
                                onCommentClick = { onCommentPost(post) },
                                onMessageAuthorClick = { onMessageAuthor(post.authorId) },
                                onEditClick = {},
                                onDeleteClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
