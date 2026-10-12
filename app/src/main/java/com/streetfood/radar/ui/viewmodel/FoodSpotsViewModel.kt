package com.streetfood.radar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.radar.data.local.AppDatabase
import com.streetfood.radar.data.remote.RetrofitClient
import com.streetfood.radar.data.repository.FoodSpotRepositoryImpl
import com.streetfood.radar.domain.model.FoodSpot
import com.streetfood.radar.domain.repository.FoodSpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que implementa la arquitectura Single Source of Truth (SSOT).
 * 
 * La UI observa 'uiState', el cual se alimenta reactivamente del Flow de Room Database.
 * Cuando se ejecuta 'refresh()', el repositorio descarga de Retrofit y actualiza Room,
 * provocando la reemisión automática hacia la UI sin provocar parpadeos ni crasheos por falta de internet.
 */
class FoodSpotsViewModel(
    application: Application,
    private val repository: FoodSpotRepository = FoodSpotRepositoryImpl(
        foodSpotDao = AppDatabase.getDatabase(application).foodSpotDao(),
        apiService = RetrofitClient.apiService
    )
) : AndroidViewModel(application) {

    private val isRefreshing = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<String?>(null)

    // Filtros activos en memoria
    val selectedCategory = MutableStateFlow<String?>("ALL")
    val isHealthyFilter = MutableStateFlow(false)
    val isStudentMode = MutableStateFlow(false)

    /**
     * SSOT: Combina el Flow de la base de datos Room con los estados de filtro y red.
     * Expuesto a la UI mediante StateFlow optimizado con WhileSubscribed.
     */
    val uiState: StateFlow<FoodSpotsUiState> = combine(
        repository.foodSpots,
        selectedCategory,
        isHealthyFilter,
        isStudentMode,
        isRefreshing
    ) { allSpots, cat, healthy, student, refreshing ->
        if (allSpots.isEmpty() && refreshing) {
            FoodSpotsUiState.Loading
        } else {
            val filtered = allSpots.filter { spot ->
                val matchCat = cat == null || cat == "ALL" || spot.category.name.equals(cat, ignoreCase = true)
                val matchHealthy = !healthy || spot.isHealthy
                val matchStudent = !student || spot.price <= 3000
                matchCat && matchHealthy && matchStudent
            }

            if (filtered.isEmpty()) {
                val title = if (healthy) {
                    "¡Ups! Aún no tenemos opciones saludables mapeadas aquí. 🥦"
                } else {
                    "¡Ups! No hay locales registrados con estos filtros. 🥢"
                }
                FoodSpotsUiState.Empty(
                    isHealthyFilter = healthy,
                    message = title,
                    subtitle = "Prueba desactivando algún filtro o cambiando de categoría."
                )
            } else {
                FoodSpotsUiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FoodSpotsUiState.Loading
    )

    init {
        // Al iniciar, refrescamos silenciosamente contra la API de red
        refresh()
    }

    /**
     * Intenta refrescar los datos remotos y persistirlos en Room.
     * Si no hay internet, captura el error de forma segura.
     */
    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            val result = repository.refreshData()
            result.onFailure { error ->
                userMessage.value = error.message
            }
            result.onSuccess {
                userMessage.value = null
            }
            isRefreshing.value = false
        }
    }

    fun setCategory(category: String?) {
        selectedCategory.value = category
    }

    fun toggleHealthy() {
        isHealthyFilter.value = !isHealthyFilter.value
    }

    fun toggleStudentMode() {
        isStudentMode.value = !isStudentMode.value
    }

    fun clearUserMessage() {
        userMessage.value = null
    }
}
