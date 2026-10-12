package com.streetfood.radar.data.model

import com.google.gson.annotations.SerializedName

/**
 * DTO (Data Transfer Object) recibido desde la API REST de Django.
 * Mapea directamente el contrato JSON generado por Django REST Framework.
 */
data class FoodSpotDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String, // "HANDROLL", "COMPLETO", "SOPAIPILLA"

    @SerializedName("region")
    val region: String? = null,

    @SerializedName("comuna")
    val comuna: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("base_price")
    val basePrice: Int,

    @SerializedName("quantity")
    val quantity: String? = null,

    @SerializedName("is_healthy_option")
    val isHealthyOption: Boolean = false,

    @SerializedName("rating")
    val rating: Double = 5.0,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)
