package com.nis.nistv.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nis.nistv.data.backup.BackupParseResult
import com.nis.nistv.data.backup.BackupPreview
import com.nis.nistv.data.backup.BackupRestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val includeRecentSearches: Boolean = true,
    val preview: BackupPreview? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BackupRestoreViewModel(
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun setIncludeRecentSearches(include: Boolean) {
        _uiState.update { it.copy(includeRecentSearches = include) }
    }

    suspend fun generateExportData(): String {
        return backupRestoreManager.createExportJson(
            includeRecentSearches = _uiState.value.includeRecentSearches
        )
    }

    fun onBackupFileRead(jsonString: String) {
        when (val result = backupRestoreManager.parseBackupJson(jsonString)) {
            is BackupParseResult.Success -> {
                _uiState.update { it.copy(preview = result.preview, errorMessage = null) }
            }
            is BackupParseResult.VersionMismatch -> {
                _uiState.update {
                    it.copy(
                        preview = null,
                        errorMessage = "Backup file version ${result.foundVersion} is not supported by this version of NisTV."
                    )
                }
            }
            is BackupParseResult.ChecksumError -> {
                _uiState.update {
                    it.copy(preview = null, errorMessage = result.message)
                }
            }
            is BackupParseResult.Corrupt -> {
                _uiState.update {
                    it.copy(preview = null, errorMessage = "Failed to parse backup: ${result.error}")
                }
            }
        }
    }

    fun executeRestore(replaceExisting: Boolean) {
        val preview = _uiState.value.preview ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true) }
            val result = backupRestoreManager.restoreBackup(preview.payload, replaceExisting)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        preview = null,
                        successMessage = "Backup successfully restored (${if (replaceExisting) "Replaced" else "Merged"})"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearPreview() {
        _uiState.update { it.copy(preview = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
