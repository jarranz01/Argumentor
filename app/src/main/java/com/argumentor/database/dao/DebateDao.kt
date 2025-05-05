package com.argumentor.database.dao

import androidx.room.*
import com.argumentor.database.entities.DebateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar los datos de debates en la base de datos.
 */
@Dao
interface DebateDao {
    /**
     * Inserta un nuevo debate o reemplaza uno existente con el mismo ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebate(debate: DebateEntity)

    /**
     * Obtiene un debate por su ID.
     */
    @Query("SELECT * FROM debates WHERE debateId = :debateId")
    suspend fun getDebateById(debateId: String): DebateEntity?

    /**
     * Obtiene todos los debates ordenados por fecha de creación (más recientes primero).
     */
    @Query("SELECT * FROM debates ORDER BY timestamp DESC")
    fun getAllDebates(): Flow<List<DebateEntity>>

    /**
     * Obtiene todos los debates creados por un autor específico.
     */
    @Query("SELECT * FROM debates WHERE authorUserId = :authorUserId ORDER BY timestamp DESC")
    fun getDebatesByAuthor(authorUserId: String): Flow<List<DebateEntity>>

    /**
     * Obtiene todos los debates en los que un usuario participa (a favor o en contra).
     */
    @Query("SELECT * FROM debates WHERE participantFavorUserId = :userId OR participantContraUserId = :userId ORDER BY timestamp DESC")
    fun getDebatesUserParticipatesIn(userId: String): Flow<List<DebateEntity>>

    /**
     * Actualiza los datos de un debate existente.
     */
    @Update
    suspend fun updateDebate(debate: DebateEntity)

    /**
     * Elimina un debate por su ID.
     */
    @Query("DELETE FROM debates WHERE debateId = :debateId")
    suspend fun deleteDebateById(debateId: String)
}