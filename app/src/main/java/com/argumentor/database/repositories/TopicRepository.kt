package com.argumentor.database.repositories

import com.argumentor.database.dao.TopicDao
import com.argumentor.database.entities.TopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que proporciona una API limpia para acceder a los datos de temas.
 * 
 * Esta clase abstrae el origen de datos (Room) y expone métodos de alto nivel
 * para interactuar con los datos.
 */
class TopicRepository(private val topicDao: TopicDao) {
    
    /**
     * Obtiene todos los temas disponibles como un Flow.
     */
    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()
    
    /**
     * Obtiene un tema por su nombre.
     */
    suspend fun getTopicByName(topicName: String): TopicEntity? = topicDao.getTopicByName(topicName)
    
    /**
     * Inserta un nuevo tema en la base de datos.
     */
    suspend fun insertTopic(topic: TopicEntity) = topicDao.insertTopic(topic)
    
    /**
     * Inserta una lista de temas en la base de datos.
     */
    suspend fun insertTopics(topics: List<TopicEntity>) = topicDao.insertTopics(topics)
}