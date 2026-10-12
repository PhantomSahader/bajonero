package com.streetfood.radar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streetfood.radar.data.model.FoodSpotDto

/**
 * Entidad SQLite para persistencia local en Room.
 * Representa la Fuente Única de Verdad (Single Source of Truth) para la UI.
 */
@Entity(tableName = "food_spots")
data class FoodSpotEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val category: String,
    val region: String?,
    val comuna: String?,
    val address: String?,
    val basePrice: Int,
    val quantity: String?,
    val isHealthyOption: Boolean,
    val rating: Double,
    val latitude: Double,
    val longitude: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Función de extensión Mapper: Transforma el DTO de red a Entidad Room.
 */
fun FoodSpotDto.toEntity(): FoodSpotEntity = FoodSpotEntity(
    id = id,
    name = name,
    category = category,
    region = region,
    comuna = comuna,
    address = address,
    basePrice = basePrice,
    quantity = quantity,
    isHealthyOption = isHealthyOption,
    rating = rating,
    latitude = latitude,
    longitude = longitude
)
