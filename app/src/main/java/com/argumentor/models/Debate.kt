package com.argumentor.models

/**
 * Modelo de datos que representa un debate en la aplicación.
 * 
 * @property id Identificador único del debate (vacío si es nuevo)
 * @property title Título del debate
 * @property description Descripción detallada del debate
 * @property author Nombre o ID del usuario que creó el debate
 * @property timestamp Fecha de creación en milisegundos (por defecto: tiempo actual)
 */
data class Debate(
    val id: String = "",
    val title: String,
    val description: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis()
)