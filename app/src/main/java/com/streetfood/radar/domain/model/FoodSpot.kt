package com.streetfood.radar.domain.model

/**
 * Modelo de Dominio de Clean Architecture para la aplicación Android.
 * Desacoplado de librerías de serialización como Gson o Jackson.
 */
data class FoodSpot(
    val id: Int,
    val name: String,
    val category: SpotCategory,
    val region: String,
    val comuna: String,
    val address: String,
    val price: Int,
    val quantity: String,
    val isHealthy: Boolean,
    val rating: Double,
    val latitude: Double,
    val longitude: Double
) {
    val formattedPrice: String
        get() = "$${String.format("%,d", price).replace(',', '.')}"
}

enum class SpotCategory {
    HANDROLL,
    COMPLETO,
    SOPAIPILLA,
    UNKNOWN;

    companion object {
        fun fromString(value: String): SpotCategory = when (value.uppercase()) {
            "HANDROLL" -> HANDROLL
            "COMPLETO" -> COMPLETO
            "SOPAIPILLA" -> SOPAIPILLA
            else -> UNKNOWN
        }
    }
}
