package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.MediaEncoder
import com.example.data.model.AVATAR_COLOR_TOKENS
import com.example.data.model.MOOD_TAGS
import com.example.data.model.VybeAccount
import com.example.data.model.VybePost
import com.example.data.model.VybeProfile
import com.example.ui.components.VybeAvatar
import com.example.ui.components.VybeEmptyState
import com.example.ui.components.VybePostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    myAccount: VybeAccount?,
    myProfile: VybeProfile?,
    myPosts: List<VybePost>,
    likedPostIds: Set<String>,
    isBusy: Boolean,
    onUpdateProfile: (
        displayName: String,
        handle: String,
        bio: String,
        moodTag: String,
        avatarColor: String,
        discoverable: Boolean,
        photoDataUrl: String?,
        onDone: () -> Unit
    ) -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onLikePost: (VybePost) -> Unit,
    onCommentPost: (VybePost) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var showEditSheet by remember { mutableStateOf(false) }

    if (myProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
    ) {
        item(key = "profile_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_default_cover),
                        contentDescription = "Profile cover banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                )
                            )
                    )
                    VybeAvatar(
                        displayName = myProfile.displayName,
                        avatarColor = myProfile.avatarColor,
                        photoUrl = myProfile.photoUrl,
                        photoDataUrl = myProfile.photoDataUrl,
                        size = 86.dp,
                        showRing = true,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp)
                            .offset(y = 26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = myProfile.displayName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "@${myProfile.handle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = myProfile.moodTag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = myProfile.bio.ifBlank { "No bio added yet." },
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (!myAccount?.email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Account: ${myAccount?.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            ProfileStat("Posts", myProfile.postsCount)
                            ProfileStat("Followers", myProfile.followersCount)
                            ProfileStat("Following", myProfile.followingCount)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showEditSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(id = R.string.action_sign_out),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.action_sign_out))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Activity Alerts", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Receive in-app alerts for likes, comments & messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = myAccount?.alertsEnabled ?: true,
                                onCheckedChange = onToggleAlerts
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Your Posts", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (myPosts.isEmpty()) {
            item(key = "no_own_posts") {
                VybeEmptyState(
                    title = "You haven't posted yet",
                    subtitle = "Tap Drop a Vybe from the Feed tab to share your first post."
                )
            }
        } else {
            items(myPosts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    VybePostCard(
                        post = post,
                        isOwner = true,
                        isLiked = post.id in likedPostIds,
                        onAuthorClick = {},
                        onLikeClick = { onLikePost(post) },
                        onCommentClick = { onCommentPost(post) },
                        onMessageAuthorClick = {},
                        onEditClick = {},
                        onDeleteClick = { onDeletePost(post.id) }
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            profile = myProfile,
            isBusy = isBusy,
            onDismiss = { showEditSheet = false },
            onSave = { name, handle, bio, mood, color, discoverable, photoDataUrl ->
                onUpdateProfile(name, handle, bio, mood, color, discoverable, photoDataUrl) {
                    showEditSheet = false
                }
            }
        )
    }
}

@Composable
private fun ProfileStat(label: String, count: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    profile: VybeProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        handle: String,
        bio: String,
        moodTag: String,
        avatarColor: String,
        discoverable: Boolean,
        photoDataUrl: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var displayName by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var bio by remember { mutableStateOf(profile.bio) }
    var moodTag by remember { mutableStateOf(profile.moodTag) }
    var avatarColor by remember { mutableStateOf(profile.avatarColor) }
    var discoverable by remember { mutableStateOf(profile.discoverable) }
    var photoDataUrl by remember { mutableStateOf(profile.photoDataUrl) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaEncoder.encodeFromUri(
                    context = context,
                    uri = uri,
                    maxDimension = 360,
                    maxBase64Chars = 120_000
                ).onSuccess { encoded ->
                    photoDataUrl = encoded.dataUrl
                }
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
                .padding(horizontal = 20.dp)
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                VybeAvatar(
                    displayName = displayName.ifBlank { "V" },
                    avatarColor = avatarColor,
                    photoUrl = profile.photoUrl,
                    photoDataUrl = photoDataUrl,
                    size = 64.dp,
                    showRing = true
                )
                Spacer(modifier = Modifier.width(14.dp))
                OutlinedButton(
                    onClick = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Profile Photo")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 60) displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = { if (it.length <= 32) handle = it },
                label = { Text("Username / Handle") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_handle_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 240) bio = it },
                label = { Text("Bio") },
                maxLines = 3,
                supportingText = { Text("${bio.length}/240") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_bio_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Mood", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MOOD_TAGS) { mood ->
                    FilterChip(
                        selected = moodTag == mood,
                        onClick = { moodTag = mood },
                        label = { Text(mood) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Avatar Glow", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AVATAR_COLOR_TOKENS.forEach { token ->
                    FilterChip(
                        selected = avatarColor == token,
                        onClick = { avatarColor = token },
                        label = { Text(token.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Discoverable in Explore", modifier = Modifier.weight(1f))
                Switch(checked = discoverable, onCheckedChange = { discoverable = it })
            }

            Button(
                onClick = {
                    onSave(displayName, handle, bio, moodTag, avatarColor, discoverable, photoDataUrl)
                },
                enabled = displayName.isNotBlank() && handle.trim().length >= 2 && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_profile_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Profile")
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
