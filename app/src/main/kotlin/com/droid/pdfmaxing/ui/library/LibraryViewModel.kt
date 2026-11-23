package com.droid.pdfmaxing.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droid.pdfmaxing.data.RecentDocumentRepository
import com.droid.pdfmaxing.data.model.RecentDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecentDocumentRepository(application)

    val recentDocuments: StateFlow<List<RecentDocument>> = repository.recentDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun removeDocument(uriString: String) {
        viewModelScope.launch {
            repository.removeDocument(uriString)
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            repository.clearRecents()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearching() {
        val next = !_uiState.value.isSearching
        _uiState.value = _uiState.value.copy(
            isSearching = next,
            searchQuery = if (!next) "" else _uiState.value.searchQuery
        )
    }
}
