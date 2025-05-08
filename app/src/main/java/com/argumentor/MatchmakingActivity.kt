package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.entities.UserStanceEntity
import com.argumentor.databinding.ActivityMatchmakingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Actividad para emparejar usuarios con posturas opuestas en temas específicos.
 * 
 * Esta actividad busca otros usuarios que tengan posturas opuestas a las del usuario actual
 * y crea automáticamente un debate cuando encuentra una coincidencia.
 */
class MatchmakingActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivityMatchmakingBinding
    private lateinit var observer: MyObserver
    private lateinit var sessionManager: SessionManager
    private lateinit var repositoryProvider: RepositoryProvider
    
    // Controla si el usuario está activamente buscando un emparejamiento
    private val isSearching = AtomicBoolean(false)
    
    // Handler para actualizar la UI y realizar búsquedas periódicas
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchRunnable: Runnable
    
    // Intervalo de búsqueda en milisegundos
    private val SEARCH_INTERVAL = 3000L
    
    // Contador para mostrar animación de puntos suspensivos
    private var dotCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        
        // Inicializar el observador de ciclo de vida
        observer = MyObserver(lifecycle, "MatchmakingActivity", this)
        
        // Inicializar componentes
        sessionManager = SessionManager(this)
        repositoryProvider = RepositoryProvider.getInstance(this)
        
        // Verificar si hay sesión activa
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.session_expired_message, Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }
        
        // Inicializar Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_matchmaking)
        
        // Configurar la barra de herramientas
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        // Configurar el botón de búsqueda
        setupSearchButton()
        
        // Inicializar el runnable para búsqueda periódica
        initializeSearchRunnable()
    }
    
    /**
     * Configura el botón de búsqueda para iniciar/detener el matchmaking.
     */
    private fun setupSearchButton() {
        binding.buttonStartSearch.setOnClickListener {
            if (isSearching.get()) {
                // Detener la búsqueda
                stopSearching()
            } else {
                // Iniciar la búsqueda
                startSearching()
            }
        }
    }
    
    /**
     * Inicializa el Runnable para realizar búsquedas periódicas.
     */
    private fun initializeSearchRunnable() {
        searchRunnable = object : Runnable {
            override fun run() {
                if (isSearching.get()) {
                    // Actualizar texto con puntos suspensivos animados
                    updateSearchingText()
                    
                    // Realizar una búsqueda
                    performMatchmaking()
                    
                    // Programar la próxima búsqueda
                    handler.postDelayed(this, SEARCH_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Inicia el proceso de búsqueda de emparejamiento.
     */
    private fun startSearching() {
        isSearching.set(true)
        
        // Actualizar UI
        binding.buttonStartSearch.setText(R.string.stop_searching)
        binding.progressBar.visibility = View.VISIBLE
        binding.textSearchStatus.visibility = View.VISIBLE
        binding.textSearchStatus.setText(R.string.searching_for_opponent)
        
        // Iniciar búsqueda periódica
        handler.post(searchRunnable)
        
        Timber.d("Iniciando búsqueda de matchmaking")
    }
    
    /**
     * Detiene el proceso de búsqueda de emparejamiento.
     */
    private fun stopSearching() {
        isSearching.set(false)
        
        // Actualizar UI
        binding.buttonStartSearch.setText(R.string.start_searching)
        binding.progressBar.visibility = View.GONE
        binding.textSearchStatus.visibility = View.GONE
        
        // Detener búsqueda periódica
        handler.removeCallbacks(searchRunnable)
        
        Timber.d("Deteniendo búsqueda de matchmaking")
    }
    
    /**
     * Actualiza el texto de búsqueda con una animación de puntos suspensivos.
     */
    private fun updateSearchingText() {
        dotCount = (dotCount + 1) % 4
        val dots = ".".repeat(dotCount)
        binding.textSearchStatus.text = getString(R.string.searching_for_opponent) + dots
    }
    
    /**
     * Crea un debate entre el usuario actual y un oponente.
     * 
     * @param currentUserId ID del usuario actual
     * @param opponentId ID del oponente
     * @param topicName Nombre del tema
     * @param isCurrentUserFavor True si el usuario actual está a favor
     * @return ID del debate creado o null si hubo un error
     */
    private suspend fun createDebateWithOpponent(
        currentUserId: String,
        opponentId: String,
        topicName: String,
        isCurrentUserFavor: Boolean
    ): String? {
        try {
            // Obtener información del tema
            val topic = repositoryProvider.topicRepository.getTopicByName(topicName)
            if (topic == null) {
                Timber.e("No se pudo encontrar el tema: $topicName")
                return null
            }
            
            // Determinar quién está a favor y quién en contra
            val favorUserId = if (isCurrentUserFavor) currentUserId else opponentId
            val contraUserId = if (isCurrentUserFavor) opponentId else currentUserId
            
            // Crear el debate
            val debateId = repositoryProvider.debateRepository.createDebate(
                title = topic.topicName,
                description = topic.description,
                authorUserId = currentUserId, // El usuario actual es el autor
                participantFavorUserId = favorUserId,
                participantContraUserId = contraUserId,
                category = "matchmaking" // Categoría especial para debates de matchmaking
            )
            
            // Sincronizar inmediatamente con Firebase
            repositoryProvider.firebaseService.syncAllData()
            
            // Notificar al oponente mediante una llamada POST a Firestore
            sendDebateNotification(debateId, opponentId, topic.topicName)
            
            Timber.d("Debate creado con ID: $debateId entre usuarios $favorUserId y $contraUserId sobre tema $topicName")
            return debateId
            
        } catch (e: Exception) {
            Timber.e(e, "Error al crear debate durante matchmaking")
            return null
        }
    }
    
    /**
     * Envía una notificación al oponente sobre el nuevo debate.
     * Utiliza una llamada POST directa a Firestore.
     */
    private suspend fun sendDebateNotification(debateId: String, opponentId: String, topicName: String) {
        try {
            val notification = hashMapOf(
                "type" to "new_debate",
                "debateId" to debateId,
                "topicName" to topicName,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )
            
            // Usar una colección específica para notificaciones
            repositoryProvider.firebaseService.firestore
                .collection("notifications")
                .document("${opponentId}_${debateId}")
                .set(notification)
                .await()
            
            Timber.d("Notificación enviada al usuario $opponentId sobre el debate $debateId")
        } catch (e: Exception) {
            Timber.e(e, "Error al enviar notificación: ${e.message}")
            // No interrumpimos el flujo principal si falla la notificación
        }
    }
    
    /**
     * Realiza la lógica de matchmaking buscando usuarios con posturas opuestas.
     */
    private fun performMatchmaking() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUserId = sessionManager.getUserId()
                if (currentUserId == null) {
                    withContext(Dispatchers.Main) {
                        stopSearching()
                        Toast.makeText(this@MatchmakingActivity, R.string.session_expired_message, Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                    return@launch
                }
                
                // Obtener las posturas del usuario actual
                val userStances = repositoryProvider.userStanceRepository
                    .getAllStancesForUser(currentUserId)
                    .first()
                
                // Filtrar solo las posturas "A favor" o "En contra" (no las "Indiferente")
                val favorString = getString(R.string.opinion_favor)
                val contraString = getString(R.string.opinion_against)
                
                val relevantStances = userStances.filter { 
                    it.stance == favorString || it.stance == contraString 
                }
                
                if (relevantStances.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MatchmakingActivity, 
                            R.string.no_stances_for_matchmaking, 
                            Toast.LENGTH_SHORT
                        ).show()
                        stopSearching()
                    }
                    return@launch
                }
                
                // Buscar potenciales oponentes usando una llamada GET a Firestore
                val potentialMatches = findPotentialOpponentsFromFirestore(relevantStances, currentUserId)
                
                if (potentialMatches.isNotEmpty()) {
                    // Tomar el primer match encontrado
                    val match = potentialMatches.first()
                    
                    // Crear un debate con el oponente encontrado
                    val stance = relevantStances.find { it.topicName == match.topicName }
                    if (stance != null) {
                        val debateId = createDebateWithOpponent(
                            currentUserId, 
                            match.userId, 
                            stance.topicName,
                            stance.stance == favorString
                        )
                        
                        if (debateId != null) {
                            withContext(Dispatchers.Main) {
                                // Detener la búsqueda
                                stopSearching()
                                
                                // Mostrar mensaje de éxito
                                Toast.makeText(
                                    this@MatchmakingActivity,
                                    getString(R.string.matchmaking_success, match.topicName),
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Navegar al debate creado
                                navigateToDebate(debateId)
                            }
                            return@launch
                        }
                    }
                } else {
                    // Intentar el método anterior como fallback
                    for (stance in relevantStances) {
                        val match = findMatchingOpponent(stance, currentUserId)
                        
                        if (match != null) {
                            // Crear un debate con el oponente encontrado
                            val debateId = createDebateWithOpponent(
                                currentUserId, 
                                match.userId, 
                                stance.topicName,
                                stance.stance == favorString
                            )
                            
                            if (debateId != null) {
                                withContext(Dispatchers.Main) {
                                    // Detener la búsqueda
                                    stopSearching()
                                    
                                    // Mostrar mensaje de éxito
                                    Toast.makeText(
                                        this@MatchmakingActivity,
                                        getString(R.string.matchmaking_success, match.topicName),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    
                                    // Navegar al debate creado
                                    navigateToDebate(debateId)
                                }
                                return@launch
                            }
                        }
                    }
                }
                
                // No se encontró emparejamiento en esta iteración
                Timber.d("No se encontró emparejamiento en esta iteración")
                
            } catch (e: Exception) {
                Timber.e(e, "Error durante el matchmaking")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MatchmakingActivity,
                        getString(R.string.matchmaking_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    stopSearching()
                }
            }
        }
    }
    
    /**
     * Busca oponentes potenciales directamente desde Firestore usando una llamada GET.
     * 
     * @param userStances Las posturas del usuario actual
     * @param currentUserId ID del usuario actual
     * @return Lista de potenciales emparejamientos
     */
    private suspend fun findPotentialOpponentsFromFirestore(
        userStances: List<UserStanceEntity>,
        currentUserId: String
    ): List<UserStanceEntity> {
        val matches = mutableListOf<UserStanceEntity>()
        
        try {
            val firestore = repositoryProvider.firebaseService.firestore
            
            // Para cada postura del usuario, buscar oponentes con postura opuesta
            for (stance in userStances) {
                val favorString = getString(R.string.opinion_favor)
                val contraString = getString(R.string.opinion_against)
                
                // Determinar la postura opuesta
                val oppositeStance = if (stance.stance == favorString) contraString else favorString
                
                // Realizar una consulta a Firestore para encontrar usuarios con la postura opuesta
                val query = firestore.collection("user_stances")
                    .whereEqualTo("topicName", stance.topicName)
                    .whereEqualTo("stance", oppositeStance)
                    .get()
                    .await()
                
                // Filtrar para excluir al usuario actual
                val potentialMatches = query.documents.mapNotNull { doc ->
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    if (userId != currentUserId) {
                        UserStanceEntity(
                            userId = userId,
                            topicName = stance.topicName,
                            stance = oppositeStance,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } else {
                        null
                    }
                }
                
                matches.addAll(potentialMatches)
            }
            
            Timber.d("Encontrados ${matches.size} potenciales oponentes en Firestore")
        } catch (e: Exception) {
            Timber.e(e, "Error al buscar oponentes en Firestore: ${e.message}")
        }
        
        return matches
    }
    
    /**
     * Busca un oponente con una postura opuesta sobre el mismo tema.
     * 
     * @param userStance La postura del usuario actual
     * @param currentUserId ID del usuario actual
     * @return La postura del oponente encontrado o null si no hay coincidencia
     */
    private suspend fun findMatchingOpponent(userStance: UserStanceEntity, currentUserId: String): UserStanceEntity? {
        // Obtener todas las posturas sobre este tema
        val allStancesOnTopic = repositoryProvider.userStanceRepository
            .getAllStancesOnTopic(userStance.topicName)
        
        // Filtrar para encontrar usuarios con postura opuesta
        val oppositeStance = if (userStance.stance == getString(R.string.opinion_favor)) {
            getString(R.string.opinion_against)
        } else {
            getString(R.string.opinion_favor)
        }
        
        // Encontrar usuarios con la postura opuesta, excluyendo al usuario actual
        return allStancesOnTopic.find { 
            it.userId != currentUserId && it.stance == oppositeStance 
        }
    }
    
    /**
     * Navega a la actividad de visualización de debate.
     * 
     * @param debateId ID del debate a visualizar
     */
    private fun navigateToDebate(debateId: String) {
        val intent = Intent(this, DebateViewActivity::class.java).apply {
            putExtra("debate_id", debateId)
        }
        startActivity(intent)
        finish() // Finalizar esta actividad para que no vuelva al matchmaking
    }
    
    /**
     * Navega a la pantalla de inicio de sesión.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    override fun onPause() {
        super.onPause()
        // Detener la búsqueda si la actividad pasa a segundo plano
        if (isSearching.get()) {
            stopSearching()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Asegurar que se detengan todas las búsquedas
        handler.removeCallbacks(searchRunnable)
    }
}