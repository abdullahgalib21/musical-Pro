package com.example.ui

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.player.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlaybackRepeatMode {
    OFF, ALL, ONE
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MusicDatabase.getDatabase(application)
    private val dao = db.musicDao()
    val playerManager = MusicPlayerManager(application)

    // Sync variables with player states
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration

    // UI State lists
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Base flows from database
    val allTracksFromDb = dao.getAllTracks()
    val playlists = dao.getPlaylists()
    val favoriteTracks = dao.getFavoriteTracks()
    val recentTracks = dao.getRecentTracks()

    // Set of favorite IDs to make favorite toggles lightning-fast in the UI
    val favoriteIds: StateFlow<Set<String>> = dao.getFavorites()
        .map { list -> list.map { it.trackId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    // Playlist tracks cache: map of playlist ID to its tracks Flow
    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>> {
        return dao.getTracksForPlaylist(playlistId)
    }

    // Playback queue states
    private val _playQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playQueue = _playQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlaybackRepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    // Sleep Timer states
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _sleepTimeRemaining = MutableStateFlow<Long?>(null) // in ms
    val sleepTimeRemaining = _sleepTimeRemaining.asStateFlow()

    // Equalizer Preset states
    private val _equalizerPreset = MutableStateFlow("Normal")
    val equalizerPreset = _equalizerPreset.asStateFlow()

    // Scan state
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // App settings and personalization states stored in SharedPreferences
    private val prefs = application.getSharedPreferences("musical_prefs", Context.MODE_PRIVATE)

    private val _accentTheme = MutableStateFlow(prefs.getString("accent_theme", "GALAXY") ?: "GALAXY")
    val accentTheme = _accentTheme.asStateFlow()

    private val _playerSkin = MutableStateFlow(prefs.getString("player_skin", "VINYL") ?: "VINYL")
    val playerSkin = _playerSkin.asStateFlow()

    fun setAccentTheme(theme: String) {
        _accentTheme.value = theme
        prefs.edit().putString("accent_theme", theme).apply()
    }

    fun setPlayerSkin(skin: String) {
        _playerSkin.value = skin
        prefs.edit().putString("player_skin", skin).apply()
    }

    // Playback queue advanced operations
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentList = _playQueue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val moved = currentList.removeAt(fromIndex)
            currentList.add(toIndex, moved)
            _playQueue.value = currentList
            // Update index
            val activeTrack = currentTrack.value
            if (activeTrack != null) {
                _currentIndex.value = currentList.indexOfFirst { it.id == activeTrack.id }
            }
        }
    }

    fun removeTrackFromQueue(trackId: String) {
        val currentList = _playQueue.value.toMutableList()
        val indexToRemove = currentList.indexOfFirst { it.id == trackId }
        if (indexToRemove != -1) {
            if (currentList.size <= 1) {
                _playQueue.value = emptyList()
                _currentIndex.value = -1
                playerManager.stop()
            } else {
                val isActiveDeleted = _currentIndex.value == indexToRemove
                currentList.removeAt(indexToRemove)
                _playQueue.value = currentList
                
                if (isActiveDeleted) {
                    val nextIndex = indexToRemove.coerceAtMost(currentList.size - 1)
                    _currentIndex.value = nextIndex
                    playTrack(currentList[nextIndex], currentList)
                } else {
                    val activeTrack = currentTrack.value
                    if (activeTrack != null) {
                        _currentIndex.value = currentList.indexOfFirst { it.id == activeTrack.id }
                    }
                }
            }
        }
    }

    fun addToQueueNext(track: TrackEntity) {
        val currentList = _playQueue.value.toMutableList()
        val activeIdx = _currentIndex.value
        
        // Remove if already in upcoming part to avoid duplicate visual clutter
        val existingIndex = currentList.indexOfFirst { it.id == track.id }
        if (existingIndex != -1 && existingIndex > activeIdx) {
            currentList.removeAt(existingIndex)
        }

        if (currentList.isEmpty()) {
            playTrack(track)
        } else {
            val insertIndex = activeIdx + 1
            currentList.add(insertIndex.coerceAtMost(currentList.size), track)
            _playQueue.value = currentList
            Toast.makeText(getApplication(), "“${track.title}” পরবর্তীতে বাজানো হবে", Toast.LENGTH_SHORT).show()
        }
    }

    // Interactive custom lyrics retrieval and editor persistence system
    fun getLyricsForTrack(trackId: String): String {
        val customLy = prefs.getString("lyrics_$trackId", null)
        if (customLy != null) return customLy
        
        return when (trackId) {
            "demo_1" -> "ভোরের শান্ত হাওয়া বয়ে যায় বুকে...\nসব দ্বন্দ্ব দূরে সরে যাক শান্ত সুখে...\nমন যেন একা খুঁজে পায় নিজের ঠিকানা...\n৫২৮ হার্জ সুরে কোনো ক্লান্তি রয় না...\nশান্ত নিঃশ্বাসে প্রতিটি স্পন্দন পবিত্র হোক...\nমনের নির্জনতা যেন পরম শান্তি বয়ে আনুক..."
            "demo_2" -> "সন্ধ্যা ঘনিয়ে আসে দিগন্ত জুড়ে...\nমন হারিয়ে যায় কোনো শান্ত এক সুরে...\nশ্বাস নিন মৃদু কম্পনে...\nছেড়ে দিন আপনার সব চিন্তা...\nজেগে উঠুক সচেতন এক নতুন সত্তা...\nএই তো প্রকৃত ধ্যান, এই তো পরম প্রাপ্তি..."
            "demo_3" -> "তারাদের দেশে ওই মেঘের কূলে...\nঘুমের পরশ আসুক সব আঘাত ভুলে...\nনিস্তব্ধ রাতের বাতাসে মনের দোলা...\nমিশে যাক এই প্রশান্তিনিবিড় স্বপ্নছায়ায়...\nগভীর ঘুমের অলস ছোঁয়ায়...\nভেসে চলি আমরা সুদূর স্বপ্নের ভেলায়..."
            "demo_4" -> "Cruising through the retro neon lights...\nSynthesizer playing in the depth of night...\nStarlight pulses in my veins...\nNo more sorrow, no more pains...\nFeel the rhythm of the neon drive...\nMidnight echoes keep our souls alive..."
            else -> "দুঃখিত, এই গানটির কোনো লিরিক্স পাওয়া যায়নি! নিচে থাকা লিরিক্স এডিট বাটন চেপে আপনি নিজের লিরিক্স যোগ করতে পারেন!"
        }
    }

    fun saveLyricsForTrack(trackId: String, lyrics: String) {
        prefs.edit().putString("lyrics_$trackId", lyrics).apply()
    }

    init {
        // Auto-play next song on completion
        playerManager.onTrackCompletedListener = {
            nextTrack()
        }

        // Insert default demo songs in Room so they exist at first start
        viewModelScope.launch {
            insertDemoSongsIfNeeded()
        }
    }

    private suspend fun insertDemoSongsIfNeeded() {
        withContext(Dispatchers.IO) {
            val demoTracksList = listOf(
                TrackEntity(
                    id = "demo_1",
                    title = "Solitude (528Hz Ambient)",
                    artist = "Musical Ambient",
                    album = "Inner Tranquility",
                    duration = 180000L,
                    uriString = "demo://1",
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_2",
                    title = "Zen Evening (Mindful Breath)",
                    artist = "Musical Ambient",
                    album = "Zen Resonance",
                    duration = 240000L,
                    uriString = "demo://2",
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_3",
                    title = "Deep Dreams (Solfeggio Sleep)",
                    artist = "Musical Ambient",
                    album = "Slumber Harmonies",
                    duration = 300000L,
                    uriString = "demo://3",
                    isDemo = true
                ),
                TrackEntity(
                    id = "demo_4",
                    title = "Starlight Pulse (Synthwave)",
                    artist = "Musical Ambient",
                    album = "Midnight Retro",
                    duration = 210000L,
                    uriString = "demo://4",
                    isDemo = true
                )
            )
            // Insert default demo tracks
            dao.insertTracks(demoTracksList)
        }
    }

    fun scanLocalMusic(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            withContext(Dispatchers.IO) {
                // Clear existing non-demo tracks so we re-scan fresh
                dao.clearLocalTracks()
                
                val tracks = mutableListOf<TrackEntity>()
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                try {
                    context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val title = cursor.getString(titleCol) ?: "Unknown Track"
                            val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                            val album = cursor.getString(albumCol) ?: "Unknown Album"
                            val dur = cursor.getLong(durationCol)
                            val data = cursor.getString(dataCol)
                            
                            val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                            // Only add valid playable items
                            if (dur > 2000L) { // skip tiny ringtones
                                tracks.add(
                                    TrackEntity(
                                        id = "local_$id",
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = dur,
                                        uriString = trackUri.toString(),
                                        isDemo = false
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (tracks.isNotEmpty()) {
                    dao.insertTracks(tracks)
                }
            }
            _isScanning.value = false
        }
    }

    // Playback handling
    fun playTrack(track: TrackEntity, customQueueList: List<TrackEntity>? = null) {
        viewModelScope.launch {
            // Update queue
            val queue = customQueueList ?: listOf(track)
            _playQueue.value = queue
            val index = queue.indexOfFirst { it.id == track.id }
            _currentIndex.value = if (index != -1) index else 0

            // Play track via manager
            playerManager.play(track, _equalizerPreset.value)

            // Register in recent play list
            withContext(Dispatchers.IO) {
                dao.insertRecent(RecentPlayEntity(track.id))
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playerManager.pause()
        } else {
            val current = currentTrack.value
            if (current != null) {
                playerManager.resume()
            } else if (playQueue.value.isNotEmpty()) {
                val nextToPlay = playQueue.value.getOrNull(currentIndex.value.coerceAtLeast(0))
                if (nextToPlay != null) {
                    playTrack(nextToPlay, playQueue.value)
                }
            }
        }
    }

    fun nextTrack() {
        val queue = playQueue.value
        if (queue.isEmpty()) return

        val mode = _repeatMode.value
        val index = _currentIndex.value

        if (mode == PlaybackRepeatMode.ONE) {
            val current = currentTrack.value
            if (current != null) {
                playerManager.play(current, _equalizerPreset.value)
                return
            }
        }

        val nextIndex = when {
            _isShuffle.value -> (queue.indices).random()
            mode == PlaybackRepeatMode.ALL -> (index + 1) % queue.size
            else -> index + 1
        }

        if (nextIndex in queue.indices) {
            val nextTrack = queue[nextIndex]
            _currentIndex.value = nextIndex
            playerManager.play(nextTrack, _equalizerPreset.value)
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertRecent(RecentPlayEntity(nextTrack.id))
            }
        } else {
            // End of queue for RepeatMode.OFF
            playerManager.stop()
        }
    }

    fun prevTrack() {
        val queue = playQueue.value
        if (queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (currentPosition.value > 3000L) {
            seekTo(0L)
            return
        }

        val index = _currentIndex.value
        var prevIndex = index - 1
        if (prevIndex < 0) {
            prevIndex = if (_repeatMode.value == PlaybackRepeatMode.ALL) queue.size - 1 else 0
        }

        if (prevIndex in queue.indices) {
            val prevTrack = queue[prevIndex]
            _currentIndex.value = prevIndex
            playerManager.play(prevTrack, _equalizerPreset.value)
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertRecent(RecentPlayEntity(prevTrack.id))
            }
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    // Favorites handling
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = dao.getTrackById(trackId)
            val isFav = favoriteIds.value.contains(trackId)
            val songName = track?.title ?: "গানটি"
            
            if (isFav) {
                dao.deleteFavorite(trackId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "“$songName” পছন্দের তালিকা থেকে বাদ দেওয়া হয়েছে",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                dao.insertFavorite(FavoriteEntity(trackId))
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "“$songName” পছন্দের তালিকায় যুক্ত করা হয়েছে",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Playlists handling
    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank()) {
                dao.insertPlaylist(PlaylistEntity(name = name))
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackId))
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylistTrack(playlistId, trackId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemaining.value = null
            return
        }
        _sleepTimeRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = viewModelScope.launch {
            var millisLeft = minutes * 60 * 1000L
            while (millisLeft > 0) {
                kotlinx.coroutines.delay(1000)
                millisLeft -= 1000
                _sleepTimeRemaining.value = millisLeft
            }
            playerManager.stop()
            _sleepTimeRemaining.value = null
        }
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        playerManager.activePreset = preset
        val current = currentTrack.value
        if (current != null && current.isDemo && isPlaying.value) {
            playTrack(current, playQueue.value)
        }
    }

    fun updateTrackTags(id: String, title: String, artist: String, album: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateTrackDetails(id, title, artist, album)
            val current = currentTrack.value
            if (current != null && current.id == id) {
                withContext(Dispatchers.Main) {
                    val updatedTrack = current.copy(title = title, artist = artist, album = album)
                    _playQueue.value = _playQueue.value.map {
                        if (it.id == id) updatedTrack else it
                    }
                    playerManager.play(updatedTrack, _equalizerPreset.value)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        playerManager.stop()
    }
}
