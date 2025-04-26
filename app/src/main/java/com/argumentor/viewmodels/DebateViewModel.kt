package com.argumentor.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.argumentor.models.Debate
import com.argumentor.models.DebateEntry
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage

/**
 * ViewModel para gestionar la visualización e interacción con un debate.
 *
 * Proporciona datos y lógica para las distintas etapas del debate
 * y maneja la comunicación con el repositorio para guardar y recuperar entradas.
 */
class DebateViewModel : ViewModel() {

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
     * Inicializa el ViewModel con los datos del debate.
     *
     * @param debateId ID del debate a cargar
     * @param userId ID del usuario actual
     */
    fun initialize(debateId: String, userId: String) {
        this.debateId = debateId
        this.userId = userId
        
        // Cargar datos del debate (simulado por ahora)
        loadDebate()
    }
    
    /**
     * Carga la información del debate y todas sus entradas.
     */
    private fun loadDebate() {
        // Simulación de carga de debate
        // En una implementación real, esto cargaría los datos desde una API o base de datos
        val loadedDebate = Debate(
            id = debateId,
            title = "¿Es ético el uso de la IA en la toma de decisiones médicas?",
            description = "Debate sobre los aspectos éticos del uso de inteligencia artificial para diagnosticar y recomendar tratamientos médicos.",
            author = "Usuario1",
            participantFavor = "Usuario1",
            participantContra = "Usuario2",
            status = if (userId == "Usuario1") com.argumentor.models.DebateStatus.ACTIVO else com.argumentor.models.DebateStatus.PENDIENTE
        )
        
        // Determinar la posición del usuario actual
        userPosition = if (userId == loadedDebate.participantFavor) {
            DebatePosition.A_FAVOR
        } else {
            DebatePosition.EN_CONTRA
        }
        
        _debate.value = loadedDebate
        
        // Simular carga de entradas
        loadDebateEntries()
    }
    
    /**
     * Carga todas las entradas existentes del debate.
     */
    private fun loadDebateEntries() {
        // Esta lógica se reemplazaría por llamadas a un repositorio o API
        val entries = mutableMapOf<DebateStage, MutableMap<DebatePosition, DebateEntry>>()
        
        // Inicializa mapas para cada etapa
        DebateStage.values().forEach { stage ->
            entries[stage] = mutableMapOf()
        }
        
        _debateEntries.value = entries
        
        // Determinar etapa actual
        determineCurrentStage()
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
            id = java.util.UUID.randomUUID().toString(),
            debateId = debateId,
            userId = userId,
            stage = currentStage,
            content = content,
            position = userPosition
        )
        
        // Actualizar las entradas localmente
        val currentEntries = _debateEntries.value?.toMutableMap() ?: mutableMapOf()
        val stageEntries = currentEntries[currentStage]?.toMutableMap() ?: mutableMapOf()
        stageEntries[userPosition] = newEntry
        currentEntries[currentStage] = stageEntries
        _debateEntries.value = currentEntries
        
        // Actualizar estado del turno
        _isUserTurn.value = false
        
        // En una implementación real, aquí se enviaría a la API o base de datos
        
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
}