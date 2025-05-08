package com.argumentor.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.argumentor.R
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.entities.TopicEntity
import com.argumentor.database.entities.UserStanceEntity
import com.argumentor.database.mappers.DataMappers
import com.argumentor.models.Jugador
import com.argumentor.models.Tema
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val repositoryProvider = RepositoryProvider.getInstance(context)
    private val topicRepository = repositoryProvider.topicRepository
    private val userStanceRepository = repositoryProvider.userStanceRepository
    private val sessionManager = com.argumentor.SessionManager(context)

    /**
     * LiveData encapsulado que contiene la información del jugador.
     */
    private val _jugador = MutableLiveData<Jugador>()

    /**
     * Jugador observable correspondiente a la información del formulario.
     */
    val jugador: LiveData<Jugador> get() = _jugador
    
    /**
     * LiveData para mostrar la lista filtrada de temas
     */
    private val _temasFiltrados = MutableLiveData<List<Tema>>()
    
    /**
     * Temas filtrados observables
     */
    val temasFiltrados: LiveData<List<Tema>> get() = _temasFiltrados
    
    /**
     * Término de búsqueda actual
     */
    private var searchQuery: String = ""
    
    /**
     * ID del usuario actual
     */
    private var currentUserId: String? = null

    init {
        // Obtener el ID de usuario de la sesión activa
        currentUserId = sessionManager.getUserId()
        
        // Inicializar un jugador vacío
        _jugador.value = Jugador(
            id = currentUserId ?: "guest",
            nombre = sessionManager.getUsername() ?: context.getString(R.string.player_default_name),
            listaTemas = mutableListOf()
        )
        
        // Cargar temas y posturas desde la base de datos
        loadTopicsAndStances()
    }
    
    /**
     * Carga los temas y las posturas del usuario desde la base de datos.
     */
    private fun loadTopicsAndStances() {
        viewModelScope.launch {
            try {
                // Obtener todos los temas de la base de datos
                val topicEntities = topicRepository.getAllTopics().first()
                
                // Si hay un usuario con sesión, obtener sus posturas
                val userStances = if (currentUserId != null) {
                    userStanceRepository.getAllStancesForUser(currentUserId!!).first()
                } else {
                    emptyList()
                }
                
                // Convertir entidades a modelos de dominio con descripciones localizadas
                val temas = topicEntities.map { topicEntity ->
                    val stance = userStances.firstOrNull { it.topicName == topicEntity.topicName }
                    
                    // Resolver el nombre localizado del tema
                    val nombreTema = resolveTopicName(topicEntity.topicName)
                    
                    // Resolver la descripción localizada o usar la de la base de datos como respaldo
                    var descripcion = resolveTopicDescription(topicEntity.topicName)
                    if (descripcion.isEmpty()) {
                        descripcion = topicEntity.description
                    }
                    
                    Tema(
                        nombre = nombreTema,
                        descripcion = descripcion,
                        opinionSeleccionada = stance?.stance ?: ""
                    )
                }
                
                // Actualizar el jugador con los temas cargados
                _jugador.value = _jugador.value?.copy(listaTemas = temas.toMutableList())
                
                // Actualizar la lista filtrada
                _temasFiltrados.value = temas
                
                Timber.d("Cargados ${temas.size} temas de la base de datos")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar temas y posturas")
            }
        }
    }
    
    /**
     * Resuelve el nombre del tema a partir de una referencia de recurso o ID.
     */
    private fun resolveTopicName(topicName: String): String {
        return try {
            // Mapeo directo de identificadores internos a recursos de strings
            val mappedResourceName = when (topicName) {
                "climate_change" -> R.string.climate_change
                "nuclear_energy" -> R.string.nuclear_energy
                "social_media" -> R.string.social_media
                "online_education" -> R.string.online_education
                "artificial_intelligence" -> R.string.artificial_intelligence
                "abortion" -> R.string.abortion
                "bullfighting" -> R.string.bullfighting
                "film_subsidies" -> R.string.film_subsidies
                "open_borders" -> R.string.open_borders
                "freedom_of_speech" -> R.string.freedom_of_speech
                "marijuana" -> R.string.marijuana
                else -> 0 // No hay mapeo directo
            }

            if (mappedResourceName != 0) {
                // Si tenemos un mapeo directo, usamos el recurso
                context.getString(mappedResourceName)
            } else {
                // Verificar si el topicName podría ser directamente un identificador de recurso
                val resourceId = context.resources.getIdentifier(topicName, "string", context.packageName)
                if (resourceId != 0) {
                    context.getString(resourceId)
                } else {
                    // Si todo falla, devolvemos el nombre original posiblemente formateado
                    topicName.replace("_", " ").capitalize()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al resolver el nombre del tema: $topicName")
            topicName // Si falla, devolvemos el nombre original
        }
    }

    /**
     * Función auxiliar para poner en mayúscula la primera letra
     */
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this[0].uppercase() + this.substring(1).lowercase()
        } else {
            this
        }
    }

    /**
     * Resuelve la descripción del tema a partir del identificador.
     */
    private fun resolveTopicDescription(topicName: String): String {
        return try {
            // Mapeo directo de identificadores internos a recursos de strings de descripción
            val descriptionResourceId = when (topicName) {
                "climate_change" -> R.string.climate_change_description
                "nuclear_energy" -> R.string.nuclear_energy_description
                "social_media" -> R.string.social_media_description
                "online_education" -> R.string.online_education_description
                "artificial_intelligence" -> R.string.artificial_intelligence_description
                "abortion" -> R.string.abortion_description
                "bullfighting" -> R.string.bullfighting_description
                "film_subsidies" -> R.string.film_subsidies_description
                "open_borders" -> R.string.open_borders_description
                "freedom_of_speech" -> R.string.freedom_of_speech_description
                "marijuana" -> R.string.marijuana_description
                else -> 0 // No hay mapeo directo
            }

            if (descriptionResourceId != 0) {
                // Si tenemos un mapeo directo, usamos el recurso
                context.getString(descriptionResourceId)
            } else {
                // Intentar encontrar una descripción con el patrón <nombre>_description
                val descriptionResId = context.resources.getIdentifier(
                    "${topicName}_description", 
                    "string", 
                    context.packageName
                )
                if (descriptionResId != 0) {
                    context.getString(descriptionResId)
                } else {
                    // Si no hay descripción localizada, devolvemos una cadena vacía
                    // La descripción de la base de datos se cargará en loadTopicsAndStances
                    ""
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al resolver la descripción del tema: $topicName")
            "" // Si falla, devolvemos cadena vacía
        }
    }

    /**
     * Asigna una postura a un tema específico.
     *
     * @param temaMostrado El nombre mostrado del tema al que se le asignará la postura.
     * @param opinion La opinión seleccionada para el tema.
     */
    fun asignarPostura(temaMostrado: String, opinion: String) {
        // Actualizar el modelo local
        _jugador.value?.asignarPostura(temaMostrado, opinion)
        
        // Actualizar la base de datos si hay un usuario con sesión
        currentUserId?.let { userId ->
            viewModelScope.launch {
                try {
                    // Encontrar el tema en la lista local que coincida con el nombre mostrado
                    val temaActual = _jugador.value?.listaTemas?.find { it.nombre == temaMostrado }
                    
                    // Obtener todos los temas de la base de datos
                    val topicEntities = topicRepository.getAllTopics().first()
                    
                    // Encontrar el identificador interno del tema
                    val internalTopicName = topicEntities.find { entity -> 
                        val nombreResuelto = resolveTopicName(entity.topicName)
                        nombreResuelto == temaMostrado
                    }?.topicName
                    
                    if (internalTopicName != null) {
                        // Guardar la postura en la base de datos con el identificador interno
                        userStanceRepository.setUserStance(userId, internalTopicName, opinion)
                        Timber.d("Postura guardada en BD: tema=$internalTopicName (mostrado como $temaMostrado), opinión=$opinion, userId=$userId")
                    } else {
                        Timber.e("No se pudo encontrar el identificador interno para el tema mostrado: $temaMostrado")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error al guardar postura en la base de datos")
                }
            }
        }
        
        // Actualizamos la lista filtrada para mantener coherencia
        _temasFiltrados.value = filtrarTemas(searchQuery)
        
        Timber.d("Postura asignada: tema=$temaMostrado, opinión=$opinion")
    }
    
    /**
     * Establece un término de búsqueda para filtrar los temas.
     * 
     * @param query Término de búsqueda.
     */
    fun buscarTemas(query: String) {
        searchQuery = query
        _temasFiltrados.value = filtrarTemas(query)
    }
    
    /**
     * Filtra los temas según el término de búsqueda.
     * 
     * @param query Término de búsqueda.
     * @return Lista filtrada de temas.
     */
    private fun filtrarTemas(query: String): List<Tema> {
        val jugadorActual = _jugador.value ?: return emptyList()
        
        return if (query.isEmpty()) {
            // Si no hay término de búsqueda, devolver todos los temas
            jugadorActual.listaTemas
        } else {
            // Filtrar por el término de búsqueda en el nombre y la descripción
            jugadorActual.listaTemas.filter { tema ->
                tema.nombre.contains(query, ignoreCase = true) || 
                tema.descripcion?.contains(query, ignoreCase = true) == true || 
                tema.opinionSeleccionada.contains(query, ignoreCase = true)
            }
        }
    }
    
    /**
     * Obtiene el total de temas disponibles.
     * 
     * @return Número total de temas.
     */
    fun getTotalTemas(): Int {
        return _jugador.value?.listaTemas?.size ?: 0
    }
    
    /**
     * Obtiene el número de temas que tienen una postura asignada.
     * 
     * @return Número de temas con postura.
     */
    fun getTemasConPostura(): Int {
        return _jugador.value?.listaTemas?.count { it.opinionSeleccionada.isNotEmpty() } ?: 0
    }
}