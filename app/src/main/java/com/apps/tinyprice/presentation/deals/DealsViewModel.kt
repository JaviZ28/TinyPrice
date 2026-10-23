package com.apps.tinyprice.presentation.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.tinyprice.data.model.ProductOffer
import com.apps.tinyprice.data.repository.DealsRepository
import com.apps.tinyprice.data.repository.DealsRepositoryProvider
import com.apps.tinyprice.data.repository.RemoteSearchException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DealsUiState(
    val query: String = "",
    val offers: List<ProductOffer> = emptyList(),
    val hasSearched: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPreviewData: Boolean = true,
    val selectedOffer: ProductOffer? = null
)

sealed interface DealsEvent {
    data class QueryChanged(val value: String) : DealsEvent
    data object SubmitSearch : DealsEvent
    data class UseSuggestion(val value: String) : DealsEvent
    data class OpenOffer(val offer: ProductOffer) : DealsEvent
    data object Back : DealsEvent
}

class DealsViewModel(
    private val repository: DealsRepository = DealsRepositoryProvider.create()
) : ViewModel() {

    private var searchJob: Job? = null

    private val _state = MutableStateFlow(
        DealsUiState(isPreviewData = repository.isPreviewData)
    )
    val state = _state.asStateFlow()

    fun onEvent(event: DealsEvent) {
        when (event) {
            is DealsEvent.QueryChanged ->
                _state.update {
                    it.copy(query = event.value, errorMessage = null)
                }

            DealsEvent.SubmitSearch ->
                search(_state.value.query)

            is DealsEvent.UseSuggestion -> {
                _state.update { it.copy(query = event.value) }
                search(event.value)
            }

            is DealsEvent.OpenOffer ->
                _state.update { it.copy(selectedOffer = event.offer) }

            DealsEvent.Back ->
                _state.update { it.copy(selectedOffer = null) }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            _state.update {
                it.copy(
                    hasSearched = true,
                    offers = emptyList(),
                    isLoading = false,
                    errorMessage = "Escribe un producto para buscar."
                )
            }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    hasSearched = true,
                    isLoading = true,
                    errorMessage = null,
                    selectedOffer = null
                )
            }

            try {
                val results = repository.search(query)
                _state.update {
                    it.copy(offers = results, isLoading = false)
                }
            } catch (e: RemoteSearchException) {
                _state.update {
                    it.copy(
                        offers = emptyList(),
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        offers = emptyList(),
                        isLoading = false,
                        errorMessage = "Ocurrió un error inesperado."
                    )
                }
            }
        }
    }
}