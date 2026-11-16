package com.eplaytime.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eplaytime.app.data.model.RadioSearchField
import com.eplaytime.app.data.model.RadioStation
import com.eplaytime.app.data.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val repository = RadioRepository(context)

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations = _stations.asStateFlow()
    private val _favorites = MutableStateFlow(repository.favorites())
    val favorites = _favorites.asStateFlow()
    private val _recents = MutableStateFlow(repository.recents())
    val recents = _recents.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init { loadPopular() }

    fun loadPopular() = load { repository.popular() }

    fun search(query: String, field: RadioSearchField) {
        if (query.isBlank()) return loadPopular()
        load { repository.search(query, field) }
    }

    fun toggleFavorite(station: RadioStation) {
        _favorites.value = repository.toggleFavorite(station)
    }

    fun recordPlay(station: RadioStation) {
        _recents.value = repository.addRecent(station)
        viewModelScope.launch { repository.registerClick(station.uuid) }
    }

    private fun load(block: suspend () -> List<RadioStation>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _stations.value = block()
                if (_stations.value.isEmpty()) _error.value = "No working stations found"
            } catch (_: Exception) {
                _error.value = "Radio directory is unavailable. Check your connection and retry."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
