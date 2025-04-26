package com.argumentor.models

/**
 * Representa una entrada (intervención) dentro de un debate.
 *
 * @property id Identificador único de la entrada
 * @property debateId ID del debate al que pertenece esta entrada
 * @property userId ID del usuario que realiza la intervención
 * @property stage Etapa del debate (INTRODUCCION, REFUTACION1, REFUTACION2, CONCLUSION)
 * @property content Contenido textual de la intervención
 * @property position Postura del usuario (A_FAVOR, EN_CONTRA)
 * @property timestamp Momento de creación
 */
data class DebateEntry(
    val id: String,
    val debateId: String,
    val userId: String,
    val stage: DebateStage,
    val content: String,
    val position: DebatePosition,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enumera las posibles etapas de un debate.
 */
enum class DebateStage {
    INTRODUCCION,
    REFUTACION1,
    REFUTACION2,
    CONCLUSION
}

/**
 * Enumera las posibles posturas en un debate.
 */
enum class DebatePosition {
    A_FAVOR,
    EN_CONTRA
}