package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.ui.components.VybeAvatar
import com.example.ui.components.VybePostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileSheet(
    creator: VybeProfile,
    posts: List<VybePost>,
    likedPostIds: Set<String>,
    isFollowing: Boolean,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessageClick: () -> Unit,
    onLikePost: (VybePost) -> Unit,
    onCommentPost: (VybePost) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VybeAvatar(
                    displayName = creator.displayName,
                    avatarColor = creator.avatarColor,
                    photoUrl = creator.photoUrl,
                    photoDataUrl = creator.photoDataUrl,
                    size = 64.dp,
                    showRing = true
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(creator.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "@${creator.handle} • ${creator.moodTag}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = creator.bio.ifBlank { "Creator on VYBE." },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CreatorStat("Posts", creator.postsCount)
                    CreatorStat("Followers", creator.followersCount)
                    CreatorStat("Following", creator.followingCount)
                }
            }

            if (!isSelf) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onToggleFollow,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Following")
                        }
                    } else {
                        Button(
                            onClick = onToggleFollow,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Follow")
                        }
                    }
                    OutlinedButton(
                        onClick = onMessageClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Vybes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (posts.isEmpty()) {
                Text(
                    text = "No posts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        VybePostCard(
                            post = post,
                            isOwner = isSelf,
                            isLiked = post.id in likedPostIds,
                            onAuthorClick = {},
                            onLikeClick = { onLikePost(post) },
                            onCommentClick = { onCommentPost(post) },
                            onMessageAuthorClick = onMessageClick,
                            onEditClick = {},
                            onDeleteClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorStat(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
