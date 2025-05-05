package com.argumentor.services

import android.content.Context
import com.argumentor.database.entities.*
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Servicio para sincronizar datos entre la base de datos local (Room) y Firebase Firestore.
 * 
 * Esta clase proporciona métodos para realizar copias de seguridad de datos locales en Firebase
 * y para sincronizar datos desde Firebase a la base de datos local.
 */
class FirebaseService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        ArgumentorApplication.getInstance().firestore
    }
    
    private val auth: FirebaseAuth by lazy {
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
    
    // Nombres de colecciones en Firestore
    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_DEBATES = "debates"
        private const val COLLECTION_ARGUMENTS = "arguments"
        private const val COLLECTION_TOPICS = "topics"
        private const val COLLECTION_USER_STANCES = "user_stances"
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
                
                Timber.d("Sincronización completa con Firebase realizada correctamente")
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
                    "createdAt" to user.createdAt
                    // No incluimos la contraseña ya que se maneja con Firebase Auth
                )
                
                firestore.collection(COLLECTION_USERS)
                    .document(user.userId)
                    .set(userMap)
                    .await()
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
                firestore.collection(COLLECTION_DEBATES)
                    .document(debate.debateId)
                    .set(debate)
                    .await()
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
                
                firestore.collection(COLLECTION_ARGUMENTS)
                    .document(docId)
                    .set(argument)
                    .await()
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
                firestore.collection(COLLECTION_TOPICS)
                    .document(topic.topicName)
                    .set(topic)
                    .await()
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
                
                firestore.collection(COLLECTION_USER_STANCES)
                    .document(docId)
                    .set(stance)
                    .await()
            }
            
            Timber.d("Posturas de usuario sincronizadas con Firestore: ${stances.size} posturas")
        } catch (e: Exception) {
            Timber.e(e, "Error sincronizando posturas de usuario con Firestore")
            throw e
        }
    }
    
    /**
     * Obtiene datos de usuario desde Firestore.
     * Este método se puede usar para obtener datos específicos de un usuario.
     */
    suspend fun getUserFromFirestore(userId: String): HashMap<String, Any>? {
        return try {
            val document = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                document.data as HashMap<String, Any>
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error obteniendo usuario desde Firestore")
            null
        }
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
                
                // Se podrían implementar sincronizaciones adicionales según sea necesario
                
                Timber.d("Sincronización desde Firestore completada: ${remoteDebates.size} debates")
            } catch (e: Exception) {
                Timber.e(e, "Error sincronizando desde Firestore")
            }
        }
    }
}