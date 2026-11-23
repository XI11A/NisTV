package com.nis.nistv.feature.livetv.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.PlaylistDao
import com.nis.nistv.core.database.entity.PlaylistEntity
import com.nis.nistv.data.playlist.PlaylistSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val syncingPlaylistId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PlaylistsViewModel(
    private val playlistDao: PlaylistDao,
    private val playlistSyncManager: PlaylistSyncManager
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    fun addPlaylist(name: String, sourceType: String, urlOrPath: String, autoRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = playlistSyncManager.addPlaylist(name, sourceType, urlOrPath, autoRefresh)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Playlist added successfully") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to add playlist"
                    )
                }
            }
        }
    }

    fun syncPlaylist(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(syncingPlaylistId = id, errorMessage = null, successMessage = null) }
            val result = playlistSyncManager.syncPlaylist(id)
            if (result.isSuccess) {
                val summary = result.getOrNull()?.summary ?: "Updated"
                _uiState.update { it.copy(syncingPlaylistId = null, successMessage = "Playlist refreshed: $summary") }
            } else {
                _uiState.update {
                    it.copy(
                        syncingPlaylistId = null,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to refresh playlist"
                    )
                }
            }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            playlistSyncManager.setPlaylistEnabled(id, enabled)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        viewModelScope.launch {
            playlistSyncManager.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            playlistSyncManager.deletePlaylist(id)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        fun provideFactory(
            playlistDao: PlaylistDao,
            playlistSyncManager: PlaylistSyncManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaylistsViewModel(playlistDao, playlistSyncManager) as T
            }
        }
    }
}
