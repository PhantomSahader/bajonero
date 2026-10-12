package com.streetfood.radar.domain.repository

import com.streetfood.radar.domain.model.FoodSpot

import kotlinx.coroutines.flow.Flow

interface FoodSpotRepository {
    /**
     * SSOT: Emite reactivamente la lista de locales desde el almacenamiento local.
     */
    val foodSpots: Flow<List<FoodSpot>>

    /**
     * Sincroniza los datos con la API remota e impacta la base de datos local.
     */
    suspend fun refreshData(): Result<Unit>

    suspend fun getFoodSpots(
        category: String? = null,
        maxPrice: Int? = null,
        comuna: String? = null,
        region: String? = null,
        isHealthy: Boolean? = null,
        isStudentMode: Boolean? = null
    ): Result<List<FoodSpot>>
}
