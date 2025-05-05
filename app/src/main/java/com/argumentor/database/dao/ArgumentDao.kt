package com.argumentor.database.dao

import androidx.room.*
import com.argumentor.database.entities.ArgumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar los datos de argumentos en la base de datos.
 */
@Dao
interface ArgumentDao {
    /**
     * Inserta un nuevo argumento o reemplaza uno existente con el mismo ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArgument(argument: ArgumentEntity): Long

    /**
     * Obtiene todos los argumentos de la base de datos.
     */
    @Query("SELECT * FROM arguments ORDER BY timestamp ASC")
    fun getAllArguments(): Flow<List<ArgumentEntity>>

    /**
     * Obtiene todos los argumentos para un debate específico, ordenados por timestamp.
     */
    @Query("SELECT * FROM arguments WHERE debateId = :debateId ORDER BY timestamp ASC")
    fun getArgumentsForDebate(debateId: String): Flow<List<ArgumentEntity>>

    /**
     * Obtiene todos los argumentos para una fase específica de un debate.
     */
    @Query("SELECT * FROM arguments WHERE debateId = :debateId AND stage = :stage ORDER BY timestamp ASC")
    fun getArgumentsForDebatePhase(debateId: String, stage: String): Flow<List<ArgumentEntity>>

    /**
     * Obtiene un argumento específico por fase y posición en un debate.
     */
    @Query("SELECT * FROM arguments WHERE debateId = :debateId AND stage = :stage AND position = :position LIMIT 1")
    suspend fun getArgumentForDebatePhaseAndPosition(debateId: String, stage: String, position: String): ArgumentEntity?

    /**
     * Obtiene el último argumento añadido para un debate específico.
     */
    @Query("SELECT * FROM arguments WHERE debateId = :debateId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastArgumentForDebate(debateId: String): ArgumentEntity?

    /**
     * Elimina un argumento por su ID.
     */
    @Delete
    suspend fun deleteArgument(argument: ArgumentEntity)

    /**
     * Elimina todos los argumentos de un debate.
     */
    @Query("DELETE FROM arguments WHERE debateId = :debateId")
    suspend fun deleteAllArgumentsForDebate(debateId: String)
}