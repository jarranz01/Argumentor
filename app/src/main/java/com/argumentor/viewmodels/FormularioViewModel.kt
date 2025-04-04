package com.argumentor.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.argumentor.R
import com.argumentor.models.Jugador
import com.argumentor.models.Tema

/**
 * ViewModel para administrar la información del formulario.
 *
 * Este ViewModel mantiene la información de un Jugador y la lista de Temas
 * asociadas al mismo. Además, permite asignar una postura para cada tema.
 *
 * @param application Contexto de la aplicación utilizado para acceder a los recursos.
 */
class FormularioViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    /**
     * LiveData encapsulado que contiene la información del jugador.
     */
    private val _jugador = MutableLiveData<Jugador>()

    /**
     * Jugador observable correspondiente a la información del formulario.
     */
    val jugador: LiveData<Jugador> get() = _jugador

    init {
        // Inicializar el jugador con una lista de temas predefinidos.
        _jugador.value = Jugador(
            id = "1",
            nombre = context.getString(R.string.player_default_name),
            listaTemas = mutableListOf(
                Tema(context.getString(R.string.climate_change), context.getString(R.string.climate_change_description)),
                Tema(context.getString(R.string.nuclear_energy), context.getString(R.string.nuclear_energy_description)),
                Tema(context.getString(R.string.social_media), context.getString(R.string.social_media_description)),
                Tema(context.getString(R.string.online_education), context.getString(R.string.online_education_description)),
                Tema(context.getString(R.string.artificial_intelligence)),
                Tema(context.getString(R.string.abortion)),
                Tema(context.getString(R.string.bullfighting)),
                Tema(context.getString(R.string.film_subsidies)),
                Tema(context.getString(R.string.open_borders)),
                Tema(context.getString(R.string.freedom_of_speech)),
                Tema(context.getString(R.string.marijuana))
            )
        )
    }

    /**
     * Asigna una postura a un tema específico.
     *
     * @param tema El nombre del tema al que se le asignará la postura.
     * @param opinion La opinión seleccionada para el tema.
     */
    fun asignarPostura(tema: String, opinion: String) {
        _jugador.value?.asignarPostura(tema, opinion)
    }
}