package com.argumentor.database.dao

import androidx.room.*
import com.argumentor.database.entities.UserStanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar las posturas de los usuarios sobre temas.
 */
@Dao
interface UserStanceDao {
    /**
     * Inserta o actualiza la postura de un usuario sobre un tema.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStance(stance: UserStanceEntity)

    /**
     * Inserta o actualiza una lista de posturas de usuarios.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStances(stances: List<UserStanceEntity>)

    /**
     * Obtiene la postura de un usuario sobre un tema específico.
     */
    @Query("SELECT * FROM user_stances WHERE userId = :userId AND topicName = :topicName")
    suspend fun getStance(userId: String, topicName: String): UserStanceEntity?

    /**
     * Obtiene todas las posturas de un usuario.
     */
    @Query("SELECT * FROM user_stances WHERE userId = :userId")
    fun getAllStancesForUser(userId: String): Flow<List<UserStanceEntity>>

    /**
     * Obtiene todas las posturas sobre un tema específico.
     */
    @Query("SELECT * FROM user_stances WHERE topicName = :topicName")
    suspend fun getAllStancesForTopic(topicName: String): List<UserStanceEntity>

    /**
     * Obtiene todas las posturas almacenadas en la base de datos.
     */
    @Query("SELECT * FROM user_stances")
    fun getAllStances(): Flow<List<UserStanceEntity>>
}