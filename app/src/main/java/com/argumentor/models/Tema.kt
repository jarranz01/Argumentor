package com.argumentor.models

data class Tema(
    val nombre: String,
    val descripcion: String = "",
    var opinionSeleccionada: String = "Indiferente"
)