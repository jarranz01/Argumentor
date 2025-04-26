package com.argumentor.models

/**
 * Modelo de datos que representa un debate en la aplicación.
 * 
 * @property id Identificador único del debate (vacío si es nuevo)
 * @property title Título del debate
 * @property description Descripción detallada del debate
 * @property author Nombre o ID del usuario que creó el debate
 * @property currentStage Etapa actual del debate
 * @property participantFavor ID del usuario que defiende la postura a favor
 * @property participantContra ID del usuario que defiende la postura en contra
 * @property status Estado del debate (pendiente, activo, terminado)
 * @property timestamp Fecha de creación en milisegundos (por defecto: tiempo actual)
 */
data class Debate(
    val id: String = "",
    val title: String,
    val description: String,
    val author: String,
    val currentStage: DebateStage = DebateStage.INTRODUCCION,
    val participantFavor: String = "",
    val participantContra: String = "",
    val status: DebateStatus = DebateStatus.PENDIENTE,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enumera los posibles estados de un debate.
 */
enum class DebateStatus {
    PENDIENTE,   // Esperando participantes
    ACTIVO,      // En curso
    TERMINADO    // Finalizado
}