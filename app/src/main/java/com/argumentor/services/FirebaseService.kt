package com.argumentor.services

import android.content.Context
import com.argumentor.database.entities.*
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Servicio para sincronizar datos entre la base de datos local (Room) y Firebase Firestore.
 * 
 * Esta clase proporciona métodos para realizar copias de seguridad de datos locales en Firebase
 * y para sincronizar datos desde Firebase a la base de datos local.
 */
class FirebaseService(private val context: Context) {

    val firestore: FirebaseFirestore by lazy {
        ArgumentorApplication.getInstance().firestore
    }
    
    val auth: FirebaseAuth by lazy {
        ArgumentorApplication.getInstance().firebaseAuth
    }
    
    private val database by lazy {
        ArgumentorApplication.getInstance().database
    }
    
    private val userDao by lazy { database.userDao() }
    private val debateDao by lazy { database.debateDao() }
    private val argumentDao by lazy { database.argumentDao() }
    private val topicDao by lazy { database.topicDao() }
    private val userStanceDao by lazy { database.userStanceDao() }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Job para la sincronización periódica
    private var syncJob: Job? = null
    
    // Nombres de colecciones en Firestore
    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_DEBATES = "debates"
        private const val COLLECTION_ARGUMENTS = "arguments"
        private const val COLLECTION_TOPICS = "topics"
        private const val COLLECTION_USER_STANCES = "user_stances"
        private const val COLLECTION_NOTIFICATIONS = "notifications"
        
        // Intervalo de sincronización periódica (15 minutos)
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
    
    /**
     * Inicia la sincronización periódica automática con Firebase.
     * Se ejecutará cada SYNC_INTERVAL_MS milisegundos.
     */
    fun startPeriodicSync() {
        // Cancelar cualquier trabajo de sincronización anterior
        stopPeriodicSync()
        
        syncJob = scope.launch {
            while (true) {
                try {
                    // Sincronizar datos
                    syncAllData()
                    
                    // Esperar el intervalo definido
                    delay(SYNC_INTERVAL_MS)
                } catch (e: Exception) {
                    Timber.e(e, "Error durante la sincronización periódica")
                    // Si hay un error, esperamos un poco antes de reintentar
                    delay(TimeUnit.MINUTES.toMillis(1))
                }
            }
        }
        
        Timber.d("Sincronización periódica iniciada")
    }
    
    /**
     * Detiene la sincronización periódica.
     */
    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        Timber.d("Sincronización periódica detenida")
    }
    
    /**
     * Sincroniza todos los datos locales con Firebase Firestore.
     * Este método se puede llamar periódicamente o cuando ocurren cambios significativos.
     */
    fun syncAllData() {
        val currentUser = auth.currentUser ?: return
        
        scope.launch {
            try {
                // Sincronizar usuarios
                syncUsersToFirestore()
                
                // Sincronizar debates
                syncDebatesToFirestore()
                
                // Sincronizar argumentos
                syncArgumentsToFirestore()
                
                // Sincronizar temas
                syncTopicsToFirestore()
                
                // Sincronizar posturas de usuario
                syncUserStancesToFirestore()
                
            } catch (e: Exception) {
                Timber.e(e, "Error durante la sincronización con Firebase")
            }
        }
    }
    
    /**
     * Sincroniza los datos de usuario con Firestore.
     */
    private suspend fun syncUsersToFirestore() {
        try {
            val users = userDao.getAllUsers().first()
            
            for (user in users) {
                // No sincronizar la contraseña a Firestore
                val userMap = hashMapOf(
                    "userId" to user.userId,
                    "name" to user.name,
                    "email" to user.email,
                    "username" to user.username,
                    "createdAt" to user.createdAt,
                    "lastSyncTimestamp" to System.currentTimeMillis()
                )
                
                postToFirestore(COLLECTION_USERS, user.userId, userMap)
            }
            
            Timber.d("Usuarios sincronizados con Firestore: ${users.size} usuarios")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando usuarios con Firestore")
            throw e
        }
    }
    
    /**
     * Sincroniza los debates con Firestore.
     */
    private suspend fun syncDebatesToFirestore() {
        try {
            val debates = debateDao.getAllDebates().first()
            
            for (debate in debates) {
                postToFirestore(COLLECTION_DEBATES, debate.debateId, debate)
            }
            
            Timber.d("Debates sincronizados con Firestore: ${debates.size} debates")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando debates con Firestore")
            throw e
        }
    }
    
    /**
     * Sincroniza los argumentos con Firestore.
     */
    private suspend fun syncArgumentsToFirestore() {
        try {
            val arguments = argumentDao.getAllArguments().first()
            
            for (argument in arguments) {
                // Los IDs generados automáticamente pueden causar problemas,
                // así que creamos un ID de documento compuesto
                val docId = "${argument.debateId}_${argument.userId}_${argument.stage}"
                
                postToFirestore(COLLECTION_ARGUMENTS, docId, argument)
            }
            
            Timber.d("Argumentos sincronizados con Firestore: ${arguments.size} argumentos")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando argumentos con Firestore")
            throw e
        }
    }
    
    /**
     * Sincroniza los temas con Firestore.
     */
    private suspend fun syncTopicsToFirestore() {
        try {
            val topics = topicDao.getAllTopics().first()
            
            for (topic in topics) {
                postToFirestore(COLLECTION_TOPICS, topic.topicName, topic)
            }
            
            Timber.d("Temas sincronizados con Firestore: ${topics.size} temas")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando temas con Firestore")
            throw e
        }
    }
    
    /**
     * Sincroniza las posturas de usuario con Firestore.
     */
    private suspend fun syncUserStancesToFirestore() {
        try {
            val stances = userStanceDao.getAllStances().first()
            
            for (stance in stances) {
                val docId = "${stance.userId}_${stance.topicName}"
                
                postToFirestore(COLLECTION_USER_STANCES, docId, stance)
            }
            
            Timber.d("Posturas de usuario sincronizadas con Firestore: ${stances.size} posturas")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando posturas de usuario con Firestore")
            throw e
        }
    }
    
    /**
     * Realiza una operación POST a Firestore (crear o actualizar documento).
     * 
     * @param collection Nombre de la colección
     * @param documentId ID del documento
     * @param data Datos a guardar
     * @return true si la operación fue exitosa
     */
    suspend fun <T> postToFirestore(collection: String, documentId: String, data: T): Boolean {
        return try {
            // Verificar si el usuario está autenticado
            if (auth.currentUser == null) {
                Timber.e("Error en POST a $collection/$documentId: Usuario no autenticado")
                return false
            }
            
            // Asegurarse de que los datos no sean nulos
            if (data == null) {
                Timber.e("Error en POST a $collection/$documentId: Datos nulos")
                return false
            }
            
            // Loggear la operación para diagnóstico
            Timber.d("Intentando POST a $collection/$documentId con usuario ${auth.currentUser?.uid}")
            
            // Realizar la operación
            firestore.collection(collection)
                .document(documentId)
                .set(data)
                .await()
            
            Timber.d("POST exitoso a $collection/$documentId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error en POST a $collection/$documentId: ${e.message}")
            false
        }
    }
    
    /**
     * Realiza una operación GET a Firestore para obtener un documento específico.
     * 
     * @param collection Nombre de la colección
     * @param documentId ID del documento
     * @return El documento o null si no existe o hay error
     */
    suspend fun getDocumentFromFirestore(collection: String, documentId: String): DocumentSnapshot? {
        return try {
            val document = firestore.collection(collection)
                .document(documentId)
                .get()
                .await()
            
            if (document.exists()) {
                Timber.d("GET exitoso de $collection/$documentId")
                document
            } else {
                Timber.d("Documento no encontrado: $collection/$documentId")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en GET de $collection/$documentId")
            null
        }
    }
    
    /**
     * Realiza una consulta GET a Firestore con filtros.
     * 
     * @param collection Nombre de la colección
     * @param field Campo para filtrar
     * @param value Valor para filtrar
     * @return Los documentos que coinciden con el filtro o una lista vacía si hay error
     */
    suspend fun queryFirestore(collection: String, field: String, value: Any): QuerySnapshot? {
        return try {
            val query = firestore.collection(collection)
                .whereEqualTo(field, value)
                .get()
                .await()
            
            Timber.d("Consulta exitosa a $collection donde $field=$value (${query.size()} resultados)")
            query
        } catch (e: Exception) {
            Timber.e(e, "Error en consulta a $collection donde $field=$value")
            null
        }
    }
    
    /**
     * Realiza una consulta GET a Firestore con múltiples filtros.
     * 
     * @param collection Nombre de la colección
     * @param filters Mapa de campos y valores para filtrar
     * @return Los documentos que coinciden con los filtros o una lista vacía si hay error
     */
    suspend fun queryFirestoreWithFilters(
        collection: String, 
        filters: Map<String, Any>
    ): QuerySnapshot? {
        return try {
            var query: Query = firestore.collection(collection)
            
            // Aplicar cada filtro a la consulta
            for ((field, value) in filters) {
                query = query.whereEqualTo(field, value)
            }
            
            val result = query.get().await()
            
            Timber.d("Consulta con múltiples filtros exitosa a $collection (${result.size()} resultados)")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error en consulta con múltiples filtros a $collection")
            null
        }
    }
    
    /**
     * Obtiene datos de usuario desde Firestore.
     * Este método se puede usar para obtener datos específicos de un usuario.
     */
    suspend fun getUserFromFirestore(userId: String): HashMap<String, Any>? {
        val document = getDocumentFromFirestore(COLLECTION_USERS, userId)
        return document?.data as HashMap<String, Any>?
    }
    
    /**
     * Obtiene todos los debates desde Firestore.
     * Útil para sincronizar datos desde Firestore a la base de datos local.
     */
    suspend fun getDebatesFromFirestore(): List<DebateEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_DEBATES)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(DebateEntity::class.java)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error obteniendo debates desde Firestore")
            emptyList()
        }
    }
    
    /**
     * Sincroniza datos desde Firestore a la base de datos local.
     * Este método se puede llamar al iniciar la aplicación o después de un período de desconexión.
     */
    fun syncFromFirestore() {
        val currentUser = auth.currentUser ?: return
        
        scope.launch {
            try {
                // Sincronizar debates
                val remoteDebates = getDebatesFromFirestore()
                for (debate in remoteDebates) {
                    debateDao.insertDebate(debate)
                }
                
                // Sincronizar argumentos
                syncArgumentsFromFirestore()
                
                // Sincronizar posturas de usuario
                syncUserStancesFromFirestore(currentUser.uid)
                
                Timber.d("Sincronización desde Firestore completada")
            } catch (e: Exception) {
                Timber.e(e, "Error sincronizando desde Firestore")
            }
        }
    }
    
    /**
     * Sincroniza los argumentos desde Firestore a la base de datos local.
     */
    private suspend fun syncArgumentsFromFirestore() {
        try {
            val snapshot = firestore.collection(COLLECTION_ARGUMENTS)
                .get()
                .await()
            
            val arguments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ArgumentEntity::class.java)
            }
            
            for (argument in arguments) {
                argumentDao.insertArgument(argument)
            }
            
            Timber.d("Argumentos sincronizados desde Firestore: ${arguments.size} argumentos")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando argumentos desde Firestore")
        }
    }
    
    /**
     * Sincroniza las posturas de usuario desde Firestore a la base de datos local.
     */
    private suspend fun syncUserStancesFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection(COLLECTION_USER_STANCES)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val stances = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserStanceEntity::class.java)
            }
            
            for (stance in stances) {
                userStanceDao.insertOrUpdateStance(stance)
            }
            
            Timber.d("Posturas de usuario sincronizadas desde Firestore: ${stances.size} posturas")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando posturas de usuario desde Firestore")
        }
    }
    
    /**
     * Obtiene las notificaciones no leídas para un usuario.
     * 
     * @param userId ID del usuario
     * @return Lista de notificaciones no leídas
     */
    suspend fun getUnreadNotifications(userId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("read", false)
                .whereGreaterThanOrEqualTo("timestamp", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                if (doc.id.startsWith(userId)) {
                    doc.data
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al obtener notificaciones no leídas")
            emptyList()
        }
    }
    
    /**
     * Marca una notificación como leída.
     * 
     * @param notificationId ID de la notificación
     * @return true si la operación fue exitosa
     */
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return postToFirestore(
            COLLECTION_NOTIFICATIONS,
            notificationId,
            mapOf("read" to true, "readTimestamp" to System.currentTimeMillis())
        )
    }
}