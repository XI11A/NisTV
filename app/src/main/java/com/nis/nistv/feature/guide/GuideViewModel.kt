package com.nis.nistv.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.reminder.ProgramReminderManager
import com.nis.nistv.data.epg.EpgManager
import com.nis.nistv.data.epg.TimelineProgram
import com.nis.nistv.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GuideUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val windowStartMs: Long = 0L,
    val windowEndMs: Long = 0L,
    val schedules: Map<String, List<TimelineProgram>> = emptyMap(),
    val activeReminders: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class GuideViewModel(
    private val channelRepository: ChannelRepository,
    private val epgManager: EpgManager,
    private val reminderManager: ProgramReminderManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _schedules = MutableStateFlow<Map<String, List<TimelineProgram>>>(emptyMap())
    private val _windowStartMs: Long
    private val _windowEndMs: Long

    init {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            val min = get(Calendar.MINUTE)
            val roundedMin = if (min >= 30) 30 else 0
            set(Calendar.MINUTE, roundedMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _windowStartMs = calendar.timeInMillis - 30 * 60 * 1000L // 30 min before
        _windowEndMs = _windowStartMs + 3 * 60 * 60 * 1000L // 3 hour total window
    }

    private val allChannelsFlow = channelRepository.getAllChannels()
    private val categoriesFlow = channelRepository.getCategories()
    private val remindersFlow = reminderManager.getUpcomingReminders()

    val uiState: StateFlow<GuideUiState> = combine(
        allChannelsFlow,
        categoriesFlow,
        _selectedCategory,
        _schedules,
        remindersFlow
    ) { channels, categories, selectedCat, schedules, reminders ->
        val filtered = if (selectedCat == null) {
            channels
        } else {
            channels.filter { it.category.equals(selectedCat, ignoreCase = true) }
        }

        val reminderKeys = reminders.map { "${it.channelId}_${it.startEpochMs}" }.toSet()

        GuideUiState(
            channels = filtered,
            categories = categories,
            selectedCategory = selectedCat,
            windowStartMs = _windowStartMs,
            windowEndMs = _windowEndMs,
            schedules = schedules,
            activeReminders = reminderKeys,
            isLoading = channels.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GuideUiState(
            windowStartMs = _windowStartMs,
            windowEndMs = _windowEndMs
        )
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun loadScheduleForChannel(channel: ChannelEntity) {
        if (_schedules.value.containsKey(channel.id)) return
        viewModelScope.launch {
            val timeline = epgManager.getTimelineForChannel(
                channelId = channel.id,
                channelName = channel.cleanName.ifEmpty { channel.name },
                category = channel.category,
                windowStart = _windowStartMs,
                windowEnd = _windowEndMs
            )
            _schedules.update { current ->
                current + (channel.id to timeline)
            }
        }
    }

    fun setReminder(channelId: String, programTitle: String, startEpochMs: Long) {
        viewModelScope.launch {
            reminderManager.scheduleReminder(channelId, programTitle, startEpochMs)
        }
    }

    fun cancelReminder(channelId: String, startEpochMs: Long) {
        viewModelScope.launch {
            reminderManager.cancelReminder(channelId, startEpochMs)
        }
    }

    companion object {
        fun provideFactory(
            repository: ChannelRepository,
            epgManager: EpgManager,
            reminderManager: ProgramReminderManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GuideViewModel(repository, epgManager, reminderManager) as T
            }
        }
    }
}
