package com.streetfood.radar.data.repository

import com.streetfood.radar.data.local.dao.FoodSpotDao
import com.streetfood.radar.data.local.entity.FoodSpotEntity
import com.streetfood.radar.data.local.entity.toEntity
import com.streetfood.radar.data.model.FoodSpotDto
import com.streetfood.radar.data.remote.StreetFoodApiService
import com.streetfood.radar.domain.model.FoodSpot
import com.streetfood.radar.domain.model.SpotCategory
import com.streetfood.radar.domain.repository.FoodSpotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementación de FoodSpotRepository siguiendo el patrón Offline-First
 * y Single Source of Truth (SSOT).
 *
 * La UI observa la base de datos Room a través de 'foodSpots'.
 * 'refreshData()' contacta a Retrofit e inserta atómicamente en Room.
 */
class FoodSpotRepositoryImpl(
    private val foodSpotDao: FoodSpotDao,
    private val apiService: StreetFoodApiService
) : FoodSpotRepository {

    /**
     * SSOT: Mapea la lista de Entidades de Room a Modelos de Dominio reactivamente.
     */
    override val foodSpots: Flow<List<FoodSpot>> = foodSpotDao.getAllFoodSpots()
        .map { entities ->
            if (entities.isEmpty()) {
                // Si la BD aún está vacía (primer inicio sin red), proveemos el catálogo inicial chileno
                getMockFallbackSpots(null, null, null, null, null, null)
            } else {
                entities.map { it.toDomain() }
            }
        }

    /**
     * Sincronización con la API de Retrofit.
     * Si no hay internet (IOException), no rompe la app y retorna un Result descriptivo.
     */
    override suspend fun refreshData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getFoodSpots()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                if (dtos.isNotEmpty()) {
                    val entities = dtos.map { it.toEntity() }
                    foodSpotDao.clearAndInsert(entities)
                }
                Result.success(Unit)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            // Sin conexión o timeout: la UI sigue funcionando con la caché de Room
            Result.failure(Exception("Sin conexión a internet. Mostrando locales en caché local.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFoodSpots(
        category: String?,
        maxPrice: Int?,
        comuna: String?,
        region: String?,
        isHealthy: Boolean?,
        isStudentMode: Boolean?
    ): Result<List<FoodSpot>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getFoodSpots(
                category = category,
                maxPrice = maxPrice,
                comuna = comuna,
                region = region,
                isHealthy = isHealthy,
                isStudentMode = isStudentMode
            )

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = dtos.map { it.toEntity() }
                foodSpotDao.insertAll(entities)
                Result.success(entities.map { it.toDomain() })
            } else {
                Result.success(getMockFallbackSpots(category, maxPrice, comuna, region, isHealthy, isStudentMode))
            }
        } catch (e: Exception) {
            Result.success(getMockFallbackSpots(category, maxPrice, comuna, region, isHealthy, isStudentMode))
        }
    }

    private fun FoodSpotEntity.toDomain(): FoodSpot = FoodSpot(
        id = id,
        name = name,
        category = SpotCategory.fromString(category),
        region = region ?: "Metropolitana de Santiago",
        comuna = comuna ?: "Santiago",
        address = address ?: "Sin dirección",
        price = basePrice,
        quantity = quantity ?: "1 porción",
        isHealthy = isHealthyOption,
        rating = rating,
        latitude = latitude,
        longitude = longitude
    )

    private fun getMockFallbackSpots(
        category: String?,
        maxPrice: Int?,
        comuna: String?,
        region: String?,
        isHealthy: Boolean?,
        isStudentMode: Boolean?
    ): List<FoodSpot> {
        val allSpots = listOf(
            // Metropolitana
            FoodSpot(1, "Handroll El Samurai", SpotCategory.HANDROLL, "Metropolitana de Santiago", "Providencia", "Av. Providencia 1350", 3500, "1 Handroll + Bebida", false, 4.8, -33.4260, -70.6150),
            FoodSpot(2, "BioRoll Integral & Veggie", SpotCategory.HANDROLL, "Metropolitana de Santiago", "Providencia", "Av. Manuel Montt 220", 4200, "1 Roll Tofu Palta", true, 4.9, -33.4300, -70.6180),
            FoodSpot(3, "El Rey del Completo Gigante", SpotCategory.COMPLETO, "Metropolitana de Santiago", "Santiago", "Paseo Ahumada 250", 2500, "Completo Italiano 25cm", false, 4.7, -33.4410, -70.6515),
            FoodSpot(4, "Completo Veggie Bellas Artes", SpotCategory.COMPLETO, "Metropolitana de Santiago", "Santiago", "Monjitas 420", 3200, "NotDog con Mayo Vegana", true, 4.9, -33.4365, -70.6450),
            FoodSpot(5, "Sopaipillas El Tío Lucho", SpotCategory.SOPAIPILLA, "Metropolitana de Santiago", "Santiago", "Plaza de Armas", 500, "Sopaipilla con Pebre", false, 4.9, -33.4372, -70.6506),
            FoodSpot(6, "Sopaipillas Horneadas San Joaquín", SpotCategory.SOPAIPILLA, "Metropolitana de Santiago", "Macul", "Metro San Joaquín (PUC)", 1200, "Calabaza al Horno (Fit)", true, 4.9, -33.4970, -70.6140),

            // Valparaíso
            FoodSpot(7, "Completos Don Pepe", SpotCategory.COMPLETO, "Valparaíso", "Valparaíso", "Av. Pedro Montt 1820", 2000, "1 Completo + Bebida", false, 4.5, -33.0472, -71.6127),
            FoodSpot(8, "Valpo Handroll Veg & Fit", SpotCategory.HANDROLL, "Valparaíso", "Viña del Mar", "Plaza Vergara 120", 4200, "Handroll Palta & Tofu", true, 4.9, -33.0230, -71.5530),
            FoodSpot(9, "Completo Veggie Valpo", SpotCategory.COMPLETO, "Valparaíso", "Valparaíso", "Plaza Sotomayor 45", 2700, "NotDog Italiano", true, 4.8, -33.0390, -71.6285),

            // Biobío
            FoodSpot(10, "Sopaipillas La Picá de Conce", SpotCategory.SOPAIPILLA, "Biobío", "Concepción", "Barros Arana 450", 600, "Sopaipilla Casera", false, 4.8, -36.8270, -73.0503),
            FoodSpot(11, "Conce Veggie Rolls", SpotCategory.HANDROLL, "Biobío", "Concepción", "O'Higgins 740", 3600, "Roll Palta Champiñón", true, 4.9, -36.8280, -73.0490),
            FoodSpot(12, "Biobío Fit Completo UdeC", SpotCategory.COMPLETO, "Biobío", "Concepción", "Plaza Perú 120", 2500, "Completo NotMeat Estudiante Fit", true, 4.9, -36.8300, -73.0400),

            // Arica y Parinacota
            FoodSpot(13, "BioRoll Vegano Chinchorro", SpotCategory.HANDROLL, "Arica y Parinacota", "Arica", "Av. Las Dunas 450", 3800, "Tofu Marinado & Sésamo", true, 4.9, -18.4590, -70.3010),

            // Tarapacá
            FoodSpot(14, "Cavancha Green Handroll", SpotCategory.HANDROLL, "Tarapacá", "Iquique", "Av. Diego Portales 1120", 3900, "Roll Vegetal Champiñón", true, 4.8, -20.2220, -70.1420),

            // Antofagasta
            FoodSpot(15, "Antofa Veggie Handroll", SpotCategory.HANDROLL, "Antofagasta", "Antofagasta", "Av. Angamos 890", 4100, "Roll Tofu Crocante", true, 4.8, -23.6620, -70.4020),

            // Coquimbo
            FoodSpot(16, "La Serena Veggie Roll", SpotCategory.HANDROLL, "Coquimbo", "La Serena", "Av. Balmaceda 1540", 3900, "Handroll Palta & Espárrago", true, 4.9, -29.9120, -71.2510),

            // La Araucanía
            FoodSpot(17, "Araucanía Handroll Fit", SpotCategory.HANDROLL, "La Araucanía", "Temuco", "Av. Alemania 0890", 4400, "Quinoa & Verduras Grill", true, 4.8, -38.7359, -72.5904),

            // Los Ríos
            FoodSpot(18, "Valdivia Veggie Rolls", SpotCategory.HANDROLL, "Los Ríos", "Valdivia", "Isla Teja 075", 4100, "Roll Quinoa & Ostra", true, 4.9, -39.8080, -73.2530),

            // Los Lagos
            FoodSpot(19, "Puerto Montt Veggie Rolls", SpotCategory.HANDROLL, "Los Lagos", "Puerto Montt", "Guillermo Gallardo 290", 4200, "Roll Tofu Ahumado Palta", true, 4.8, -41.4720, -72.9380),

            // Aysén
            FoodSpot(20, "Coyhaique Veggie Handroll", SpotCategory.HANDROLL, "Aysén", "Coyhaique", "Calle Dussen 310", 4600, "Roll Hongos Morillas & Palta", true, 4.9, -45.5725, -72.0660),

            // Magallanes
            FoodSpot(21, "Punta Arenas Green Roll", SpotCategory.HANDROLL, "Magallanes", "Punta Arenas", "Calle José Nogueira 1180", 4800, "Roll Tofu Austral & Palta", true, 4.9, -53.1645, -70.9120)
        )

        return allSpots.filter { spot ->
            val matchCategory = category == null || category == "ALL" || spot.category.name.equals(category, ignoreCase = true)
            val matchPrice = maxPrice == null || spot.price <= maxPrice
            val matchHealthy = isHealthy == null || !isHealthy || spot.isHealthy
            val matchStudent = isStudentMode == null || !isStudentMode || spot.price <= 3000
            val matchComuna = comuna == null || comuna.isEmpty() || comuna.equals("all", ignoreCase = true) || spot.comuna.equals(comuna, ignoreCase = true)
            val matchRegion = region == null || region.isEmpty() || region.equals("all", ignoreCase = true) || spot.region.equals(region, ignoreCase = true)

            matchCategory && matchPrice && matchHealthy && matchStudent && matchComuna && matchRegion
        }
    }
}
