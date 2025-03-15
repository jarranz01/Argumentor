package com.argumentor.models

data class Jugador(
    val id: String,
    val nombre: String,
    val listaTemas: MutableList<Tema>
) {
    fun asignarPostura(temaNombre: String, opinion: String) {
        listaTemas.find { it.nombre == temaNombre }?.opinionSeleccionada = opinion
    }
}