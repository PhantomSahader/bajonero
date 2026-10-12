package com.streetfood.radar.data.remote

import com.streetfood.radar.data.model.FoodSpotDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaz de Retrofit para consumir la API REST de StreetFood Radar en Django.
 * Soporta filtrado por categoría, precio máximo, comuna, región y las Killer Features:
 * 'healthy' (Alternativa Sana) y 'student_mode' (Modo Estudiante).
 */
interface StreetFoodApiService {

    @GET("api/food-spots/")
    suspend fun getFoodSpots(
        @Query("category") category: String? = null,
        @Query("max_price") maxPrice: Int? = null,
        @Query("comuna") comuna: String? = null,
        @Query("region") region: String? = null,
        @Query("healthy") isHealthy: Boolean? = null,
        @Query("student_mode") isStudentMode: Boolean? = null
    ): Response<List<FoodSpotDto>>
}
