package com.argumentor.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.entities.ArgumentEntity
import com.argumentor.database.mappers.DataMappers
import com.argumentor.models.Debate
import com.argumentor.models.DebateEntry
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * ViewModel para gestionar la visualización e interacción con un debate.
 *
 * Proporciona datos y lógica para las distintas etapas del debate
 * y maneja la comunicación con el repositorio para guardar y recuperar entradas.
 */
class DebateViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryProvider = RepositoryProvider.getInstance(application)
    private val debateRepository = repositoryProvider.debateRepository
    private val userRepository = repositoryProvider.userRepository
    
    // ID del debate actual
    private lateinit var debateId: String
    
    // ID del usuario actual
    private lateinit var userId: String
    
    // Posición del usuario actual en el debate
    private lateinit var userPosition: DebatePosition

    /**
     * Información básica del debate
     */
    private val _debate = MutableLiveData<Debate>()
    val debate: LiveData<Debate> = _debate

    /**
     * Entradas del debate, organizadas por etapa y posición
     */
    private val _debateEntries = MutableLiveData<Map<DebateStage, Map<DebatePosition, DebateEntry>>>()
    val debateEntries: LiveData<Map<DebateStage, Map<DebatePosition, DebateEntry>>> = _debateEntries

    /**
     * Etapa actual del debate que se está mostrando
     */
    private val _currentStage = MutableLiveData<DebateStage>()
    val currentStage: LiveData<DebateStage> = _currentStage

    /**
     * Si es el turno del usuario para contribuir al debate
     */
    private val _isUserTurn = MutableLiveData<Boolean>()
    val isUserTurn: LiveData<Boolean> = _isUserTurn
    
    /**
     * Estado de carga
     */
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Mensaje de error
     */
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Inicializa el ViewModel con los datos del debate.
     *
     * @param debateId ID del debate a cargar
     * @param userId ID del usuario actual
     */
    fun initialize(debateId: String, userId: String) {
        this.debateId = debateId
        this.userId = userId
        
        // Cargar datos del debate desde la base de datos
        loadDebate()
    }
    
    /**
     * Carga la información del debate y todas sus entradas desde la base de datos.
     */
    private fun loadDebate() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Obtener información del debate
                val debateEntity = debateRepository.getDebateById(debateId)
                
                if (debateEntity == null) {
                    _errorMessage.value = "No se pudo encontrar el debate"
                    _isLoading.value = false
                    return@launch
                }
                
                // Convertir a modelo de dominio
                val loadedDebate = DataMappers.toDebate(debateEntity)
                _debate.value = loadedDebate
                
                // Determinar la posición del usuario actual
                userPosition = if (userId == loadedDebate.participantFavor) {
                    DebatePosition.A_FAVOR
                } else {
                    DebatePosition.EN_CONTRA
                }
                
                // Cargar las entradas del debate
                loadDebateEntries()
                
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar el debate")
                _errorMessage.value = "Error al cargar el debate: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Carga todas las entradas existentes del debate desde la base de datos.
     */
    private fun loadDebateEntries() {
        viewModelScope.launch {
            try {
                // Obtener todas las entradas para este debate
                val argumentsFlow = debateRepository.getArgumentsForDebate(debateId)
                val arguments = argumentsFlow.first() // Obtener el valor actual
                
                // Convertir a mapa organizado por etapa y posición
                val entries = organizeDebateEntries(arguments)
                _debateEntries.value = entries
                
                // Determinar etapa actual
                determineCurrentStage()
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar las entradas del debate")
                _errorMessage.value = "Error al cargar los argumentos: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Organiza las entradas de debate por etapa y posición.
     */
    private fun organizeDebateEntries(arguments: List<ArgumentEntity>): Map<DebateStage, Map<DebatePosition, DebateEntry>> {
        val entries = mutableMapOf<DebateStage, MutableMap<DebatePosition, DebateEntry>>()
        
        // Inicializa mapas para cada etapa
        DebateStage.values().forEach { stage ->
            entries[stage] = mutableMapOf()
        }
        
        // Organizar argumentos por etapa y posición
        for (argument in arguments) {
            val stage = DataMappers.getDebateStageFromString(argument.stage)
            val position = DataMappers.getDebatePositionFromString(argument.position)
            val entry = DataMappers.toDebateEntry(argument)
            
            // Añadir entrada al mapa correspondiente
            entries[stage]?.put(position, entry)
        }
        
        return entries
    }
    
    /**
     * Determina la etapa actual del debate basándose en las entradas existentes.
     */
    private fun determineCurrentStage() {
        val entries = _debateEntries.value ?: return
        
        // Por defecto, comenzamos en introducción
        var currentStage = DebateStage.INTRODUCCION
        
        // Si ambas introducciones están completas, pasamos a refutación 1
        val introEntries = entries[DebateStage.INTRODUCCION] ?: emptyMap()
        if (introEntries.size == 2) {
            currentStage = DebateStage.REFUTACION1
            
            // Si ambas refutaciones 1 están completas, pasamos a refutación 2
            val ref1Entries = entries[DebateStage.REFUTACION1] ?: emptyMap()
            if (ref1Entries.size == 2) {
                currentStage = DebateStage.REFUTACION2
                
                // Si ambas refutaciones 2 están completas, pasamos a conclusión
                val ref2Entries = entries[DebateStage.REFUTACION2] ?: emptyMap()
                if (ref2Entries.size == 2) {
                    currentStage = DebateStage.CONCLUSION
                }
            }
        }
        
        _currentStage.value = currentStage
        
        // Determinar si es el turno del usuario
        determineUserTurn()
    }
    
    /**
     * Determina si es el turno del usuario para contribuir al debate.
     */
    private fun determineUserTurn() {
        val entries = _debateEntries.value ?: return
        val stage = _currentStage.value ?: return
        
        val stageEntries = entries[stage] ?: emptyMap()
        
        // Si ya tenemos una entrada para la postura del usuario en la etapa actual, no es su turno
        val isUserTurn = !stageEntries.containsKey(userPosition)
        _isUserTurn.value = isUserTurn
    }
    
    /**
     * Envía una entrada de debate para la etapa actual.
     *
     * @param content Contenido de la argumentación
     * @return true si se envió correctamente
     */
    fun submitEntry(content: String): Boolean {
        if (content.isBlank() || !(_isUserTurn.value ?: false)) {
            return false
        }
        
        val currentStage = _currentStage.value ?: return false
        
        // Crear nueva entrada
        val newEntry = DebateEntry(
            id = UUID.randomUUID().toString(),
            debateId = debateId,
            userId = userId,
            stage = currentStage,
            content = content,
            position = userPosition
        )
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Guardar la entrada en la base de datos
                debateRepository.addArgument(
                    debateId = debateId,
                    userId = userId,
                    stage = currentStage.name,
                    position = userPosition.name,
                    content = content
                )
                
                // Actualizar las entradas localmente (podríamos recargar desde la BD también)
                val currentEntries = _debateEntries.value?.toMutableMap() ?: mutableMapOf()
                val stageEntries = currentEntries[currentStage]?.toMutableMap() ?: mutableMapOf()
                stageEntries[userPosition] = newEntry
                currentEntries[currentStage] = stageEntries
                _debateEntries.value = currentEntries
                
                // Actualizar estado del turno
                _isUserTurn.value = false
                
                // Verificar si se debe avanzar la etapa del debate
                val isPhaseComplete = debateRepository.isDebatePhaseComplete(debateId, currentStage.name)
                if (isPhaseComplete) {
                    debateRepository.advanceDebateStageIfComplete(debateId, currentStage.name)
                }
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Timber.e(e, "Error al enviar el argumento")
                _errorMessage.value = "Error al enviar el argumento: ${e.message}"
                _isLoading.value = false
                return@launch
            }
        }
        
        return true
    }
    
    /**
     * Obtiene la instrucción correspondiente a una etapa específica del debate.
     *
     * @param stage La etapa del debate
     * @return El texto de instrucción para esa etapa
     */
    fun getInstructionForStage(stage: DebateStage): String {
        return when (stage) {
            DebateStage.INTRODUCCION -> "Presenta tu postura inicial sobre el tema."
            DebateStage.REFUTACION1 -> "Refuta la introducción de tu oponente."
            DebateStage.REFUTACION2 -> "Refuta la primera refutación de tu oponente."
            DebateStage.CONCLUSION -> "Presenta tu conclusión final basada en los argumentos anteriores."
        }
    }
    
    /**
     * Obtiene la entrada del oponente para una etapa anterior específica.
     *
     * @param stage La etapa actual
     * @return La entrada del oponente a refutar, o null si no existe
     */
    fun getOpponentEntryToRefute(stage: DebateStage): DebateEntry? {
        val entries = _debateEntries.value ?: return null
        
        val opponentPosition = if (userPosition == DebatePosition.A_FAVOR) {
            DebatePosition.EN_CONTRA
        } else {
            DebatePosition.A_FAVOR
        }
        
        return when (stage) {
            DebateStage.INTRODUCCION -> null  // No hay entrada previa en la introducción
            DebateStage.REFUTACION1 -> entries[DebateStage.INTRODUCCION]?.get(opponentPosition)
            DebateStage.REFUTACION2 -> entries[DebateStage.REFUTACION1]?.get(opponentPosition)
            DebateStage.CONCLUSION -> entries[DebateStage.REFUTACION2]?.get(opponentPosition)
        }
    }
    
    /**
     * Comprueba si el usuario ya ha completado una etapa específica.
     *
     * @param stage La etapa a comprobar
     * @return true si la etapa ya está completada por el usuario
     */
    fun isStageCompletedByUser(stage: DebateStage): Boolean {
        val entries = _debateEntries.value ?: return false
        val stageEntries = entries[stage] ?: return false
        return stageEntries.containsKey(userPosition)
    }
    
    /**
     * Obtiene la entrada del usuario para una etapa específica.
     *
     * @param stage La etapa para la que se quiere obtener la entrada
     * @return La entrada del usuario para esa etapa, o null si no existe
     */
    fun getUserEntry(stage: DebateStage): DebateEntry? {
        val entries = _debateEntries.value ?: return null
        val stageEntries = entries[stage] ?: return null
        return stageEntries[userPosition]
    }
    
    /**
     * Genera un JSON del debate en el formato requerido para el análisis del ganador.
     *
     * @return String con la representación JSON del debate o null si hay error
     */
    fun generateDebateJson(): String? {
        // Si el debate no está completo, no generamos JSON
        if (_currentStage.value != DebateStage.CONCLUSION || !isStageCompletedByUser(DebateStage.CONCLUSION)) {
            return null
        }
        
        viewModelScope.launch {
            try {
                val jsonContent = debateRepository.debateToJson(debateId)
                Timber.d("JSON del debate generado: $jsonContent")
            } catch (e: Exception) {
                Timber.e(e, "Error al generar JSON del debate")
            }
        }
        
        return null
    }
    
    /**
     * Factory para crear instancias del ViewModel con el contexto de la aplicación.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DebateViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}