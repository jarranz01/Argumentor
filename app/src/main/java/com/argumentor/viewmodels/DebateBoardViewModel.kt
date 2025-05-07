package com.argumentor.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.mappers.DataMappers
import com.argumentor.models.Debate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * ViewModel que administra la lista de debates.
 *
 * Proporciona funcionalidades para agregar debates y obtener los debates
 * almacenados en la base de datos Room.
 */
class DebateBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryProvider = RepositoryProvider.getInstance(application)
    private val debateRepository = repositoryProvider.debateRepository
    private val userRepository = repositoryProvider.userRepository
    private val sessionManager = com.argumentor.SessionManager(application)

    /**
     * Lista de debates observable.
     */
    private val _debates = MutableLiveData<List<Debate>>()
    val debates: LiveData<List<Debate>> = _debates
    
    /**
     * Término de búsqueda para filtrar debates
     */
    private var searchQuery = ""
    
    /**
     * Categoría seleccionada para filtrar debates
     */
    private var selectedCategory: String? = null
    
    /**
     * Lista completa de debates (sin filtrar)
     */
    private var allDebates = listOf<Debate>()

    init {
        // Cargar los debates desde la base de datos
        loadDebates()
    }
    
    /**
     * Carga los debates desde el repositorio y los convierte al modelo de dominio.
     */
    private fun loadDebates() {
        viewModelScope.launch {
            try {
                debateRepository.getAllDebates().collect { debateEntities ->
                    // Convertir entidades a modelos de dominio
                    allDebates = debateEntities.map { entity -> 
                        val debate = DataMappers.toDebate(entity)
                        
                        // Obtener nombre de usuario para el autor
                        var authorName = "Desconocido"
                        try {
                            val authorUserId = entity.authorUserId
                            if (!authorUserId.isNullOrEmpty()) {
                                val authorUser = userRepository.getUserById(authorUserId)
                                if (authorUser != null) {
                                    // Usar username si está disponible, sino usar name
                                    authorName = if (!authorUser.username.isNullOrEmpty()) {
                                        authorUser.username
                                    } else {
                                        authorUser.name
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error al obtener usuario autor: ${e.message}")
                        }
                        
                        // Crear copia del debate con el nombre de usuario en lugar del ID
                        debate.copy(
                            author = authorName
                        )
                    }
                    
                    // Aplicar filtros
                    applyFilters()
                    
                    Timber.d("Cargados ${allDebates.size} debates de la base de datos")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar debates")
                _debates.value = emptyList()
            }
        }
    }

    /**
     * Agrega un nuevo debate a la base de datos.
     *
     * @param title Título del debate.
     * @param description Descripción del debate.
     * @param position Posición del creador en el debate ("A_FAVOR" o "EN_CONTRA").
     * @param category Categoría del debate (opcional).
     */
    fun addDebate(title: String, description: String, position: String = "A_FAVOR", category: String? = null) {
        viewModelScope.launch {
            try {
                val currentUserId = sessionManager.getUserId()
                if (currentUserId == null) {
                    Timber.e("No se puede crear debate sin usuario autenticado")
                    return@launch
                }
                
                // Configurar las posiciones según la selección del usuario
                val participantFavorId = if (position == "A_FAVOR") currentUserId else ""
                val participantContraId = if (position == "EN_CONTRA") currentUserId else ""
                
                debateRepository.createDebate(
                    title = title,
                    description = description,
                    authorUserId = currentUserId,
                    participantFavorUserId = participantFavorId,
                    participantContraUserId = participantContraId,
                    category = category
                )
                
                Timber.d("Debate añadido: $title, posición: $position")
                // La lista se actualizará automáticamente a través del Flow del repository
            } catch (e: Exception) {
                Timber.e(e, "Error al añadir debate")
            }
        }
    }
    
    /**
     * Añade un usuario como participante en un debate existente.
     * 
     * @param debateId ID del debate al que unirse
     * @param position Posición en el debate (opcional, determinará automáticamente si no se especifica)
     */
    fun joinDebate(debateId: String, position: String? = null) {
        viewModelScope.launch {
            try {
                val currentUserId = sessionManager.getUserId() ?: return@launch
                val debate = debateRepository.getDebateById(debateId) ?: return@launch
                
                // Verificar si el usuario ya está participando
                if (debate.participantFavorUserId == currentUserId || 
                    debate.participantContraUserId == currentUserId) {
                    Timber.d("El usuario ya está participando en este debate")
                    return@launch
                }
                
                // Determinar la posición disponible automáticamente si no se especifica
                val joinPosition = position ?: if (debate.participantFavorUserId.isEmpty()) {
                    "A_FAVOR"
                } else if (debate.participantContraUserId.isEmpty()) {
                    "EN_CONTRA"
                } else {
                    // No hay posiciones disponibles
                    Timber.d("No hay posiciones disponibles en este debate")
                    return@launch
                }
                
                // Actualizar el debate con el nuevo participante
                val updatedDebate = when (joinPosition) {
                    "A_FAVOR" -> {
                        if (debate.participantFavorUserId.isNotEmpty()) {
                            Timber.d("La posición A_FAVOR ya está ocupada")
                            return@launch
                        }
                        debate.copy(participantFavorUserId = currentUserId)
                    }
                    "EN_CONTRA" -> {
                        if (debate.participantContraUserId.isNotEmpty()) {
                            Timber.d("La posición EN_CONTRA ya está ocupada")
                            return@launch
                        }
                        debate.copy(participantContraUserId = currentUserId)
                    }
                    else -> debate
                }
                
                // Actualizar estado si ahora tiene ambos participantes
                val finalDebate = if (updatedDebate.participantFavorUserId.isNotEmpty() && 
                                      updatedDebate.participantContraUserId.isNotEmpty()) {
                    updatedDebate.copy(status = "ACTIVO")
                } else {
                    updatedDebate
                }
                
                debateRepository.updateDebate(finalDebate)
                Timber.d("Usuario $currentUserId se unió al debate $debateId como $joinPosition")
                
            } catch (e: Exception) {
                Timber.e(e, "Error al unirse al debate")
            }
        }
    }
    
    /**
     * Aplica filtros a la lista completa de debates.
     */
    private fun applyFilters() {
        var filteredDebates = allDebates
        
        // Filtrar por categoría si está seleccionada
        if (selectedCategory != null) {
            filteredDebates = filteredDebates.filter { it.category == selectedCategory }
        }
        
        // Filtrar por término de búsqueda
        if (searchQuery.isNotEmpty()) {
            filteredDebates = filteredDebates.filter { debate ->
                debate.title.contains(searchQuery, ignoreCase = true) || 
                debate.description.contains(searchQuery, ignoreCase = true)
            }
        }
        
        _debates.value = filteredDebates
    }
    
    /**
     * Establece un término de búsqueda para filtrar debates.
     * 
     * @param query Término de búsqueda.
     */
    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }
    
    /**
     * Establece una categoría para filtrar debates.
     * 
     * @param category Categoría seleccionada o null para mostrar todas.
     */
    fun setCategory(category: String?) {
        selectedCategory = category
        applyFilters()
    }
    
    /**
     * Factory para crear instancias del ViewModel con el contexto de la aplicación.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebateBoardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DebateBoardViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}