package com.argumentor.database.repositories

import com.argumentor.database.dao.ArgumentDao
import com.argumentor.database.dao.DebateDao
import com.argumentor.database.dao.UserDao
import com.argumentor.database.entities.ArgumentEntity
import com.argumentor.database.entities.DebateEntity
import com.argumentor.database.utils.DebateJsonFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Repositorio que proporciona una API para gestionar debates y sus argumentos.
 */
class DebateRepository(
    private val debateDao: DebateDao,
    private val argumentDao: ArgumentDao,
    private val userDao: UserDao
) {

    private val jsonFormatter = DebateJsonFormatter()

    /**
     * Obtiene todos los debates.
     */
    fun getAllDebates(): Flow<List<DebateEntity>> = debateDao.getAllDebates()

    /**
     * Obtiene un debate por su ID.
     */
    suspend fun getDebateById(debateId: String): DebateEntity? = debateDao.getDebateById(debateId)

    /**
     * Obtiene los debates creados por un autor específico.
     */
    fun getDebatesByAuthor(authorUserId: String): Flow<List<DebateEntity>> = 
        debateDao.getDebatesByAuthor(authorUserId)

    /**
     * Obtiene los debates en los que un usuario participa.
     */
    fun getDebatesUserParticipatesIn(userId: String): Flow<List<DebateEntity>> = 
        debateDao.getDebatesUserParticipatesIn(userId)

    /**
     * Crea un nuevo debate.
     * @return El ID del debate creado
     */
    suspend fun createDebate(
        title: String, 
        description: String, 
        authorUserId: String?,
        participantFavorUserId: String,
        participantContraUserId: String,
        category: String? = null
    ): String {
        val debateId = generateDebateId()
        
        val debate = DebateEntity(
            debateId = debateId,
            title = title,
            description = description,
            authorUserId = authorUserId,
            participantFavorUserId = participantFavorUserId,
            participantContraUserId = participantContraUserId,
            status = "PENDIENTE", // Estado inicial del debate
            category = category
        )
        
        debateDao.insertDebate(debate)
        return debateId
    }

    /**
     * Actualiza un debate existente.
     */
    suspend fun updateDebate(debate: DebateEntity) = debateDao.updateDebate(debate)

    /**
     * Elimina un debate por su ID.
     */
    suspend fun deleteDebate(debateId: String) {
        // Eliminar todos los argumentos asociados
        argumentDao.deleteAllArgumentsForDebate(debateId)
        // Eliminar el debate
        debateDao.deleteDebateById(debateId)
    }

    /**
     * Añade un nuevo argumento a un debate.
     * @return El ID del argumento creado
     */
    suspend fun addArgument(
        debateId: String,
        userId: String,
        stage: String,
        position: String,
        content: String
    ): Long {
        val argument = ArgumentEntity(
            debateId = debateId,
            userId = userId,
            stage = stage,
            position = position,
            content = content
        )
        
        return argumentDao.insertArgument(argument)
    }

    /**
     * Obtiene todos los argumentos para un debate.
     */
    fun getArgumentsForDebate(debateId: String): Flow<List<ArgumentEntity>> = 
        argumentDao.getArgumentsForDebate(debateId)

    /**
     * Obtiene todos los argumentos para una fase específica de un debate.
     */
    fun getArgumentsForDebatePhase(debateId: String, stage: String): Flow<List<ArgumentEntity>> = 
        argumentDao.getArgumentsForDebatePhase(debateId, stage)

    /**
     * Obtiene un argumento específico por fase y posición en un debate.
     */
    suspend fun getArgumentForDebatePhaseAndPosition(
        debateId: String,
        stage: String,
        position: String
    ): ArgumentEntity? = 
        argumentDao.getArgumentForDebatePhaseAndPosition(debateId, stage, position)

    /**
     * Obtiene el último argumento añadido a un debate.
     */
    suspend fun getLastArgumentForDebate(debateId: String): ArgumentEntity? = 
        argumentDao.getLastArgumentForDebate(debateId)

    /**
     * Comprueba si una fase del debate está completa (con argumentos para ambas posiciones).
     */
    suspend fun isDebatePhaseComplete(debateId: String, stage: String): Boolean {
        val favorArgument = argumentDao.getArgumentForDebatePhaseAndPosition(
            debateId, stage, "A_FAVOR"
        )
        val contraArgument = argumentDao.getArgumentForDebatePhaseAndPosition(
            debateId, stage, "EN_CONTRA"
        )
        
        return favorArgument != null && contraArgument != null
    }

    /**
     * Avanza un debate a la siguiente fase si se completa la fase actual.
     * @return true si se avanzó a la siguiente fase, false en caso contrario
     */
    suspend fun advanceDebateStageIfComplete(debateId: String, currentStage: String): Boolean {
        if (!isDebatePhaseComplete(debateId, currentStage)) {
            return false
        }
        
        val debate = debateDao.getDebateById(debateId) ?: return false
        
        val nextStage = getNextStage(currentStage)
        if (nextStage != null) {
            debateDao.updateDebate(debate.copy(status = nextStage))
            return true
        }
        
        return false
    }

    /**
     * Genera una representación JSON del debate en el formato requerido para el análisis del ganador.
     *
     * @param debateId ID del debate a formatear
     * @return String con la representación JSON del debate o null si hay error
     */
    suspend fun debateToJson(debateId: String): String? {
        // Obtener el debate
        val debate = getDebateById(debateId) ?: return null
        
        // Obtener todos los argumentos del debate
        val arguments = argumentDao.getArgumentsForDebate(debateId).first()
        
        // Obtener información de los usuarios participantes
        val favorUser = debate.participantFavorUserId?.let { 
            userDao.getUserById(it) 
        }
        
        val contraUser = debate.participantContraUserId?.let { 
            userDao.getUserById(it) 
        }
        
        // Utilizar el formateador para generar el JSON
        return jsonFormatter.formatDebateToJson(debate, arguments, favorUser, contraUser)
    }

    /**
     * Determina la siguiente fase de un debate.
     */
    private fun getNextStage(currentStage: String): String? {
        return when (currentStage) {
            "INTRODUCCION" -> "REFUTACION1"
            "REFUTACION1" -> "REFUTACION2"
            "REFUTACION2" -> "CONCLUSION"
            "CONCLUSION" -> "TERMINADO"
            else -> null
        }
    }

    /**
     * Genera un ID único para un debate.
     */
    private fun generateDebateId(): String {
        return "debate_${UUID.randomUUID().toString().substring(0, 8)}"
    }
}