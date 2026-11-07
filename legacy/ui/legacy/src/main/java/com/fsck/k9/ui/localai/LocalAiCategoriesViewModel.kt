package com.fsck.k9.ui.localai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalAiCategoriesViewModel(
    private val repository: LocalAiRepository,
    private val modelManager: LocalModelManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        LocalAiUiState(overview = repository.loadOverview()),
    )
    val state: StateFlow<LocalAiUiState> = mutableState.asStateFlow()

    private var scanJob: Job? = null

    fun scan() {
        if (scanJob?.isActive == true) return

        scanJob = viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isScanning = true,
                    progress = null,
                    lastSummary = null,
                    errorMessage = null,
                )
            }

            try {
                val summary = repository.scan { progress ->
                    mutableState.update { state -> state.copy(progress = progress) }
                }

                mutableState.update { state ->
                    state.copy(
                        isScanning = false,
                        progress = null,
                        overview = summary.overview,
                        lastSummary = summary,
                    )
                }
            } catch (e: Exception) {
                mutableState.update { state ->
                    state.copy(
                        isScanning = false,
                        progress = null,
                        errorMessage = e.message ?: "Classification failed",
                    )
                }
            }
        }
    }

    fun clearCache() {
        repository.clearCache()
        mutableState.update { state ->
            state.copy(
                overview = repository.loadOverview(),
                lastSummary = null,
                errorMessage = null,
            )
        }
    }

    fun messagesFor(category: EmailCategory): List<CachedCategory> = repository.messagesFor(category)

    fun modelDescription(): String = modelManager.describeStatus()
}

data class LocalAiUiState(
    val isScanning: Boolean = false,
    val progress: ScanProgress? = null,
    val overview: List<CategoryOverview> = emptyList(),
    val lastSummary: ScanSummary? = null,
    val errorMessage: String? = null,
)
