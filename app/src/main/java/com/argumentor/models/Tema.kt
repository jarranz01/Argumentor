package com.argumentor.models

/**
 * Modelo de datos que representa un tema de debate.
 * 
 * @property nombre Nombre identificativo del tema
 * @property descripcion Explicación detallada del tema (opcional)
 * @property opinionSeleccionada Postura actual sobre el tema (por defecto: "Indiferente")
 */
data class Tema(
    val nombre: String,
    val descripcion: String = "",
    var opinionSeleccionada: String = "Indiferente"
)