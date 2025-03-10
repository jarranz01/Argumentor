package com.argumentor.models

class Jugador(
    id: String,
    nombre: String,
    val listaTemas: MutableList<Tema> = mutableListOf()
) : Usuario(id, nombre) {

    fun asignarPostura(tema: String, opinion: String) {
        listaTemas.find { it.nombre == tema }?.opinionSeleccionada = opinion
    }
}
