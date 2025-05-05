package com.argumentor.database.dao

import androidx.room.*
import com.argumentor.database.entities.TopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar los datos de temas en la base de datos.
 */
@Dao
interface TopicDao {
    /**
     * Inserta un nuevo tema ignorando conflictos.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopic(topic: TopicEntity)

    /**
     * Inserta una lista de temas ignorando conflictos.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    /**
     * Obtiene un tema por su nombre.
     */
    @Query("SELECT * FROM topics WHERE topicName = :topicName")
    suspend fun getTopicByName(topicName: String): TopicEntity?

    /**
     * Obtiene todos los temas ordenados alfabéticamente por nombre.
     */
    @Query("SELECT * FROM topics ORDER BY topicName ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>
}