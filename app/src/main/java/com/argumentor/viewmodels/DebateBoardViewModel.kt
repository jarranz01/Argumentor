package com.argumentor.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.argumentor.models.Debate
import java.util.*

/**
 * ViewModel que administra la lista de debates.
 *
 * Proporciona funcionalidades para agregar debates y carga algunos debates de ejemplo al inicializarse.
 */
class DebateBoardViewModel : ViewModel() {

    /**
     * Lista de debates encapsulada. Se expone como LiveData para observar los cambios.
     */
    private val _debates = MutableLiveData<List<Debate>>(emptyList())
    
    /**
     * Lista de debates observable.
     */
    val debates: LiveData<List<Debate>> = _debates

    init {
        // Cargamos algunos debates de ejemplo para que no esté vacía de inicio
        addSampleDebates()
    }

    /**
     * Agrega un nuevo debate a la lista.
     *
     * @param title Título del debate.
     * @param description Descripción del debate.
     * @param author Autor que crea el debate.
     */
    fun addDebate(title: String, description: String, author: String) {
        val newDebate = Debate(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            author = author
        )

        _debates.value = _debates.value?.plus(newDebate) ?: listOf(newDebate)
    }

    /**
     * Agrega debates de ejemplo a la lista.
     *
     * Este método se utiliza para inicializar la lista de debates con datos de muestra.
     * 
     * No usamos el fichero strings ya que vamos a quitarlos en un futuro (es algo estético por ahora)
     */
    private fun addSampleDebates() {
        val sampleDebates = listOf(
            Debate(
                id = "1",
                title = "¿Es ético el uso de la IA en la toma de decisiones médicas?",
                description = "Debate sobre los aspectos éticos del uso de inteligencia artificial para diagnosticar y recomendar tratamientos médicos.",
                author = "Gonzalo"
            ),
            Debate(
                id = "2",
                title = "¿Es necesaria la energía nuclear para combatir el cambio climático?",
                description = "Análisis de las ventajas y desventajas de la energía nuclear como alternativa a los combustibles fósiles.",
                author = "Joseba"
            )
        )

        _debates.value = sampleDebates
    }
}