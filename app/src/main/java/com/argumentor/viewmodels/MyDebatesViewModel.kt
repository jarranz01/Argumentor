package com.argumentor.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.mappers.DataMappers
import com.argumentor.models.Debate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel que administra la lista de debates en los que participa el usuario actual.
 */
class MyDebatesViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryProvider = RepositoryProvider.getInstance(application)
    private val debateRepository = repositoryProvider.debateRepository
    private val userRepository = repositoryProvider.userRepository
    private val sessionManager = com.argumentor.SessionManager(application)

    private val _userDebates = MutableLiveData<List<Debate>>()
    val userDebates: LiveData<List<Debate>> = _userDebates
    
    private val _ongoingDebates = MutableLiveData<List<Debate>>()
    val ongoingDebates: LiveData<List<Debate>> = _ongoingDebates
    
    private val _completedDebates = MutableLiveData<List<Debate>>()
    val completedDebates: LiveData<List<Debate>> = _completedDebates
    
    // Control de visibilidad para las secciones desplegables
    private val _isOngoingSectionExpanded = MutableLiveData(true)
    val isOngoingSectionExpanded: LiveData<Boolean> = _isOngoingSectionExpanded
    
    private val _isCompletedSectionExpanded = MutableLiveData(true)
    val isCompletedSectionExpanded: LiveData<Boolean> = _isCompletedSectionExpanded

    init {
        loadUserDebates()
    }

    /**
     * Carga los debates en los que participa el usuario actual.
     */
    private fun loadUserDebates() {
        val userId = sessionManager.getUserId()
        if (userId == null) {
            _userDebates.value = emptyList()
            _ongoingDebates.value = emptyList()
            _completedDebates.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                debateRepository.getDebatesUserParticipatesIn(userId).collect { debateEntities ->
                    // Convertir entidades a modelos de dominio
                    val debates = debateEntities.map { entity ->
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
                    
                    _userDebates.value = debates
                    
                    // Separar debates según su estado
                    val ongoing = debates.filter { 
                        it.status != Debate.Status.TERMINADO 
                    }
                    val completed = debates.filter { 
                        it.status == Debate.Status.TERMINADO 
                    }
                    
                    _ongoingDebates.value = ongoing
                    _completedDebates.value = completed
                    
                    Timber.d("Cargados ${debates.size} debates del usuario (${ongoing.size} en curso, ${completed.size} completados)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar debates del usuario")
                _userDebates.value = emptyList()
                _ongoingDebates.value = emptyList()
                _completedDebates.value = emptyList()
            }
        }
    }
    
    /**
     * Alterna el estado expandido/colapsado de la sección de debates en curso
     */
    fun toggleOngoingSection() {
        _isOngoingSectionExpanded.value = !(_isOngoingSectionExpanded.value ?: true)
    }
    
    /**
     * Alterna el estado expandido/colapsado de la sección de debates completados
     */
    fun toggleCompletedSection() {
        _isCompletedSectionExpanded.value = !(_isCompletedSectionExpanded.value ?: true)
    }

    /**
     * Factory para crear instancias del ViewModel con el contexto de la aplicación.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MyDebatesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MyDebatesViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 