package com.example.myapplication.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemQueueBinding
import java.util.Collections
import com.example.myapplication.data.PlaybackService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.audiofx.Visualizer
import androidx.media3.session.MediaController as Media3Controller
import coil.compose.AsyncImage
import coil.imageLoader
import coil.transform.RoundedCornersTransformation
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter

enum class MediaType { AUDIO, VIDEO }
enum class SortOrder { NAME, DATE, SIZE, ARTIST }

data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val type: MediaType,
    val dateAdded: Long,
    val size: Long,
    val albumArtUri: Uri? = null
)

@kotlin.OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerView(currentLanguage: String, username: String, onMediaPlayed: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val permissions = mutableListOf<String>()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    permissions.add(android.Manifest.permission.RECORD_AUDIO)
    
    val permissionState = rememberMultiplePermissionsState(permissions)

    val permissionText = if (currentLanguage == "FR") "L'accès aux fichiers multimédias et aux notifications est nécessaire." else "Access to media files and notifications is required."
    val buttonText = if (currentLanguage == "FR") "Autoriser l'accès" else "Allow access"

    if (permissionState.allPermissionsGranted) {
        MediaControllerWrapper(context, currentLanguage, username, onMediaPlayed)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(permissionText)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun MediaControllerWrapper(context: Context, currentLanguage: String, username: String, onMediaPlayed: (MediaItem) -> Unit) {
    var controller by remember { mutableStateOf<MediaController?>(null) }
    
    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())
        
        onDispose {
            MediaController.releaseFuture(controllerFuture)
            controller = null
        }
    }

    controller?.let {
        val audioSessionId = it.sessionExtras.getInt("AUDIO_SESSION_ID", 0)
        MediaTabs(context, currentLanguage, it, audioSessionId, onMediaPlayed)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MediaTabs(context: Context, currentLanguage: String, player: Player, audioSessionId: Int, onMediaPlayed: (MediaItem) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
    var isAscending by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = if (currentLanguage == "FR") listOf("Musique", "Vidéo") else listOf("Music", "Video")
    val searchHint = if (currentLanguage == "FR") "Rechercher musique, artiste ou album..." else "Search music, artist or album..."
    
    val mediaItems = remember { getLocalMedia(context) }
    
    val filteredItems = remember(selectedTab, sortOrder, isAscending, mediaItems, searchQuery) {
        val baseItems = if (selectedTab == 0) {
            mediaItems.filter { it.type == MediaType.AUDIO }
        } else {
            mediaItems.filter { it.type == MediaType.VIDEO }
        }

        val items = if (searchQuery.isBlank()) {
            baseItems
        } else {
            baseItems.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
        
        val sorted = when (sortOrder) {
            SortOrder.NAME -> items.sortedBy { it.title.lowercase() }
            SortOrder.DATE -> items.sortedBy { it.dateAdded }
            SortOrder.SIZE -> items.sortedBy { it.size }
            SortOrder.ARTIST -> items.sortedBy { it.artist.lowercase() }
        }
        
        if (isAscending) sorted else sorted.reversed()
    }

    val emptyText = if (currentLanguage == "FR") "Aucun fichier trouvé." else "No files found."

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text(searchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.weight(1f)) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = if (showSortMenu) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    val sortOptions = if (currentLanguage == "FR") {
                        listOf("Nom", "Date", "Taille", "Artiste")
                    } else {
                        listOf("Name", "Date", "Size", "Artist")
                    }
                    sortOptions.forEachIndexed { index, name ->
                        val option = SortOrder.entries[index]
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name)
                                    if (sortOrder == option) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (sortOrder == option) {
                                    isAscending = !isAscending
                                } else {
                                    sortOrder = option
                                    isAscending = true
                                }
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }
        
        key(selectedTab) {
            MediaList(filteredItems, if (selectedTab == 0) MediaType.AUDIO else MediaType.VIDEO, emptyText, player, audioSessionId, currentLanguage, sortOrder, onMediaPlayed)
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaList(items: List<MediaItem>, type: MediaType, emptyText: String, player: Player, audioSessionId: Int, currentLanguage: String, sortOrder: SortOrder, onMediaPlayed: (MediaItem) -> Unit) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isVideoHidden by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var shuffleMode by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var showNowPlaying by remember { mutableStateOf(false) }
    
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
                val currentUri = player.currentMediaItem?.localConfiguration?.uri
                if (currentUri != null) {
                    val idx = items.indexOfFirst { it.uri == currentUri }
                    if (idx != -1) {
                        currentIndex = idx
                        onMediaPlayed(items[idx])
                    }
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleMode = shuffleModeEnabled
            }
            override fun onRepeatModeChanged(newRepeatMode: Int) {
                repeatMode = newRepeatMode
            }
        }
        player.addListener(listener)
        
        isPlaying = player.isPlaying
        val currentUri = player.currentMediaItem?.localConfiguration?.uri
        if (currentUri != null) {
            val idx = items.indexOfFirst { it.uri == currentUri }
            if (idx != -1) currentIndex = idx
        }
        playbackSpeed = player.playbackParameters.speed
        volume = player.volume
        shuffleMode = player.shuffleModeEnabled
        repeatMode = player.repeatMode
        duration = player.duration.coerceAtLeast(0L)

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, currentIndex) {
        while (isPlaying) {
            currentPos = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    val visualizerData = remember { mutableStateOf(FloatArray(40) { 0f }) }

    DisposableEffect(audioSessionId, isPlaying) {
        if (audioSessionId <= 0 || !isPlaying) {
            visualizerData.value = FloatArray(40) { 0f }
            return@DisposableEffect onDispose {}
        }
        
        val visualizer = try {
            android.media.audiofx.Visualizer(audioSessionId).apply {
                captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: android.media.audiofx.Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: android.media.audiofx.Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) {
                            val newData = FloatArray(40)
                            for (i in 0 until 40) {
                                val rf = fft[i * 2].toInt()
                                val ifrag = fft[i * 2 + 1].toInt()
                                val mag = Math.sqrt((rf * rf + ifrag * ifrag).toDouble()).toFloat()
                                newData[i] = mag / 30f // Scaling for UI
                            }
                            visualizerData.value = newData
                        }
                    }
                }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 10, false, true) // Reduced frequency
                enabled = true
            }
        } catch (e: Exception) { null }

        onDispose {
            visualizer?.enabled = false
            try { visualizer?.release() } catch (e: Exception) {}
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isDraggingScrollbar by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (type == MediaType.VIDEO && currentIndex != -1 && currentIndex < items.size) {
            VideoPlayerSection(
                player = player,
                isHidden = isVideoHidden,
                onToggleHidden = { isVideoHidden = !isVideoHidden },
                onToggleFullScreen = { isFullScreen = true }
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(emptyText)
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    itemsIndexed(items) { index, item ->
                        var showItemMenu by remember { mutableStateOf(false) }
                        
                        ListItem(
                            modifier = Modifier.clickable {
                                if (currentIndex == index && type == (if (player.currentMediaItem?.localConfiguration?.uri == item.uri) type else null)) {
                                    if (player.isPlaying) player.pause() else player.play()
                                } else {
                                    val media3Items = items.map { createMedia3Item(it) }
                                    player.setMediaItems(media3Items, index, 0L)
                                    player.prepare()
                                    player.play()
                                    currentIndex = index
                                    onMediaPlayed(item)
                                }
                            },
                            headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { 
                                Text(
                                    text = "${item.artist} • ${item.album}", 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            leadingContent = {
                                if (type == MediaType.AUDIO && item.albumArtUri != null) {
                                    AsyncImage(
                                        model = item.albumArtUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Default.MusicNote)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (type == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (currentIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentIndex == index && isPlaying) {
                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Box {
                                        IconButton(onClick = { showItemMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                        }
                                        DropdownMenu(expanded = showItemMenu, onDismissRequest = { showItemMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text(if (currentLanguage == "FR") "Lire ensuite" else "Play Next") },
                                                onClick = {
                                                    val nextIndex = if (player.mediaItemCount > 0) player.currentMediaItemIndex + 1 else 0
                                                    player.addMediaItem(nextIndex, createMedia3Item(item))
                                                    showItemMenu = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
                
                // Interactive Scrollbar with Indicator
                val totalItems = items.size
                if (totalItems > 0) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(48.dp) // Wider area for easier touch
                    ) {
                        val containerHeight = maxHeight
                        val firstVisible = listState.firstVisibleItemIndex
                        val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        
                        if (visibleItemsCount < totalItems) {
                            val scrollbarHeightFraction = (visibleItemsCount.toFloat() / totalItems).coerceIn(0.1f, 1f)
                            val scrollbarOffsetFraction = (firstVisible.toFloat() / totalItems)
                            
                            val thumbHeight = containerHeight * (visibleItemsCount.toFloat() / totalItems).coerceIn(0.1f, 1f)
                            val thumbOffset = containerHeight * (firstVisible.toFloat() / totalItems)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(items) {
                                        detectDragGestures(
                                            onDragStart = { isDraggingScrollbar = true },
                                            onDragEnd = { isDraggingScrollbar = false },
                                            onDragCancel = { isDraggingScrollbar = false },
                                            onDrag = { change, _ ->
                                                val y = change.position.y
                                                val totalHeightPx = size.height.toFloat()
                                                val percentage = (y / totalHeightPx).coerceIn(0f, 1f)
                                                val targetIndex = (percentage * totalItems).toInt()
                                                scope.launch {
                                                    listState.scrollToItem(targetIndex.coerceIn(0, totalItems - 1))
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // Background track
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(Color.Gray.copy(alpha = 0.1f), MaterialTheme.shapes.extraLarge)
                                )

                                // Visual thumb
                                Box(
                                    modifier = Modifier
                                        .offset(y = thumbOffset)
                                        .align(Alignment.TopEnd)
                                        .width(6.dp)
                                        .height(thumbHeight)
                                        .background(
                                            if (isDraggingScrollbar) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                                            MaterialTheme.shapes.extraLarge
                                        )
                                )

                                // A-Z or Date Indicator Popup
                                if (isDraggingScrollbar && firstVisible < items.size) {
                                    val currentItem = items[firstVisible]
                                    val indicatorText = when (sortOrder) {
                                        SortOrder.NAME -> currentItem.title.take(1).uppercase()
                                        SortOrder.ARTIST -> currentItem.artist.take(1).uppercase()
                                        SortOrder.DATE -> formatDateShort(currentItem.dateAdded)
                                        SortOrder.SIZE -> formatSizeShort(currentItem.size)
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-40).dp, y = thumbOffset.coerceIn(0.dp, containerHeight - 48.dp)),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        tonalElevation = 8.dp
                                    ) {
                                        Text(
                                            text = indicatorText,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentIndex != -1 && currentIndex < items.size) {
            AdvancedBottomPlayer(
                item = items[currentIndex],
                isPlaying = isPlaying,
                currentPos = currentPos,
                duration = duration,
                playbackSpeed = playbackSpeed,
                volume = volume,
                visualizerData = visualizerData.value,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
                onNext = { player.seekToNext() },
                onPrev = { player.seekToPrevious() },
                onSpeedChange = { 
                    playbackSpeed = it
                    player.setPlaybackSpeed(it)
                },
                onVolumeChange = {
                    volume = it
                    player.volume = it
                },
                onSeek = { player.seekTo(player.currentPosition + it) },
                onCycleMode = {
                    val (nextShuffle, nextRepeat, msg) = when {
                        !shuffleMode && repeatMode == Player.REPEAT_MODE_OFF -> {
                            Triple(true, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Aléatoire" else "Shuffle Mode")
                        }
                        shuffleMode -> {
                            Triple(false, Player.REPEAT_MODE_ALL, if (currentLanguage == "FR") "Tout répéter" else "Loop All")
                        }
                        repeatMode == Player.REPEAT_MODE_ALL -> {
                            Triple(false, Player.REPEAT_MODE_ONE, if (currentLanguage == "FR") "Répéter un titre" else "Repeat One")
                        }
                        else -> {
                            Triple(false, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Normal" else "Normal Mode")
                        }
                    }
                    
                    if (nextShuffle && !shuffleMode) {
                        shuffleQueuePhysical(player)
                    }

                    player.shuffleModeEnabled = nextShuffle
                    player.repeatMode = nextRepeat
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onClick = { showNowPlaying = true }
            )
        }
    }

    if (showNowPlaying && currentIndex != -1 && currentIndex < items.size) {
        NowPlayingDialog(
            item = items[currentIndex],
            player = player,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            volume = volume,
            visualizerData = visualizerData.value,
            onVolumeChange = {
                volume = it
                player.volume = it
            },
            shuffleMode = shuffleMode,
            repeatMode = repeatMode,
            currentLanguage = currentLanguage,
            onDismiss = { showNowPlaying = false }
        )
    }

    if (isFullScreen && currentIndex != -1 && currentIndex < items.size) {
        VideoFullScreenDialog(
            player = player,
            onDismiss = { isFullScreen = false }
        )
    }
}

fun formatDateShort(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM yy", java.util.Locale.getDefault())
    return format.format(date)
}

fun formatSizeShort(size: Long): String {
    return if (size < 1024 * 1024) "${size / 1024} KB" else "${size / (1024 * 1024)} MB"
}

fun createMedia3Item(item: MediaItem): Media3Item {
    return Media3Item.Builder()
        .setUri(item.uri)
        .setMediaId("${item.id}_${System.nanoTime()}") // Unique ID for each entry in the queue
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.album)
                .setArtworkUri(item.albumArtUri) // Add artwork URI here
                .build()
        )
        .build()
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSection(player: Player, isHidden: Boolean, onToggleHidden: () -> Unit, onToggleFullScreen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isHidden) 80.dp else 240.dp)
            .background(Color.Black)
    ) {
        if (!isHidden) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicVideo, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Audio Only Mode", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            IconButton(onClick = onToggleHidden) {
                Icon(
                    imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Video",
                    tint = Color.White
                )
            }
            if (!isHidden) {
                IconButton(onClick = { onToggleFullScreen() }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Plein écran", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoFullScreenDialog(player: Player, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Quitter plein écran", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingDialog(
    item: MediaItem,
    player: Player,
    isPlaying: Boolean,
    playbackSpeed: Float,
    volume: Float,
    visualizerData: FloatArray,
    onVolumeChange: (Float) -> Unit,
    shuffleMode: Boolean,
    repeatMode: Int,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPos by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var showQueue by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPos = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = if (currentLanguage == "FR") "À l'écoute" else "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showQueue = !showQueue }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic, 
                            contentDescription = "Queue",
                            tint = if (showQueue) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }

                if (showQueue) {
                    Spacer(modifier = Modifier.height(16.dp))
                    QueueView(player, currentLanguage)
                } else {
                    Spacer(modifier = Modifier.height(48.dp))

                    // Large Artwork Placeholder with Circular Visualizer
                    Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
                        // Rotation animation for the circular icon and visualizer
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(15000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        Surface(
                            modifier = Modifier
                                .size(240.dp)
                                .graphicsLayer {
                                    rotationZ = if (isPlaying) rotation else 0f
                                },
                            shape = CircleShape, // Perfectly round
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (item.type == MediaType.AUDIO && item.albumArtUri != null) {
                                    AsyncImage(
                                        model = item.albumArtUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Default.MusicNote)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (item.type == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Move visualizer after surface to be in front
                        CircularVisualizer(
                            isPlaying = isPlaying,
                            visualizerData = visualizerData,
                            rotation = if (isPlaying) rotation else 0f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.artist} • ${item.album}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Visualiseur de Basses style "Papillon" (Bowtie) centré
                        BassVisualizer(
                            isPlaying = isPlaying, 
                            volume = volume,
                            visualizerData = visualizerData,
                            modifier = Modifier
                                .fillMaxWidth(0.6f) // 60% width
                                .height(40.dp)
                                .padding(bottom = 8.dp)
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth() // Back to full width for seek bar
                                .height(24.dp)
                                .pointerInput(duration) {
                                    if (duration > 0) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            val width = size.width.toFloat()
                                            val newValue = (change.position.x / width).coerceIn(0f, 1f)
                                            val seekTo = (newValue * duration).toLong()
                                            player.seekTo(seekTo)
                                            currentPos = seekTo
                                        }
                                    }
                                }
                                .pointerInput(duration) {
                                    if (duration > 0) {
                                        detectTapGestures { offset ->
                                            val width = size.width.toFloat()
                                            val newValue = (offset.x / width).coerceIn(0f, 1f)
                                            val seekTo = (newValue * duration).toLong()
                                            player.seekTo(seekTo)
                                            currentPos = seekTo
                                        }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val progress = if (duration > 0) currentPos.toFloat() / duration else 0f
                            
                            // Background track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                            )
                            // Active progress level
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            // Thumb indicator
                            Box(
                                modifier = Modifier
                                    .offset(x = (maxWidth * progress) - 6.dp)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatTime(currentPos), style = MaterialTheme.typography.labelSmall)
                            Text(text = formatTime(duration), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val (nextShuffle, nextRepeat, msg) = when {
                                !shuffleMode && repeatMode == Player.REPEAT_MODE_OFF -> {
                                    Triple(true, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Aléatoire" else "Shuffle Mode")
                                }
                                shuffleMode -> {
                                    Triple(false, Player.REPEAT_MODE_ALL, if (currentLanguage == "FR") "Tout répéter" else "Loop All")
                                }
                                repeatMode == Player.REPEAT_MODE_ALL -> {
                                    Triple(false, Player.REPEAT_MODE_ONE, if (currentLanguage == "FR") "Répéter un titre" else "Repeat One")
                                }
                                else -> {
                                    Triple(false, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Normal" else "Normal Mode")
                                }
                            }
                            
                            if (nextShuffle && !shuffleMode) {
                                shuffleQueuePhysical(player)
                            }

                            player.shuffleModeEnabled = nextShuffle
                            player.repeatMode = nextRepeat
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }) {
                            val icon = when {
                                shuffleMode -> Icons.Default.Shuffle
                                repeatMode == Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            }
                            Icon(
                                imageVector = icon, 
                                contentDescription = "Mode",
                                tint = if (shuffleMode || repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        
                        IconButton(onClick = { player.seekToPrevious() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(48.dp))
                        }

                        FloatingActionButton(
                            onClick = { if (player.isPlaying) player.pause() else player.play() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(onClick = { player.seekToNext() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                        }

                        Box(contentAlignment = Alignment.BottomCenter) {
                            IconButton(onClick = { showVolumeControl = !showVolumeControl }) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = if (showVolumeControl) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            
                            if (showVolumeControl) {
                                Popup(
                                    alignment = Alignment.TopCenter,
                                    offset = IntOffset(0, -50),
                                    onDismissRequest = { showVolumeControl = false }
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .height(200.dp)
                                            .width(32.dp),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                        tonalElevation = 6.dp
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "${(volume * 100).toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .pointerInput(Unit) {
                                                        detectDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val height = size.height.toFloat()
                                                            val newValue = (1f - (change.position.y / height)).coerceIn(0f, 1f)
                                                            onVolumeChange(newValue)
                                                        }
                                                    },
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                // Background track
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(4.dp)
                                                        .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                                                )
                                                // Active volume level
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight(volume)
                                                        .width(4.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                                // Thumb indicator
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .offset(y = (- (volume * 140)).dp) // Approximation for thumb pos
                                                        .size(12.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

class QueueAdapter(
    private var items: MutableList<Media3Item>,
    private val player: Player,
    private val onOrderReleased: () -> Unit
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    var isDragging: Boolean = false

    inner class QueueViewHolder(val binding: ItemQueueBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val mediaItem = items[position]
        val isCurrent = position == player.currentMediaItemIndex
        
        holder.binding.apply {
            textNumber.text = (position + 1).toString()
            textTitle.text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
            textArtist.text = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
            
            // Load Artwork
            val artworkUri = mediaItem.mediaMetadata.artworkUri
            if (artworkUri != null) {
                imageArtwork.visibility = View.VISIBLE
                // Use Coil to load image into XML ImageView
                imageArtwork.context.imageLoader.enqueue(
                    coil.request.ImageRequest.Builder(imageArtwork.context)
                        .data(artworkUri)
                        .target(imageArtwork)
                        .crossfade(true)
                        .transformations(RoundedCornersTransformation(16f)) // Match radius
                        .error(android.R.drawable.ic_menu_report_image)
                        .placeholder(android.R.drawable.ic_menu_report_image)
                        .build()
                )
            } else {
                imageArtwork.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            // Design Amélioré
            if (isCurrent) {
                textTitle.setTextColor(android.graphics.Color.parseColor("#64B5F6")) // Dark Blue Accent
                textTitle.setTypeface(null, android.graphics.Typeface.BOLD)
                textNumber.setTextColor(android.graphics.Color.parseColor("#64B5F6"))
                cardRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#1A64B5F6")) // Blue highlight
            } else {
                textTitle.setTextColor(android.graphics.Color.WHITE)
                textTitle.setTypeface(null, android.graphics.Typeface.NORMAL)
                textNumber.setTextColor(android.graphics.Color.GRAY)
                cardRoot.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            }

            root.setOnClickListener {
                player.seekTo(holder.bindingAdapterPosition, 0L)
                player.play()
            }

            btnRemove.setOnClickListener {
                player.removeMediaItem(holder.bindingAdapterPosition)
            }
            
            btnRemove.visibility = if (isCurrent) View.GONE else View.VISIBLE
            handleDrag.visibility = if (isCurrent) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Media3Item>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun moveItem(fromPos: Int, toPos: Int) {
        if (fromPos == toPos) return
        Collections.swap(items, fromPos, toPos)
        player.moveMediaItem(fromPos, toPos)
        notifyItemMoved(fromPos, toPos)
    }
    
    fun onDragReleased() {
        // Refresh positions (numbers 1, 2, 3...)
        notifyDataSetChanged()
        onOrderReleased()
    }
}

class QueueTouchCallback(private val adapter: QueueAdapter) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.moveItem(vh.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }
    
    override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}

    // AUGMENTATION DE LA VITESSE DE DÉFILEMENT (Pour passer de 1 à 700 rapidement)
    override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long
    ): Int {
        val standardSpeed = super.interpolateOutOfBoundsScroll(
            recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll
        )
        // Multiplier la vitesse par 4 pour une réactivité extrême sur les longues listes
        val multiplier = if (msSinceStartScroll > 2000) 6 else 3 // Accélération progressive
        return standardSpeed * multiplier
    }
    
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.isDragging = false
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.scaleX = 1.0f
        viewHolder.itemView.scaleY = 1.0f
        if (viewHolder is QueueAdapter.QueueViewHolder) {
            viewHolder.binding.cardRoot.cardElevation = 0f
        }
        adapter.onDragReleased()
    }
    
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            adapter.isDragging = true
            viewHolder?.itemView?.alpha = 0.9f
            viewHolder?.itemView?.scaleX = 1.05f // Soulèvement plus prononcé
            viewHolder?.itemView?.scaleY = 1.05f
            if (viewHolder is QueueAdapter.QueueViewHolder) {
                viewHolder.binding.cardRoot.cardElevation = 12f // Ombre plus forte
            }
        }
    }
}
@Composable
fun QueueView(player: Player, currentLanguage: String) {
    val context = LocalContext.current
    val queueItems = remember { mutableStateListOf<Media3Item>() }
    
    fun getQueueList(): List<Media3Item> {
        val list = mutableListOf<Media3Item>()
        for (i in 0 until player.mediaItemCount) {
            list.add(player.getMediaItemAt(i))
        }
        return list
    }

    val adapter = remember { 
        QueueAdapter(getQueueList().toMutableList(), player) {
            // Callback when drag is released
            Toast.makeText(context, if (currentLanguage == "FR") "Ordre mis à jour" else "Order updated", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (!adapter.isDragging) {
                    adapter.updateItems(getQueueList())
                }
            }
            override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
                if (!adapter.isDragging) {
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (!adapter.isDragging) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentLanguage == "FR") "File d'attente" else "Up Next",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Playback Mode Button in Queue
            IconButton(onClick = {
                val shuffleMode = player.shuffleModeEnabled
                val repeatMode = player.repeatMode
                
                val (nextShuffle, nextRepeat, msg) = when {
                    !shuffleMode && repeatMode == Player.REPEAT_MODE_OFF -> {
                        Triple(true, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Aléatoire" else "Shuffle Mode")
                    }
                    shuffleMode -> {
                        Triple(false, Player.REPEAT_MODE_ALL, if (currentLanguage == "FR") "Tout répéter" else "Loop All")
                    }
                    repeatMode == Player.REPEAT_MODE_ALL -> {
                        Triple(false, Player.REPEAT_MODE_ONE, if (currentLanguage == "FR") "Répéter un titre" else "Repeat One")
                    }
                    else -> {
                        Triple(false, Player.REPEAT_MODE_OFF, if (currentLanguage == "FR") "Mode Normal" else "Normal Mode")
                    }
                }
                
                if (nextShuffle && !shuffleMode) {
                    shuffleQueuePhysical(player)
                }

                player.shuffleModeEnabled = nextShuffle
                player.repeatMode = nextRepeat
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }) {
                val shuffleMode = player.shuffleModeEnabled
                val repeatMode = player.repeatMode
                val icon = when {
                    shuffleMode -> Icons.Default.Shuffle
                    repeatMode == Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                    repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon, 
                    contentDescription = "Mode",
                    tint = if (shuffleMode || repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
        
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                RecyclerView(ctx).apply {
                    layoutManager = LinearLayoutManager(ctx)
                    this.adapter = adapter
                    val touchHelper = ItemTouchHelper(QueueTouchCallback(adapter))
                    touchHelper.attachToRecyclerView(this)
                }
            },
            update = { rv ->
                // Basic updates if needed
            }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun CircularVisualizer(isPlaying: Boolean, visualizerData: FloatArray, rotation: Float = 0f, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.70f
        val barCount = 80 // Doubled number of bars for a denser look
        val angleStep = 360f / barCount

        for (i in 0 until barCount) {
            // Interpolate data for more bars
            val dataIndex = (i * visualizerData.size / barCount)
            val amplitude = if (isPlaying) visualizerData[dataIndex] else 0f
            
            // RGB Hue based on position and rotation for a cycling effect
            val hue = (i * angleStep + rotation) % 360f
            val barColor = Color.hsv(hue, 0.8f, 1f)
            
            // INCREASED MAX HEIGHT
            val barHeight = (amplitude * 120f).coerceIn(4f, 160f)
            val angle = i * angleStep - 90f + rotation
            val angleRad = Math.toRadians(angle.toDouble()).toFloat()
            
            val startX = center.x + Math.cos(angleRad.toDouble()).toFloat() * radius
            val startY = center.y + Math.sin(angleRad.toDouble()).toFloat() * radius
            
            val endX = center.x + Math.cos(angleRad.toDouble()).toFloat() * (radius + barHeight)
            val endY = center.y + Math.sin(angleRad.toDouble()).toFloat() * (radius + barHeight)
            
            drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // INCREASED GLOW INTENSITY
            if (amplitude > 1.0f) {
                drawCircle(
                    color = barColor.copy(alpha = 0.05f * amplitude),
                    radius = radius + barHeight + 10f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun BassVisualizer(isPlaying: Boolean, volume: Float, visualizerData: FloatArray, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bass")
    val barCount = 40
    
    val colorBass = MaterialTheme.colorScheme.primary
    val colorMid = MaterialTheme.colorScheme.secondary
    val colorHigh = MaterialTheme.colorScheme.tertiary
    
    // Overall amplitude for pulsing the entire component
    val maxAmplitude = visualizerData.maxOrNull() ?: 0f
    val animatedPulse by animateFloatAsState(
        targetValue = if (isPlaying) maxAmplitude else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pulse"
    )

    // Jitter for micro-vibrations
    val globalJitter by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "globalJitter"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            // Pulse in BOTH directions for a modern "stressed" effect
            scaleX = 1f + (animatedPulse * 0.15f * volume)
            scaleY = 1f + (animatedPulse * 0.2f * volume) // Vertical pulse enabled
        }
    ) {
        val spacing = 2.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        
        repeat(barCount) { i ->
            // Normalized distance from center (0 at middle, 1 at edges)
            val distFromCenter = Math.abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
            
            // Butterfly shape logic: taller on ends
            val shapeFactor = 0.3f + (distFromCenter * 0.7f)
            
            // Map data index symmetrically (Bass/Kick in the center, Highs at edges)
            val dataIndex = (distFromCenter * (visualizerData.size - 1)).toInt()
            val amplitude = visualizerData[dataIndex.coerceIn(0, visualizerData.size - 1)]
            
            // Color based on frequency type (like the circular one)
            val barColor = when {
                distFromCenter < 0.3f -> colorBass // Middle = Bass/Kick
                distFromCenter < 0.7f -> colorMid  // Mid = Vocals/Clap
                else -> colorHigh                 // Edges = Cymbals/Highs
            }
            
            val baseHeight = size.height
            val finalHeight = if (isPlaying) {
                baseHeight * shapeFactor * globalJitter * (0.6f + (amplitude * 0.9f))
            } else {
                baseHeight * shapeFactor * 0.5f
            }

            val colorAlpha = if (isPlaying) (0.4f + (amplitude * 0.6f)).coerceIn(0.2f, 1f) else 0.2f
            
            val left = i * (barWidth + spacing)
            val top = (size.height - finalHeight) / 2
            
            drawRoundRect(
                color = barColor.copy(alpha = colorAlpha),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, finalHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedBottomPlayer(
    item: MediaItem,
    isPlaying: Boolean,
    currentPos: Long,
    duration: Long,
    playbackSpeed: Float,
    volume: Float,
    visualizerData: FloatArray,
    shuffleMode: Boolean,
    repeatMode: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onCycleMode: () -> Unit,
    onClick: () -> Unit
) {
    var showVolumeControl by remember { mutableStateOf(false) }

    // RGB Animation for the background border
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_player")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )
    val rgbColor = Color.hsv(hue, 0.7f, 0.9f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(72.dp) // Smaller height
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .border(1.dp, rgbColor, RoundedCornerShape(16.dp)), // RGB Border
        color = Color.Transparent,
        tonalElevation = 12.dp
    ) {
        Column {
            // Very thin top progress line (Spotify style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(rgbColor.copy(alpha = 0.3f))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Artwork with Circular Progress
                Box(contentAlignment = Alignment.Center) {
                    val progress = if (duration > 0) currentPos.toFloat() / duration else 0f
                    
                    // Progress ring around the circle
                    Canvas(modifier = Modifier.size(54.dp)) {
                        drawArc(
                            color = rgbColor.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = rgbColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    if (item.albumArtUri != null) {
                        AsyncImage(
                            model = item.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = rgbColor)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Playback Mode Button
                IconButton(onClick = onCycleMode) {
                    val icon = when {
                        shuffleMode -> Icons.Default.Shuffle
                        repeatMode == Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                        repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Mode",
                        modifier = Modifier.size(22.dp),
                        tint = if (shuffleMode || repeatMode != Player.REPEAT_MODE_OFF) rgbColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Simplified controls for mini-player
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(28.dp))
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(44.dp)
                        .background(rgbColor.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = rgbColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

fun getLocalMedia(context: Context): List<MediaItem> {
    val mediaItems = mutableListOf<MediaItem>()
    
    // Audio
    val audioUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    
    context.contentResolver.query(
        audioUri,
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        ),
        null, null, null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val albumId = cursor.getLong(albumIdCol)
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
            
            mediaItems.add(MediaItem(
                id, cursor.getString(titleCol), cursor.getString(artistCol), cursor.getString(albumCol),
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                MediaType.AUDIO,
                cursor.getLong(dateCol),
                cursor.getLong(sizeCol),
                albumArtUri
            ))
        }
    }

    // Video
    val videoUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    context.contentResolver.query(
        videoUri,
        arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE
        ),
        null, null, null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            mediaItems.add(MediaItem(
                id, cursor.getString(titleCol), "Vidéo", cursor.getString(albumCol) ?: "Collection",
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                MediaType.VIDEO,
                cursor.getLong(dateCol),
                cursor.getLong(sizeCol)
            ))
        }
    }
    
    return mediaItems
}

fun shuffleQueuePhysical(player: Player) {
    if (player.mediaItemCount > 1) {
        val list = mutableListOf<Media3Item>()
        for (i in 0 until player.mediaItemCount) {
            list.add(player.getMediaItemAt(i))
        }
        val currentIdx = player.currentMediaItemIndex
        val currentItem = list.removeAt(currentIdx)
        list.shuffle()
        list.add(currentIdx, currentItem)
        
        // Use setMediaItems with resetPosition = false for seamless update
        player.setMediaItems(list, false)
        // No seekTo needed if item and index remain the same
    }
}
