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
                
                // Convertir entidades a modelos de dominio
                val temas = topicEntities.map { topicEntity ->
                    val stance = userStances.firstOrNull { it.topicName == topicEntity.topicName }
                    
                    // Resolver el nombre del tema - versión simplificada y robusta
                    val nombreTema = resolveTopicName(topicEntity.topicName)
                    
                    Tema(
                        nombre = nombreTema,
                        descripcion = topicEntity.description,
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
            when {
                // Caso 1: Es una referencia de tipo "R.string.nombre"
                topicName.startsWith("R.string.") -> {
                    val resourceName = topicName.substringAfter("R.string.")
                    val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
                    if (resourceId != 0) context.getString(resourceId) else topicName
                }
                
                // Caso 2: Es directamente un ID numérico
                topicName.all { it.isDigit() } -> {
                    val resourceId = topicName.toIntOrNull()
                    if (resourceId != null && resourceId != 0) context.getString(resourceId) else topicName
                }
                
                // Caso 3: Podría ser un identificador de recurso directo (como "marijuana")
                else -> {
                    // Intentar resolver como identificador de recurso
                    val resourceId = context.resources.getIdentifier(topicName, "string", context.packageName)
                    if (resourceId != 0) {
                        // Si existe un recurso con ese nombre, usar el texto localizado
                        context.getString(resourceId)
                    } else {
                        // Si no es un recurso, simplemente devolver el nombre original
                        topicName
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al resolver el nombre del tema: $topicName")
            topicName // Si falla, devolvemos el nombre original
        }
    }
    /**
     * Asigna una postura a un tema específico.
     *
     * @param tema El nombre del tema al que se le asignará la postura.
     * @param opinion La opinión seleccionada para el tema.
     */
    fun asignarPostura(tema: String, opinion: String) {
        // Actualizar el modelo local
        _jugador.value?.asignarPostura(tema, opinion)
        
        // Actualizar la base de datos si hay un usuario con sesión
        currentUserId?.let { userId ->
            viewModelScope.launch {
                try {
                    // Obtener el topicName original desde la base de datos usando el nombre resuelto
                    val topicEntities = topicRepository.getAllTopics().first()
                    val originalTopicName = topicEntities.find { 
                        resolveTopicName(it.topicName) == tema 
                    }?.topicName ?: tema
                    
                    // Guardar la postura en la base de datos con el nombre original del tema
                    userStanceRepository.setUserStance(userId, originalTopicName, opinion)
                    Timber.d("Postura guardada en BD: tema=$originalTopicName, opinión=$opinion, userId=$userId")
                } catch (e: Exception) {
                    Timber.e(e, "Error al guardar postura en la base de datos")
                }
            }
        }
        
        // Actualizamos la lista filtrada para mantener coherencia
        _temasFiltrados.value = filtrarTemas(searchQuery)
        
        Timber.d("Postura asignada: tema=$tema, opinión=$opinion")
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