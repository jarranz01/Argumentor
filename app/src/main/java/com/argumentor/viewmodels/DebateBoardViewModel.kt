package com.argumentor.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.argumentor.models.Debate
import java.util.*

class DebateBoardViewModel : ViewModel() {

    private val _debates = MutableLiveData<List<Debate>>(emptyList())
    val debates: LiveData<List<Debate>> = _debates

    init {
        // Cargamos algunos debates de ejemplo
        addSampleDebates()
    }

    fun addDebate(title: String, description: String, author: String) {
        val newDebate = Debate(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            author = author
        )

        _debates.value = _debates.value?.plus(newDebate) ?: listOf(newDebate)
    }

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