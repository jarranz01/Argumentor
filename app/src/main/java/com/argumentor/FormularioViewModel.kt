package com.argumentor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.argumentor.models.Jugador
import com.argumentor.models.Tema
import android.app.Application

import androidx.lifecycle.AndroidViewModel

class FormularioViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    // LiveData para el jugador
    private val _jugador = MutableLiveData<Jugador>()
    val jugador: LiveData<Jugador> get() = _jugador

    init {
        // Inicializar el jugador con temas
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

    // Método para asignar una postura a un tema
    fun asignarPostura(tema: String, opinion: String) {
        _jugador.value?.asignarPostura(tema, opinion)
    }
}
