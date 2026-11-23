package com.nis.nistv.feature.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.epg.EpgManager
import com.nis.nistv.data.epg.ProgrammeInfo
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.saver.DataUsageManager
import com.nis.nistv.data.zapper.ZapContext
import com.nis.nistv.playback.AspectRatioMode
import com.nis.nistv.playback.PlaybackStatus
import com.nis.nistv.playback.PlayerController
import com.nis.nistv.playback.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class PlayerViewModel(
    private val context: Context,
    private val channelRepository: ChannelRepository,
    private val playerController: PlayerController,
    private val epgManager: EpgManager,
    private val userPreferences: UserPreferences,
    private val initialChannelId: String,
    val zapContext: ZapContext? = null,
    val dataUsageManager: DataUsageManager? = null
) : ViewModel() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _allChannels = MutableStateFlow<List<ChannelEntity>>(emptyList())
    val allChannels: StateFlow<List<ChannelEntity>> = _allChannels.asStateFlow()

    private var currentChannelIndex: Int = -1

    val uiState: StateFlow<PlayerUiState> = playerController.uiState

    // Live EPG Info
    private val _programmeInfo = MutableStateFlow<ProgrammeInfo?>(null)
    val programmeInfo: StateFlow<ProgrammeInfo?> = _programmeInfo.asStateFlow()

    // Brightness and Volume HUD states
    private val _brightness = MutableStateFlow(0.5f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _showBrightnessHud = MutableStateFlow(false)
    val showBrightnessHud: StateFlow<Boolean> = _showBrightnessHud.asStateFlow()

    private val _showVolumeHud = MutableStateFlow(false)
    val showVolumeHud: StateFlow<Boolean> = _showVolumeHud.asStateFlow()

    // Lock and Drawer controls
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    // Sleep Timer (Minutes remaining, null if off)
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Related channels
    val relatedChannels: StateFlow<List<ChannelEntity>> = combine(
        _allChannels,
        uiState
    ) { channels, state ->
        val current = state.channel
        if (current != null) {
            channels.filter { it.id != current.id && (it.category == current.category || it.language == current.language) }.take(15)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite status
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var hudHideJob: Job? = null

    init {
        initVolume()
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { list ->
                _allChannels.value = list
                currentChannelIndex = list.indexOfFirst { it.id == initialChannelId }
            }
        }
        loadAndPlayChannel(initialChannelId)
    }

    private fun initVolume() {
        try {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
            _volume.value = currentVol / maxVol
        } catch (_: Exception) {}
    }

    fun loadAndPlayChannel(channelId: String, forceReload: Boolean = false) {
        viewModelScope.launch {
            try {
                val currentUi = playerController.uiState.value
                val isAlreadyActive = !forceReload &&
                    currentUi.channel?.id == channelId &&
                    (currentUi.status == PlaybackStatus.PLAYING ||
                     currentUi.status == PlaybackStatus.PAUSED ||
                     currentUi.status == PlaybackStatus.BUFFERING ||
                     currentUi.status == PlaybackStatus.LOADING)

                val relation = channelRepository.getChannelWithStreamsSync(channelId)
                if (relation != null) {
                    if (!isAlreadyActive) {
                        playerController.playChannel(
                            channel = relation.channel,
                            streams = relation.streams
                        )
                    }
                    // Watch history
                    channelRepository.recordWatch(channelId = channelId)
                    zapContext?.updateCurrentChannel(channelId)

                    // Dynamic launcher shortcut: Resume last channel (Phase 3 F9)
                    com.nis.nistv.core.util.AppShortcutsHelper.updateLastWatchedChannelShortcut(
                        context = context,
                        channelId = channelId,
                        channelName = relation.channel.cleanName.ifEmpty { relation.channel.name }
                    )

                    // Check favorite
                    launch {
                        channelRepository.isFavorite(channelId = channelId).collect {
                            _isFavorite.value = it
                        }
                    }
                }

                // Load real or dynamic EPG schedule
                relation?.channel?.let { ch ->
                    val info = epgManager.getProgrammeForChannel(
                        channelId = ch.id,
                        channelName = ch.cleanName.ifEmpty { ch.name },
                        category = ch.category
                    )
                    _programmeInfo.value = info
                    playerController.setProgrammeTitle(info.title)
                }
            } catch (e: Exception) {
                // Prevent crash on any unexpected channel data error
            }
        }
    }

    private val _showZapHud = MutableStateFlow(false)
    val showZapHud: StateFlow<Boolean> = _showZapHud.asStateFlow()
    private var zapHudJob: Job? = null

    fun showZapHud() {
        zapHudJob?.cancel()
        _showZapHud.value = true
        zapHudJob = viewModelScope.launch {
            delay(2000)
            _showZapHud.value = false
        }
    }

    fun playNextChannel(lockedChannelIds: Set<String> = emptySet()) {
        val nextId = zapContext?.getNextChannelId(lockedChannelIds)
        if (nextId != null) {
            loadAndPlayChannel(nextId)
            showZapHud()
            return
        }
        val list = _allChannels.value
        if (list.isNotEmpty() && currentChannelIndex != -1) {
            val nextIndex = (currentChannelIndex + 1) % list.size
            currentChannelIndex = nextIndex
            loadAndPlayChannel(list[nextIndex].id)
            showZapHud()
        }
    }

    fun playPreviousChannel(lockedChannelIds: Set<String> = emptySet()) {
        val prevId = zapContext?.getPreviousChannelId(lockedChannelIds)
        if (prevId != null) {
            loadAndPlayChannel(prevId)
            showZapHud()
            return
        }
        val list = _allChannels.value
        if (list.isNotEmpty() && currentChannelIndex != -1) {
            val prevIndex = if (currentChannelIndex - 1 < 0) list.size - 1 else currentChannelIndex - 1
            currentChannelIndex = prevIndex
            loadAndPlayChannel(list[prevIndex].id)
            showZapHud()
        }
    }

    fun jumpToChannelNumber(number: Int, lockedChannelIds: Set<String> = emptySet()) {
        val targetId = zapContext?.getChannelByIndex(number, lockedChannelIds)
        if (targetId != null) {
            loadAndPlayChannel(targetId)
            showZapHud()
        }
    }

    fun toggleLastChannel(lockedChannelIds: Set<String> = emptySet()) {
        val lastId = zapContext?.getLastChannelId(lockedChannelIds) ?: uiState.value.previousChannelId
        if (!lastId.isNullOrBlank()) {
            loadAndPlayChannel(lastId)
            showZapHud()
        }
    }

    fun toggleListenMode() {
        playerController.toggleListenMode()
    }

    fun startMultiView(
        secondaryChannel: ChannelEntity,
        multiViewController: com.nis.nistv.playback.multiview.MultiViewController
    ) {
        val primaryChannel = uiState.value.channel ?: return
        val primaryStreams = uiState.value.streams
        viewModelScope.launch {
            val secondaryRelation = channelRepository.getChannelWithStreams(secondaryChannel.id).firstOrNull()
            val secondaryStreams = secondaryRelation?.streams ?: emptyList()
            playerController.pause()
            multiViewController.startMultiView(
                primaryChannel = primaryChannel,
                primaryStreams = primaryStreams,
                secondaryChannel = secondaryChannel,
                secondaryStreams = secondaryStreams
            )
        }
    }

    fun replaceMultiViewChannel(
        slot: com.nis.nistv.playback.multiview.MultiViewSlot,
        channel: ChannelEntity,
        multiViewController: com.nis.nistv.playback.multiview.MultiViewController
    ) {
        viewModelScope.launch {
            val rel = channelRepository.getChannelWithStreams(channel.id).firstOrNull()
            val streams = rel?.streams ?: emptyList()
            if (slot == com.nis.nistv.playback.multiview.MultiViewSlot.PRIMARY) {
                val secSlot = multiViewController.uiState.value.secondarySlot
                val secChannel = secSlot.channel ?: channel
                val secStreams = secSlot.streams
                multiViewController.startMultiView(
                    primaryChannel = channel,
                    primaryStreams = streams,
                    secondaryChannel = secChannel,
                    secondaryStreams = secStreams
                )
            } else {
                multiViewController.setSecondaryChannel(channel, streams)
            }
        }
    }

    fun quickSwitchPreviousChannel() {
        val prevId = uiState.value.previousChannelId
        if (!prevId.isNullOrBlank()) {
            loadAndPlayChannel(prevId)
        }
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        playerController.selectAudioTrack(groupIndex, trackIndex)
    }

    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        playerController.selectSubtitleTrack(groupIndex, trackIndex)
    }

    fun toggleDiagnostics() {
        playerController.toggleDiagnostics()
    }

    fun retry() {
        playerController.retryCurrentStream()
    }

    fun selectSource(index: Int) {
        playerController.selectSource(index)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun cycleAspectRatio() {
        playerController.cycleAspectRatio()
    }

    fun setControlsVisible(visible: Boolean) {
        if (!_isLocked.value) {
            playerController.setControlsVisible(visible)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId = channelId)
            _isFavorite.value = !_isFavorite.value
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        playerController.setControlsVisible(true)
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun adjustVolume(delta: Float) {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newRatio = (_volume.value + delta).coerceIn(0f, 1f)
            val newIndex = (newRatio * maxVol).toInt().coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newIndex, 0)

            _volume.value = newRatio
            _showVolumeHud.value = true
            _showBrightnessHud.value = false
            triggerHudTimer()
        } catch (_: Exception) {}
    }

    fun adjustBrightness(delta: Float, activity: Activity?) {
        val newBrightness = (_brightness.value + delta).coerceIn(0.01f, 1f)
        _brightness.value = newBrightness
        _showBrightnessHud.value = true
        _showVolumeHud.value = false

        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = newBrightness
            window.attributes = lp
        }

        triggerHudTimer()
    }

    private fun triggerHudTimer() {
        hudHideJob?.cancel()
        hudHideJob = viewModelScope.launch {
            delay(1500)
            _showBrightnessHud.value = false
            _showVolumeHud.value = false
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes

        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60_000)
                    remaining -= 1
                    _sleepTimerMinutes.value = remaining
                }
                // When timer reaches zero, pause playback!
                playerController.togglePlayPause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        hudHideJob?.cancel()
    }

    companion object {
        fun provideFactory(
            context: Context,
            repository: ChannelRepository,
            playerController: PlayerController,
            epgManager: EpgManager,
            userPreferences: UserPreferences,
            channelId: String,
            zapContext: ZapContext? = null,
            dataUsageManager: DataUsageManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(
                    context,
                    repository,
                    playerController,
                    epgManager,
                    userPreferences,
                    channelId,
                    zapContext,
                    dataUsageManager
                ) as T
            }
        }
    }
}
