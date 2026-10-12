package com.streetfood.radar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.streetfood.radar.data.local.entity.FoodSpotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de locales de comida en Room.
 */
@Dao
interface FoodSpotDao {

    /**
     * SSOT: Retorna un Flow reactivo. Room emite una nueva lista automáticamente
     * cada vez que los datos en la tabla cambien.
     */
    @Query("SELECT * FROM food_spots ORDER BY rating DESC, name ASC")
    fun getAllFoodSpots(): Flow<List<FoodSpotEntity>>

    /**
     * Inserta o reemplaza una lista de locales.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(spots: List<FoodSpotEntity>)

    /**
     * Limpia la tabla por completo.
     */
    @Query("DELETE FROM food_spots")
    suspend fun clearAll()

    /**
     * Reemplaza atómicamente toda la caché local:
     * borra los registros obsoletos e inserta la nueva lista en una sola transacción.
     */
    @Transaction
    suspend fun clearAndInsert(spots: List<FoodSpotEntity>) {
        clearAll()
        insertAll(spots)
    }
}
