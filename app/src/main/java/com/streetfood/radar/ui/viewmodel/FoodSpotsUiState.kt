package com.streetfood.radar.ui.viewmodel

import com.streetfood.radar.domain.model.FoodSpot

/**
 * Estados de UI basados en MVI / UDF (Unidirectional Data Flow).
 */
sealed interface FoodSpotsUiState {
    object Loading : FoodSpotsUiState
    
    data class Success(val spots: List<FoodSpot>) : FoodSpotsUiState

    data class Empty(
        val isHealthyFilter: Boolean,
        val message: String,
        val subtitle: String
    ) : FoodSpotsUiState

    data class Error(val message: String) : FoodSpotsUiState
}
