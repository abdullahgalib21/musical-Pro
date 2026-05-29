package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity
import com.example.ui.MusicViewModel
import com.example.ui.PlaybackRepeatMode
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlin.math.abs
import kotlin.math.sin

data class ThemeColors(
    val bg: Color,
    val card: Color,
    val border: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textSecondary: Color = Color(0xFF9CA3AF)
)

// Deep Luxury Dark Palette
val ObsidianBg = Color(0xFF0A0B10)
val SurfaceCard = Color(0xFF141624)
val SurfaceBorder = Color(0xFF25293E)
val AccentIndigo = Color(0xFF6366F1)
val AccentMagenta = Color(0xFFEC4899)
val SoftGold = Color(0xFFF59E0B)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

@Composable
fun MainScreen(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    val currentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()
    val playerSkin by viewModel.playerSkin.collectAsStateWithLifecycle()

    val themeColors = remember(currentTheme) {
        when (currentTheme) {
            "FOREST" -> {
                ThemeColors(
                    bg = Color(0xFF040A08),
                    card = Color(0xFF0B1412),
                    border = Color(0xFF1E3530),
                    primaryAccent = Color(0xFF10B981),
                    secondaryAccent = Color(0xFFFBBF24)
                )
            }
            "VALENTINE" -> {
                ThemeColors(
                    bg = Color(0xFF14080D),
                    card = Color(0xFF220F19),
                    border = Color(0xFF3E1B2C),
                    primaryAccent = Color(0xFFEC4899),
                    secondaryAccent = Color(0xFFEAB308)
                )
            }
            "CYBERPUNK" -> {
                ThemeColors(
                    bg = Color(0xFF030507),
                    card = Color(0xFF0C1322),
                    border = Color(0xFF1E293B),
                    primaryAccent = Color(0xFF06B6D4),
                    secondaryAccent = Color(0xFF22C55E)
                )
            }
            else -> { // "GALAXY"
                ThemeColors(
                    bg = Color(0xFF0A0B10),
                    card = Color(0xFF141624),
                    border = Color(0xFF25293E),
                    primaryAccent = Color(0xFF6366F1),
                    secondaryAccent = Color(0xFFEC4899)
                )
            }
        }
    }

    val ObsidianBg = themeColors.bg
    val SurfaceCard = themeColors.card
    val SurfaceBorder = themeColors.border
    val AccentIndigo = themeColors.primaryAccent
    val AccentMagenta = themeColors.secondaryAccent

    val allTracks by viewModel.allTracksFromDb.collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    var selectedTab by remember { mutableStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    
    // Popup Dialogue states
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToAddTo by remember { mutableStateOf<TrackEntity?>(null) }
    var playlistToViewDetails by remember { mutableStateOf<PlaylistEntity?>(null) }
    
    var activeTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToEditDetails by remember { mutableStateOf<TrackEntity?>(null) }
    var showSleepTimerSelector by remember { mutableStateOf(false) }
    var showEqualizerSelector by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    
    val sleepTimeRemaining by viewModel.sleepTimeRemaining.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    
    // Request permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.scanLocalMusic(context)
            Toast.makeText(context, "গান খোঁজা শুরু হয়েছে...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context, 
                "পারমিশন দেওয়া হয়নি। ডেমো গানগুলি শুনতে পারেন!", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun triggerStorageScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            )
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    // Trigger initial scan silently if permissions are already given
    LaunchedEffect(Unit) {
        // Just runs a silent scanner
        viewModel.scanLocalMusic(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // MAIN SPLASH SCROLL LAYER
        Column(modifier = Modifier.fillMaxSize()) {
            // HIGH-END TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Musical",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "অফলাইন প্রিমিয়াম প্লেয়ার",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Theme/Skins Selection button
                    IconButton(
                        onClick = { showThemeSelector = true },
                        modifier = Modifier
                            .background(SurfaceBorder.copy(alpha = 0.4f), shape = CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes",
                            tint = AccentIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick Sleep Timer status icon
                    IconButton(
                        onClick = { showSleepTimerSelector = true },
                        modifier = Modifier
                            .background(
                                color = if (sleepTimeRemaining != null) AccentMagenta.copy(alpha = 0.2f) else SurfaceBorder.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .size(38.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimeRemaining != null) AccentMagenta else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            if (sleepTimeRemaining != null) {
                                val minutesLeft = (sleepTimeRemaining ?: 0) / 1000 / 60
                                Text(
                                    text = "${minutesLeft}m",
                                    fontSize = 8.sp,
                                    color = AccentMagenta,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Scan / Refresh Button with loading state
                    Button(
                        onClick = { triggerStorageScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentIndigo
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("scan_storage_button")
                    ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("খোঁজা হচ্ছে...", fontSize = 12.sp, color = Color.White)
                    } else {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Scan Storage",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("স্ক্যান", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                }
            }

            // SEARCH BAR IN-ROW
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("song_search_input"),
                placeholder = { Text("গান, শিল্পী বা অ্যালবাম খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY SCROLLABLE TAB CHIPS
            val tabs = listOf("গানের তালিকা", "পছন্দের", "প্লেলিস্ট", "সাম্প্রতিক", "স্ট্যাটস")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                contentColor = AccentIndigo,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentMagenta,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color.White else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RENDER SELECTED TAB SCENE
            val filteredTracks = remember(allTracks, searchQuery) {
                if (searchQuery.isBlank()) {
                    allTracks
                } else {
                    allTracks.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> { // ALL TUNES LIST
                        if (filteredTracks.isEmpty()) {
                            EmptyPlaceholderState(
                                iconOption = Icons.Default.MusicNote,
                                mainMessage = "কোন গান পাওয়া যায়নি!",
                                tipMessage = "উপরে 'স্ক্যান' বাটনে চাপ দিয়ে ফোনে থাকা অফলাইন গান খুজে বের করুন।"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    TrackItemCard(
                                        track = track,
                                        isCurrent = currentTrack?.id == track.id,
                                        isFavorite = favoriteIds.contains(track.id),
                                        onPlayClick = { viewModel.playTrack(track, filteredTracks) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track.id) },
                                        onMoreClick = { activeTrackOptions = track }
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // FAVORITES LIST
                        val favsFiltered = remember(favoriteTracks, searchQuery) {
                            if (searchQuery.isBlank()) favoriteTracks else favoriteTracks.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        if (favsFiltered.isEmpty()) {
                            EmptyPlaceholderState(
                                iconOption = Icons.Default.Favorite,
                                mainMessage = "প্রিয় গান শূন্য!",
                                tipMessage = "গান তালিকার পাশে থাকা লাভ রিঅ্যাক্ট বাটনে ট্যাপ করে গান প্রিয় তালিকায় যুক্ত করুন।"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
                            ) {
                                items(favsFiltered, key = { it.id }) { track ->
                                    TrackItemCard(
                                        track = track,
                                        isCurrent = currentTrack?.id == track.id,
                                        isFavorite = true,
                                        onPlayClick = { viewModel.playTrack(track, favsFiltered) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track.id) },
                                        onMoreClick = { activeTrackOptions = track }
                                    )
                                }
                            }
                        }
                    }
                    2 -> { // PLAYLIST TAB VIEW
                        PlaylistViewSection(
                            playlists = playlists,
                            onCreateClick = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                            onPlaylistSelect = { playlist -> playlistToViewDetails = playlist },
                            searchQuery = searchQuery
                        )
                    }
                    3 -> { // RECENT TRACKS LIST
                        val recentsFiltered = remember(recentTracks, searchQuery) {
                            if (searchQuery.isBlank()) recentTracks else recentTracks.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        if (recentsFiltered.isEmpty()) {
                            EmptyPlaceholderState(
                                iconOption = Icons.Default.Settings,
                                mainMessage = "কোন গান খেলা হয়নি!",
                                tipMessage = "একটি গান চালনা করুন, আপনার চালিত শেষ গানগুলো এখানে দেখতে পাবেন।"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
                            ) {
                                items(recentsFiltered, key = { it.id }) { track ->
                                    TrackItemCard(
                                        track = track,
                                        isCurrent = currentTrack?.id == track.id,
                                        isFavorite = favoriteIds.contains(track.id),
                                        onPlayClick = { viewModel.playTrack(track, recentsFiltered) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track.id) },
                                        onMoreClick = { activeTrackOptions = track }
                                    )
                                }
                            }
                        }
                    }
                    4 -> { // STATS DASHBOARD VIEW
                        StatsDashboardView(recentTracks = recentTracks, favoriteTracks = favoriteTracks, ObsidianBg = ObsidianBg, SurfaceCard = SurfaceCard, SurfaceBorder = SurfaceBorder, AccentIndigo = AccentIndigo, AccentMagenta = AccentMagenta, TextSecondary = TextSecondary)
                    }
                }
            }
        }

        // FIXED MINI PLAYER (AT THE FOOT OF SCREEN)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AnimatedVisibility(
                visible = currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                currentTrack?.let { track ->
                    MiniPlayerBar(
                        track = track,
                        isPlaying = isPlaying,
                        currentPos = currentPosition,
                        totalDuration = duration,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.nextTrack() },
                        onBarClick = { isPlayerExpanded = true }
                    )
                }
            }
        }

        // FULL SCREEN COMPOSITIVE EXQUISITE MUSIC PLAYER PANEL SHEET
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeOut()
        ) {
            currentTrack?.let { track ->
                FullPlayerOverlay(
                    viewModel = viewModel,
                    track = track,
                    isPlaying = isPlaying,
                    currentTime = currentPosition,
                    totalDuration = duration,
                    isFavorite = favoriteIds.contains(track.id),
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    equalizerPreset = equalizerPreset,
                    sleepTimeRemaining = sleepTimeRemaining,
                    onEqualizerClick = { showEqualizerSelector = true },
                    onSleepTimerClick = { showSleepTimerSelector = true },
                    onCollapse = { isPlayerExpanded = false },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onPrev = { viewModel.prevTrack() },
                    onSeek = { pos -> viewModel.seekTo(pos) },
                    onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() }
                )
            }
        }

        // DIALOGS & OVERLAYS SYSTEM

        // TRACK CONTEXT OPTIONS DIALOG / MODAL
        activeTrackOptions?.let { track ->
            Dialog(onDismissRequest = { activeTrackOptions = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "গান অপশনস",
                            color = AccentMagenta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = track.artist,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Custom spacer divider
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        // OPTION 0: Play Next / Add to Queue
                        TextButton(
                            onClick = {
                                viewModel.addToQueueNext(track)
                                activeTrackOptions = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("পরবর্তীতে বাজান (Play Next)", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        // OPTION 1: Add to Playlists
                        TextButton(
                            onClick = {
                                playlistToAddTo = track
                                activeTrackOptions = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.List, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("প্লেলিস্টে যোগ করুন", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        // OPTION 2: Edit Metadata
                        TextButton(
                            onClick = {
                                trackToEditDetails = track
                                activeTrackOptions = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("বিবরণ এডিট করুন", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        // OPTION 3: Share (Simulated)
                        TextButton(
                            onClick = {
                                activeTrackOptions = null
                                try {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "এই চমৎকার গানটি শুনুন: ${track.title} - ${track.artist} (Musical অ্যাপ থেকে)")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "শেয়ার করা সম্ভব হয়নি!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("শেয়ার করুন", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        // OPTION 4: Set as Ringtone (Simulated feedback)
                        TextButton(
                            onClick = {
                                activeTrackOptions = null
                                Toast.makeText(context, "রিংটোন হিসেবে সফলভাবে সেট করা হয়েছে! (সিমুলেটেড)", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("রিংটোন হিসেবে সেট করুন", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { activeTrackOptions = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("বন্ধ করুন", color = Color.White)
                        }
                    }
                }
            }
        }

        // METADATA TAGS EDIT DIALOG
        trackToEditDetails?.let { track ->
            var tempTitle by remember { mutableStateOf(track.title) }
            var tempArtist by remember { mutableStateOf(track.artist) }
            var tempAlbum by remember { mutableStateOf(track.album) }

            Dialog(onDismissRequest = { trackToEditDetails = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "বিবরণ সংশোধন করুন",
                            color = AccentMagenta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // FIELD 1: Title
                        OutlinedTextField(
                            value = tempTitle,
                            onValueChange = { tempTitle = it },
                            placeholder = { Text("গান বা সুরের নাম", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentIndigo
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        // FIELD 2: Artist
                        OutlinedTextField(
                            value = tempArtist,
                            onValueChange = { tempArtist = it },
                            placeholder = { Text("শিল্পী / স্রষ্টা", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentIndigo
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        // FIELD 3: Album
                        OutlinedTextField(
                            value = tempAlbum,
                            onValueChange = { tempAlbum = it },
                            placeholder = { Text("অ্যালবাম", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentIndigo
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { trackToEditDetails = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("বাতিল", color = TextSecondary)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.updateTrackTags(track.id, tempTitle, tempArtist, tempAlbum)
                                    trackToEditDetails = null
                                    Toast.makeText(context, "গানটির তথ্য সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("সংরক্ষণ", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // SLEEP TIMER SELECT MODAL
        if (showSleepTimerSelector) {
            Dialog(onDismissRequest = { showSleepTimerSelector = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "স্লিপ টাইমার",
                            color = AccentIndigo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "নির্দিষ্ট সময় পর স্বয়ংক্রিয়ভাবে প্লেব্যাক বন্ধ করতে নির্বাচন করুন।",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        val timerOptions = listOf(
                            "টাইমার বন্ধ" to 0,
                            "৫ মিনিট" to 5,
                            "১৫ মিনিট" to 15,
                            "৩০ মিনিট" to 30,
                            "৪৫ মিনিট" to 45,
                            "৬০ মিনিট" to 60
                        )

                        timerOptions.forEach { (label, minutes) ->
                            val isSelected = if (minutes == 0) sleepTimeRemaining == null else {
                                sleepTimeRemaining != null && Math.abs((sleepTimeRemaining ?: 0) - (minutes * 60 * 1000L)) < 11000L
                            }
                            
                            TextButton(
                                onClick = {
                                    viewModel.setSleepTimer(minutes)
                                    showSleepTimerSelector = false
                                    if (minutes > 0) {
                                        Toast.makeText(context, "$label সেট করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "টাইমার বন্ধ করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label, 
                                        color = if (isSelected) AccentMagenta else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = "Selected", 
                                            tint = AccentMagenta, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showSleepTimerSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("বন্ধ করুন", color = Color.White)
                        }
                    }
                }
            }
        }

        // EQUALIZER SELECT DIALOG
        if (showEqualizerSelector) {
            Dialog(onDismissRequest = { showEqualizerSelector = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ইকুয়ালাইজার প্রি-সেট",
                            color = AccentMagenta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "আপনার পছন্দসই শব্দ তরঙ্গের অনুধাবন স্টাইল নির্বাচন করুন।",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        val eqPresets = listOf(
                            "Normal" to "স্বাভাবিক (Normal)",
                            "Bass Boost" to "দ্বিগুণ বেস বুস্টার (Bass Boost)",
                            "Acoustic" to "অ্যাকোস্টিক মেলোডি (Acoustic)",
                            "Vocal Booster" to "স্পষ্ট কণ্ঠস্বর (Vocal Booster)",
                            "Classical" to "শাস্ত্রীয় শান্ত টিউন (Classical)"
                        )

                        eqPresets.forEach { (presetVal, description) ->
                            val isSelected = equalizerPreset == presetVal
                            
                            TextButton(
                                onClick = {
                                    viewModel.setEqualizerPreset(presetVal)
                                    showEqualizerSelector = false
                                    Toast.makeText(context, "$presetVal প্রি-সেট কার্যকর হয়েছে।", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = description, 
                                        color = if (isSelected) AccentIndigo else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = "Selected", 
                                            tint = AccentIndigo, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showEqualizerSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("বন্ধ করুন", color = Color.White)
                        }
                    }
                }
            }
        }

        // DIALOGS & OVERLAYS SYSTEM
        if (showCreatePlaylistDialog) {
            var playlistTitle by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showCreatePlaylistDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "নতুন প্লেলিস্ট তৈরি করুন",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = playlistTitle,
                            onValueChange = { playlistTitle = it },
                            placeholder = { Text("প্লেলিস্টের নাম দিন") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentIndigo
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                Text("বাতিল", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (playlistTitle.isNotBlank()) {
                                        viewModel.createPlaylist(playlistTitle)
                                        showCreatePlaylistDialog = false
                                        Toast.makeText(context, "প্লেলিস্ট তৈরি হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                            ) {
                                Text("তৈরি করুন")
                            }
                        }
                    }
                }
            }
        }

        // THEME & SKIN STYLE SELECT OVERLAY
        if (showThemeSelector) {
            Dialog(onDismissRequest = { showThemeSelector = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "থিম ও প্লেয়ার স্কিন কাস্টমাইজেশন",
                            color = AccentMagenta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Section 1: Color Themes
                        Text(
                            text = "অ্যাকসেন্ট কালার থিম",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
                        )

                        val themeOptions = listOf(
                            "GALAXY" to "গ্যালাক্সি ইন্ডিগো (Galaxy Indigo)",
                            "FOREST" to "অরণ্য হিল (Forest Green)",
                            "VALENTINE" to "লাল ভেলভেট (Velvet Rose)",
                            "CYBERPUNK" to "সাইবার নিয়ন (Cyberpunk Lime)"
                        )

                        themeOptions.forEach { (themeCode, name) ->
                            val isChosen = currentTheme == themeCode
                            TextButton(
                                onClick = { viewModel.setAccentTheme(themeCode) },
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = if (isChosen) AccentIndigo else TextSecondary, fontSize = 14.sp)
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Section 2: Player Skins
                        Text(
                            text = "প্লেয়ার ভিজ্যুয়াল স্কিন",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
                        )

                        val skinOptions = listOf(
                            "VINYL" to "ক্লাসিক ভাইনাইল ঘূর্ণন (Classic Vinyl)",
                            "GLASS" to "গ্লাস মর্ফিক আর্ট (Glassmorphic Glow)",
                            "RETRO" to "রেট্রো ক্যাসেট প্লেয়ার (Retro Tape Deck)"
                        )

                        skinOptions.forEach { (skinCode, name) ->
                            val isChosen = playerSkin == skinCode
                            TextButton(
                                onClick = { viewModel.setPlayerSkin(skinCode) },
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = if (isChosen) AccentIndigo else TextSecondary, fontSize = 14.sp)
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showThemeSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("প্রয়োগ করুন", color = Color.White)
                        }
                    }
                }
            }
        }

        playlistToAddTo?.let { track ->
            Dialog(onDismissRequest = { playlistToAddTo = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            "প্লেলিস্টে যুক্ত করুন",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text(
                            track.title,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (playlists.isEmpty()) {
                            Text(
                                "আপনার কোন প্লেলিস্ট নেই। প্রথমে একটি প্লেলিস্ট তৈরি করুন!",
                                color = SoftGold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(playlists) { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.addTrackToPlaylist(playlist.id, track.id)
                                                playlistToAddTo = null
                                                Toast.makeText(context, "'${playlist.name}' প্লেলিস্টে যুক্ত করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.List, contentDescription = null, tint = AccentIndigo)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(playlist.name, color = Color.White, fontSize = 14.sp)
                                    }
                                    HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showCreatePlaylistDialog = true
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("নতুন প্লেলিস্ট", color = AccentMagenta, fontSize = 13.sp)
                                }
                            }
                            TextButton(onClick = { playlistToAddTo = null }) {
                                Text("বাহির", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        playlistToViewDetails?.let { playlist ->
            var playlistTracks by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }
            val playlistTracksFlow = remember(playlist.id) { viewModel.getTracksForPlaylist(playlist.id) }
            val tracksState by playlistTracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            
            Dialog(onDismissRequest = { playlistToViewDetails = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.7f)
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${tracksState.size} টি গান",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { playlistToViewDetails = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (tracksState.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "প্লেলিস্টটি ফাঁকা! গান ড্যাশবোর্ডের থ্রি-ডট মেনু থেকে গান যুক্ত করুন।",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                items(tracksState) { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.playTrack(track, tracksState)
                                                playlistToViewDetails = null
                                                isPlayerExpanded = true
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        if (track.isDemo) listOf(AccentMagenta, AccentIndigo)
                                                        else listOf(AccentIndigo, SurfaceBorder)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                track.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                track.artist,
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(onClick = {
                                            viewModel.removeTrackFromPlaylist(playlist.id, track.id)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// EMPTY PLACEHOLDER SCREEN COMPRESSED GUEST
@Composable
fun EmptyPlaceholderState(
    iconOption: androidx.compose.ui.graphics.vector.ImageVector,
    mainMessage: String,
    tipMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentIndigo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconOption,
                contentDescription = null,
                tint = AccentIndigo,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            mainMessage,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            tipMessage,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

// COMPOSABLE CARD REPRESENTING INDIVIDUAL AUDIO ELEMENT
@Composable
fun TrackItemCard(
    track: TrackEntity,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onPlayClick() }
            .testTag("track_card_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SurfaceCard.copy(alpha = 0.8f) else SurfaceCard
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrent) AccentIndigo.copy(alpha = 0.6f) else SurfaceBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ALBUM ART ARTIFICIAL NEUROMORPHIC VIEW
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            if (track.isDemo) {
                                listOf(AccentMagenta, AccentIndigo)
                            } else {
                                listOf(AccentIndigo, SurfaceBorder)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    // Small floating indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // SONG METADATA
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) AccentIndigo else Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isDemo) {
                        Text(
                            text = "AMBIENT SYNTH",
                            color = AccentMagenta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(AccentMagenta.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // DURATION DISPATCHER
            val minutes = track.duration / 1000 / 60
            val seconds = (track.duration / 1000) % 60
            val durString = String.format("%d:%02d", minutes, seconds)
            Text(
                text = durString,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // FAVORITE CARD ICON
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag("favorite_button_${track.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AccentMagenta else TextSecondary
                )
            }

            // ADD TO PLAYLIST OPTION TRIGGER
            IconButton(onClick = onMoreClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Add to playlist menu",
                    tint = TextSecondary
                )
            }
        }
    }
}

// COMPARTMENTALIZED PLAYLIST VIEW PANEL
@Composable
fun PlaylistViewSection(
    playlists: List<PlaylistEntity>,
    onCreateClick: () -> Unit,
    onDeletePlaylist: (Int) -> Unit,
    onPlaylistSelect: (PlaylistEntity) -> Unit,
    searchQuery: String
) {
    val playlistsFiltered = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists else playlists.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "আমার প্লেলিস্টসমূহ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("create_playlist_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (playlistsFiltered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "কোন প্লেলিস্ট নেই! একটি প্লেলিস্ট তৈরি করতে উপরে চাপ দিন।",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 90.dp, top = 4.dp)
            ) {
                items(playlistsFiltered, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onPlaylistSelect(p) }
                            .testTag("playlist_card_${p.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    p.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "প্লেলিস্ট দেখতে ট্যাপ করুন",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { onDeletePlaylist(p.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// GLOWING CONTEMPORARY MINI PLAYER AT THE FOOT
@Composable
fun MiniPlayerBar(
    track: TrackEntity,
    isPlaying: Boolean,
    currentPos: StateFlow<Long>,
    totalDuration: StateFlow<Long>,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBarClick: () -> Unit
) {
    val posVal by currentPos.collectAsStateWithLifecycle(initialValue = 0L)
    val durVal by totalDuration.collectAsStateWithLifecycle(initialValue = 1L)
    
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin_mini")
    val spinningFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "spin"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onBarClick() }
            .testTag("mini_player"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning Album Visual
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (track.isDemo) listOf(AccentMagenta, AccentIndigo)
                                else listOf(AccentIndigo, SurfaceBorder)
                            )
                        )
                        .rotate(if (isPlaying) spinningFactor else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.testTag("play_pause_button")) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = onNextClick, modifier = Modifier.testTag("skip_forward_button")) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Realtime Linear Track Seek Slider line
            val progressPercent = if (durVal > 0) posVal.toFloat() / durVal.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressPercent.coerceIn(0f, 1f) },
                color = AccentIndigo,
                trackColor = SurfaceBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}

// LUXURIOUS FULL SCREEN OVERLAY VIEW
@Composable
fun FullPlayerOverlay(
    viewModel: MusicViewModel,
    track: TrackEntity,
    isPlaying: Boolean,
    currentTime: StateFlow<Long>,
    totalDuration: StateFlow<Long>,
    isFavorite: Boolean,
    repeatMode: PlaybackRepeatMode,
    isShuffle: Boolean,
    equalizerPreset: String,
    sleepTimeRemaining: Long?,
    onEqualizerClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    val posVal by currentTime.collectAsStateWithLifecycle(initialValue = 0L)
    val durVal by totalDuration.collectAsStateWithLifecycle(initialValue = 1L)

    val currentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()
    val playerSkin by viewModel.playerSkin.collectAsStateWithLifecycle()
    val rawQueue by viewModel.playQueue.collectAsStateWithLifecycle(initialValue = emptyList())

    val themeColors = remember(currentTheme) {
        when (currentTheme) {
            "FOREST" -> {
                ThemeColors(
                    bg = Color(0xFF040A08),
                    card = Color(0xFF0B1412),
                    border = Color(0xFF1E3530),
                    primaryAccent = Color(0xFF10B981),
                    secondaryAccent = Color(0xFFFBBF24),
                    textSecondary = Color(0xFF8B9B97)
                )
            }
            "VALENTINE" -> {
                ThemeColors(
                    bg = Color(0xFF14080D),
                    card = Color(0xFF220F19),
                    border = Color(0xFF3E1B2C),
                    primaryAccent = Color(0xFFEC4899),
                    secondaryAccent = Color(0xFFEAB308),
                    textSecondary = Color(0xFFC4A5B5)
                )
            }
            "CYBERPUNK" -> {
                ThemeColors(
                    bg = Color(0xFF030507),
                    card = Color(0xFF0C1322),
                    border = Color(0xFF1E293B),
                    primaryAccent = Color(0xFF06B6D4),
                    secondaryAccent = Color(0xFF22C55E),
                    textSecondary = Color(0xFF64748B)
                )
            }
            else -> { // "GALAXY"
                ThemeColors(
                    bg = Color(0xFF0A0B10),
                    card = Color(0xFF141624),
                    border = Color(0xFF25293E),
                    primaryAccent = Color(0xFF6366F1),
                    secondaryAccent = Color(0xFFEC4899),
                    textSecondary = Color(0xFF9CA3AF)
                )
            }
        }
    }

    val ObsidianBg = themeColors.bg
    val SurfaceCard = themeColors.card
    val SurfaceBorder = themeColors.border
    val AccentIndigo = themeColors.primaryAccent
    val AccentMagenta = themeColors.secondaryAccent
    val TextSecondary = themeColors.textSecondary

    var activeMiniTab by remember { mutableStateOf(0) } // 0: Player with skins, 1: Lyrics, 2: Play queue reordering

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_full_rotation")
    val spinningDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing)
        ),
        label = "large_spin"
    )

    // Breathe LFO for visualizer
    val pulseTransition = rememberInfiniteTransition(label = "pulse_lfo")
    val visualizerScale by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HEADER BAR OVERLAY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    "এখন বাজছে",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentMagenta else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // MIDDLE TAB SWITCH CHIPS (Player, Lyrics, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("প্লেয়ার", "লিরিক্স", "প্লে কিউ (${rawQueue.size})").forEachIndexed { index, title ->
                    val isSelected = activeMiniTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AccentIndigo else Color.Transparent)
                            .clickable { activeMiniTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // PRIMARY HUB RENDER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeMiniTab) {
                    0 -> { // TAB 0: PLAYER BODY SKINS
                        when (playerSkin) {
                            "GLASS" -> { // GLASSMORPHIC GLOW SKIN
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(horizontal = 8.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(170.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            AccentIndigo.copy(alpha = 0.28f * visualizerScale),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = AccentMagenta,
                                                modifier = Modifier.size(50.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = track.album,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                            Text("গ্লাস প্লেয়ার ডেক (Glass HUD)", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                            "RETRO" -> { // RETRO STEREO DECK TAPE CASSETTE SKIN
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(horizontal = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                                    border = BorderStroke(2.dp, Color(0xFF2C2C35))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("CHROME BIAS D-90 • DOLBY B", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(vertical = 12.dp)
                                                .background(Color.Black, RoundedCornerShape(8.dp))
                                                .border(2.dp, Color(0xFF3E3E48), RoundedCornerShape(8.dp)),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2E2E36))
                                                    .rotate(if (isPlaying) spinningDegrees else 0f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    for (a in 0 until 6) {
                                                        val deg = a * 60f
                                                        val rad = Math.toRadians(deg.toDouble())
                                                        val cx = (size.width / 2f) + kotlin.math.cos(rad).toFloat() * 16.dp.toPx()
                                                        val cy = (size.height / 2f) + kotlin.math.sin(rad).toFloat() * 16.dp.toPx()
                                                        drawCircle(color = Color.Black, radius = 3.dp.toPx(), center = Offset(cx, cy))
                                                    }
                                                }
                                                Box(modifier = Modifier.size(16.dp).background(Color.Black, CircleShape))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(24.dp)
                                                    .background(Color(0xFF121215), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(modifier = Modifier.size(6.dp).background(AccentMagenta, CircleShape))
                                                    Box(modifier = Modifier.size(6.dp).background(AccentIndigo, CircleShape))
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2E2E36))
                                                    .rotate(if (isPlaying) spinningDegrees else 0f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    for (a in 0 until 6) {
                                                        val deg = a * 60f
                                                        val rad = Math.toRadians(deg.toDouble())
                                                        val cx = (size.width / 2f) + kotlin.math.cos(rad).toFloat() * 16.dp.toPx()
                                                        val cy = (size.height / 2f) + kotlin.math.sin(rad).toFloat() * 16.dp.toPx()
                                                        drawCircle(color = Color.Black, radius = 3.dp.toPx(), center = Offset(cx, cy))
                                                    }
                                                }
                                                Box(modifier = Modifier.size(16.dp).background(Color.Black, CircleShape))
                                            }
                                        }

                                        Text("হাই-ফাই রেট্রো ক্যাসেট ডেক (HI-FI Cassette)", color = TextSecondary, fontSize = 9.sp)
                                    }
                                }
                            }
                            else -> { // DEFAULT: CLASSIC ROTATING VINYL SKIN
                                Box(
                                    modifier = Modifier
                                        .size(230.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(AccentIndigo.copy(alpha = 0.4f), Color.Transparent)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(210.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF11131E))
                                            .rotate(if (isPlaying) spinningDegrees else 0f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = 85.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                                            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = 65.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                                            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = 45.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        if (track.isDemo) listOf(AccentMagenta, AccentIndigo)
                                                        else listOf(AccentIndigo, SurfaceBorder)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(ObsidianBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // TAB 1: SYNCED OFFLINE LYRICS HUB
                        var localLyrics by remember(track.id) { mutableStateOf("লোড হচ্ছে...") }
                        var showLyricsEditState by remember { mutableStateOf(false) }

                        LaunchedEffect(track.id) {
                            localLyrics = viewModel.getLyrics(track.id) ?: "কোন লিরিক্স পাওয়া যায়নি! নিচে সবুজ কলামের 'এডিট' বাটনে চাপ দিয়ে লিরিক্স লিখে সংরক্ষণ করুন।"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                    Text(
                                        text = localLyrics,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        color = Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { showLyricsEditState = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("লিরিক্স এডিট ও অনুবাদ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // LYRICS EDITOR MODAL OVERLAY
                        if (showLyricsEditState) {
                            var textDraft by remember { mutableStateOf(if (localLyrics.contains("কোন লিরিক্স পাওয়া যায়নি")) "" else localLyrics) }
                            Dialog(onDismissRequest = { showLyricsEditState = false }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    border = BorderStroke(1.dp, SurfaceBorder)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text("লিরিক্স এডিটর (অফলাইন)", color = AccentMagenta, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = textDraft,
                                            onValueChange = { textDraft = it },
                                            modifier = Modifier.fillMaxWidth().height(180.dp),
                                            placeholder = { Text("গানের লিরিক্স এখানে লিখুন...") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = AccentIndigo
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { showLyricsEditState = false }) {
                                                Text("বাতিল", color = TextSecondary)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.saveLyrics(track.id, textDraft)
                                                    localLyrics = textDraft.ifBlank { "কোন লিরিক্স পাওয়া যায়নি! নিচে 'এডিট' বাটনে চাপ দিয়ে লিরিক্স লিখে সংরক্ষণ করুন।" }
                                                    showLyricsEditState = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                                            ) {
                                                Text("সংরক্ষণ", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // TAB 2: ACTIVE REORDERABLE PLAY QUEUE MANAGER
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            if (rawQueue.isEmpty()) {
                                EmptyPlaceholderState(
                                    iconOption = Icons.Default.List,
                                    mainMessage = "প্লে কিউ একদম ফাকা!",
                                    tipMessage = "একটি গান চালনা করুন অথবা গানের তালিকা থেকে পরবর্তীতে বাজান চাপুন।"
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                ) {
                                    itemsIndexed(rawQueue) { idx, qTrack ->
                                        val isActive = qTrack.id == track.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isActive) AccentIndigo.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isActive) AccentIndigo.copy(alpha = 0.4f) else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                color = if (isActive) AccentMagenta else TextSecondary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.width(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(qTrack.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text(qTrack.artist, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                                            }

                                            // Reorder up controller
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        viewModel.reorderPlayQueue(idx, idx - 1)
                                                    }
                                                },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = if (idx > 0) Color.White else Color.Gray, modifier = Modifier.size(14.dp))
                                            }

                                            // Reorder down controller
                                            IconButton(
                                                onClick = {
                                                    if (idx < rawQueue.size - 1) {
                                                        viewModel.reorderPlayQueue(idx, idx + 1)
                                                    }
                                                },
                                                enabled = idx < rawQueue.size - 1,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = if (idx < rawQueue.size - 1) Color.White else Color.Gray, modifier = Modifier.size(14.dp))
                                            }

                                            // Delete icon
                                            IconButton(
                                                onClick = { viewModel.removeFromQueue(idx) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AccentMagenta, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DYNAMIC DANCING SOUNDWAVE ORNAMENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = 24
                    val width = size.width
                    val middle = size.height / 2f
                    val space = width / points
                    
                    for (i in 0 until points) {
                        val progressFactor = if (isPlaying) {
                            val phase = (System.currentTimeMillis() % 1400) / 1400.0 * 2.0 * java.lang.Math.PI
                            val offset = i * (2.0 * java.lang.Math.PI / points)
                            kotlin.math.sin(phase + offset).toFloat()
                        } else {
                            kotlin.math.sin(i * 0.3).toFloat() * 0.15f
                        }
                        val barHeight = (6.dp.toPx() + (visualizerScale * 20.dp.toPx() * kotlin.math.abs(progressFactor)))
                        val startX = i * space + (space / 2f)
                        drawLine(
                            brush = Brush.verticalGradient(listOf(AccentMagenta, AccentIndigo)),
                            start = Offset(startX, middle - (barHeight / 2f)),
                            end = Offset(startX, middle + (barHeight / 2f)),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // TRACK METADATA DISPLAY
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = track.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // PREMIUM EQUALIZER & SLEEP TIMER BADGES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceBorder.copy(alpha = 0.5f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onEqualizerClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Equalizer",
                        tint = AccentIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ইকু : $equalizerPreset",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sleepTimeRemaining != null) AccentMagenta.copy(alpha = 0.15f) else SurfaceBorder.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp, 
                            color = if (sleepTimeRemaining != null) AccentMagenta.copy(alpha = 0.6f) else Color.Transparent, 
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSleepTimerClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimeRemaining != null) AccentMagenta else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimeRemaining != null) {
                            val mins = sleepTimeRemaining / 1000 / 60
                            val secs = (sleepTimeRemaining / 1000) % 60
                            String.format("টাইমার %d:%02d", mins, secs)
                        } else "স্লিপ টাইমার",
                        color = if (sleepTimeRemaining != null) AccentMagenta else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // INTERACTIVE SEEK SYSTEM
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (durVal > 0) posVal.toFloat() / durVal.toFloat() else 0f,
                    onValueChange = { percent ->
                        onSeek((percent * durVal).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentIndigo,
                        activeTrackColor = AccentIndigo,
                        inactiveTrackColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMins = posVal / 1000 / 60
                    val currentSecs = (posVal / 1000) % 60
                    Text(
                        text = String.format("%d:%02d", currentMins, currentSecs),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    val totalMins = durVal / 1000 / 60
                    val totalSecs = (durVal / 1000) % 60
                    Text(
                        text = String.format("%d:%02d", totalMins, totalSecs),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // PRIMARY PLAYER HUB CONTROLS PANEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Text(
                        "⇄",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isShuffle) AccentIndigo else Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = onPrev) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Prev",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentMagenta, AccentIndigo)
                            )
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "PlayPause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val glyph = when (repeatMode) {
                            PlaybackRepeatMode.OFF -> "↻"
                            PlaybackRepeatMode.ALL -> "↻"
                            PlaybackRepeatMode.ONE -> "➀"
                        }
                        val tint = if (repeatMode != PlaybackRepeatMode.OFF) AccentIndigo else Color.White.copy(alpha = 0.5f)
                        
                        Text(
                            glyph,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint
                        )
                        if (repeatMode == PlaybackRepeatMode.ALL) {
                            Text(
                                "ALL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tint
                            )
                        }
                    }
                }
            }
        }
    }
}

// LUXURIOUS MUSIC STATS AND ANALYTICS DASHBOARD
@Composable
fun StatsDashboardView(
    recentTracks: List<TrackEntity>,
    favoriteTracks: List<TrackEntity>,
    ObsidianBg: Color,
    SurfaceCard: Color,
    SurfaceBorder: Color,
    AccentIndigo: Color,
    AccentMagenta: Color,
    TextSecondary: Color
) {
    val totalMinutes = remember(recentTracks) {
        val basePlaycount = recentTracks.size
        (basePlaycount * 3.5).toInt()
    }

    val styleText = remember(recentTracks) {
        val totalDemo = recentTracks.count { it.isDemo }
        if (totalDemo > recentTracks.size / 2) {
            "শান্ত ধ্যানসঙ্গীত (Zen Ambient)"
        } else {
            "অফলাইন মেলোডি (Offline Melodies)"
        }
    }

    val favoriteCount = favoriteTracks.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp, start = 20.dp, end = 20.dp)
    ) {
        item {
            Text(
                text = "মিউজিক স্ট্যাটস & বিশ্লেষণ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("মোট শোনা হয়েছে", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalMinutes মিনিট", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = AccentMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("প্রিয় গান রয়েছে", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$favoriteCount টি গান", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(AccentIndigo.copy(alpha = 0.15f), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SoftGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("আপনার মিউজিক ব্যক্তিত্ব", fontSize = 11.sp, color = TextSecondary)
                        Text(styleText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "শ্রুতি প্রবাহ এনালাইজার (Weekly Audio Analysis)",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "আপনার সাপ্তাহিক শোনার সময়ের উপর ভিত্তি করে তৈরি গ্রাফ",
                        fontSize = 11.sp, color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = androidx.compose.ui.graphics.Path()
                            val width = size.width
                            val height = size.height
                            
                            val points = listOf(0.15f, 0.45f, 0.25f, 0.85f, 0.55f, 0.95f, 0.65f)
                            val stepX = width / (points.size - 1)
                            
                            path.moveTo(0f, height * (1f - points[0]))
                            for (i in 1 until points.size) {
                                val cx = (i - 1) * stepX + stepX / 2f
                                val cy1 = height * (1f - points[i - 1])
                                val cy2 = height * (1f - points[i])
                                val targetX = i * stepX
                                path.quadraticBezierTo(cx, cy1, targetX, cy2)
                            }

                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(AccentIndigo, AccentMagenta)),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            path.lineTo(width, height)
                            path.lineTo(0f, height)
                            path.close()
                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(
                                    listOf(AccentIndigo.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )

                            for (i in points.indices) {
                                drawCircle(
                                    color = if (i % 2 == 0) AccentIndigo else AccentMagenta,
                                    radius = 4.dp.toPx(),
                                    center = Offset(i * stepX, height * (1f - points[i]))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র").forEach { day ->
                            Text(day, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "শোনার ধরণ বিশ্লেষণ (Listening Spectrum)",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    listOf(
                        "শান্ত টোন (Acoustic Ambient)" to 0.75f,
                        "উচ্চ বেস (Heavy Bass Synth)" to 0.45f,
                        "সাধারণ সুর (Soft Melodies)" to 0.85f,
                        "রেকর্ডড ভয়েস (Vocal Pods)" to 0.25f
                    ).forEach { (styleName, valRate) ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(styleName, fontSize = 11.sp, color = TextSecondary)
                                Text("${(valRate * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { valRate },
                                color = AccentIndigo,
                                trackColor = SurfaceBorder,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
