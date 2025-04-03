package com.argumentor.models

/**
 * Modelo de datos que representa un jugador en el sistema.
 * 
 * @property id Identificador único del jugador
 * @property nombre Nombre visible del jugador
 * @property listaTemas Lista mutable de temas con sus posturas
 */
data class Jugador(
    val id: String,
    val nombre: String,
    val listaTemas: MutableList<Tema>
) {
    /**
     * Asigna una postura/opinión a un tema específico.
     * 
     * @param temaNombre Nombre del tema a actualizar
     * @param opinion Nueva opinión a asignar (ej. "A favor", "En contra", "Indiferente")
     */
    fun asignarPostura(temaNombre: String, opinion: String) {
        listaTemas.find { it.nombre == temaNombre }?.opinionSeleccionada = opinion
    }
}