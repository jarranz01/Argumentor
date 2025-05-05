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
                        DataMappers.toDebate(entity)
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
     * @param category Categoría del debate (opcional).
     */
    fun addDebate(title: String, description: String, category: String? = null) {
        viewModelScope.launch {
            try {
                val currentUserId = sessionManager.getUserId()
                
                // Por ahora, usamos el mismo usuario para ambos participantes
                // En una implementación real, esto debería gestionarse en otra parte
                debateRepository.createDebate(
                    title = title,
                    description = description,
                    authorUserId = currentUserId,
                    participantFavorUserId = currentUserId ?: "anonymous",
                    participantContraUserId = currentUserId ?: "anonymous",
                    category = category
                )
                
                Timber.d("Debate añadido: $title")
                // La lista se actualizará automáticamente a través del Flow del repository
            } catch (e: Exception) {
                Timber.e(e, "Error al añadir debate")
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