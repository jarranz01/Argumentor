package com.argumentor.database.repositories

import com.argumentor.database.dao.UserStanceDao
import com.argumentor.database.entities.UserStanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que proporciona una API para gestionar las posturas de los usuarios sobre los temas.
 */
class UserStanceRepository(private val userStanceDao: UserStanceDao) {

    /**
     * Obtiene todas las posturas de un usuario específico.
     */
    fun getAllStancesForUser(userId: String): Flow<List<UserStanceEntity>> {
        return userStanceDao.getAllStancesForUser(userId)
    }

    /**
     * Obtiene la postura de un usuario sobre un tema específico.
     */
    suspend fun getUserStanceOnTopic(userId: String, topicName: String): UserStanceEntity? {
        return userStanceDao.getStance(userId, topicName)
    }

    /**
     * Establece o actualiza la postura de un usuario sobre un tema.
     */
    suspend fun setUserStance(userId: String, topicName: String, stance: String) {
        val userStance = UserStanceEntity(
            userId = userId,
            topicName = topicName,
            stance = stance,
            updatedAt = System.currentTimeMillis()
        )
        userStanceDao.insertOrUpdateStance(userStance)
    }

    /**
     * Guarda múltiples posturas de usuario a la vez.
     */
    suspend fun saveUserStances(stances: List<UserStanceEntity>) {
        userStanceDao.insertOrUpdateStances(stances)
    }

    /**
     * Obtiene todas las posturas sobre un tema específico.
     */
    suspend fun getAllStancesOnTopic(topicName: String): List<UserStanceEntity> {
        return userStanceDao.getAllStancesForTopic(topicName)
    }
}