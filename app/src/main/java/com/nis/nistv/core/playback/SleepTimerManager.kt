package com.nis.nistv.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SleepTimerState {
    data object Inactive : SleepTimerState
    data class Active(
        val remainingSeconds: Long,
        val totalSeconds: Long,
        val isEndOfEpisode: Boolean = false
    ) : SleepTimerState
}

sealed interface SleepTimerTickResult {
    data object Inactive : SleepTimerTickResult
    data class Active(val remainingSeconds: Long) : SleepTimerTickResult
    data object Expired : SleepTimerTickResult
}

/**
 * Pure state machine representing sleep timer logic.
 */
class SleepTimerStateMachine {
    private var targetTimeMs: Long = 0L
    private var totalDurationSec: Long = 0L
    private var isEndOfEpisode: Boolean = false
    var isRunning: Boolean = false
        private set

    fun start(minutes: Int, now: Long) {
        require(minutes > 0) { "Minutes must be positive" }
        totalDurationSec = minutes * 60L
        targetTimeMs = now + (totalDurationSec * 1000L)
        isEndOfEpisode = false
        isRunning = true
    }

    fun startEndOfEpisode() {
        totalDurationSec = 0L
        targetTimeMs = Long.MAX_VALUE
        isEndOfEpisode = true
        isRunning = true
    }

    fun tick(now: Long, endOfEpisodeReached: Boolean = false): SleepTimerTickResult {
        if (!isRunning) return SleepTimerTickResult.Inactive

        if (isEndOfEpisode) {
            if (endOfEpisodeReached) {
                isRunning = false
                return SleepTimerTickResult.Expired
            }
            return SleepTimerTickResult.Active(remainingSeconds = -1L)
        }

        val remainingMs = targetTimeMs - now
        if (remainingMs <= 0L) {
            isRunning = false
            return SleepTimerTickResult.Expired
        }

        val remainingSec = (remainingMs + 999L) / 1000L
        return SleepTimerTickResult.Active(remainingSec)
    }

    fun cancel() {
        isRunning = false
        targetTimeMs = 0L
        totalDurationSec = 0L
        isEndOfEpisode = false
    }

    fun snapshot(now: Long): SleepTimerState {
        if (!isRunning) return SleepTimerState.Inactive
        if (isEndOfEpisode) {
            return SleepTimerState.Active(remainingSeconds = -1L, totalSeconds = 0L, isEndOfEpisode = true)
        }
        val remainingSec = ((targetTimeMs - now).coerceAtLeast(0L) + 999L) / 1000L
        return SleepTimerState.Active(remainingSeconds = remainingSec, totalSeconds = totalDurationSec, isEndOfEpisode = false)
    }
}

/**
 * Global AppContainer singleton managing sleep timers for both Live TV and Offline Player.
 */
class SleepTimerManager(
    private val scope: CoroutineScope
) {
    private val stateMachine = SleepTimerStateMachine()
    private val _timerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    private val _onTimerExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onTimerExpired: SharedFlow<Unit> = _onTimerExpired.asSharedFlow()

    private var countdownJob: Job? = null
    private var externalExpiryAction: (suspend () -> Unit)? = null

    fun setExternalExpiryAction(action: (suspend () -> Unit)?) {
        this.externalExpiryAction = action
    }

    fun startTimer(minutes: Int) {
        countdownJob?.cancel()
        stateMachine.start(minutes, System.currentTimeMillis())
        _timerState.value = stateMachine.snapshot(System.currentTimeMillis())

        countdownJob = scope.launch(Dispatchers.Default) {
            while (stateMachine.isRunning) {
                delay(1000L)
                val now = System.currentTimeMillis()
                when (val result = stateMachine.tick(now)) {
                    is SleepTimerTickResult.Active -> {
                        _timerState.value = stateMachine.snapshot(now)
                    }
                    is SleepTimerTickResult.Expired -> {
                        _timerState.value = SleepTimerState.Inactive
                        triggerExpiry()
                        break
                    }
                    is SleepTimerTickResult.Inactive -> {
                        _timerState.value = SleepTimerState.Inactive
                        break
                    }
                }
            }
        }
    }

    fun startEndOfEpisode() {
        countdownJob?.cancel()
        stateMachine.startEndOfEpisode()
        _timerState.value = stateMachine.snapshot(System.currentTimeMillis())
    }

    fun notifyEndOfEpisodeReached() {
        if (stateMachine.isRunning) {
            val result = stateMachine.tick(System.currentTimeMillis(), endOfEpisodeReached = true)
            if (result is SleepTimerTickResult.Expired) {
                _timerState.value = SleepTimerState.Inactive
                scope.launch {
                    triggerExpiry()
                }
            }
        }
    }

    fun cancelTimer() {
        countdownJob?.cancel()
        countdownJob = null
        stateMachine.cancel()
        _timerState.value = SleepTimerState.Inactive
    }

    private suspend fun triggerExpiry() {
        _onTimerExpired.tryEmit(Unit)
        try {
            externalExpiryAction?.invoke()
        } catch (_: Exception) {}
    }
}
