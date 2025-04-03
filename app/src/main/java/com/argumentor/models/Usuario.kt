package com.argumentor.models

/**
 * Clase base que representa un usuario del sistema.
 * 
 * @property id Identificador único del usuario
 * @property nombre Nombre visible del usuario
 */
open class Usuario(
    val id: String,
    val nombre: String
)