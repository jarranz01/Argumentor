package com.argumentor.database

import android.content.Context
import com.argumentor.database.repositories.DebateRepository
import com.argumentor.database.repositories.TopicRepository
import com.argumentor.database.repositories.UserRepository
import com.argumentor.database.repositories.UserStanceRepository
import com.argumentor.database.utils.DebateJsonFormatter
import com.argumentor.models.ArgumentorApplication
import com.argumentor.services.FirebaseService

/**
 * Proveedor de repositorios para acceder fácilmente a los repositorios desde cualquier parte de la aplicación.
 */
class RepositoryProvider(private val context: Context) {
    
    private val database by lazy { (context.applicationContext as ArgumentorApplication).database }
    
    /**
     * Proporciona acceso al repositorio de usuarios con soporte de Firebase.
     */
    val userRepository by lazy { UserRepository(database.userDao(), context) }
    
    /**
     * Proporciona acceso al repositorio de temas.
     */
    val topicRepository by lazy { TopicRepository(database.topicDao()) }
    
    /**
     * Proporciona acceso al repositorio de posturas de usuario.
     */
    val userStanceRepository by lazy { UserStanceRepository(database.userStanceDao()) }
    
    /**
     * Proporciona acceso al repositorio de debates.
     */
    val debateRepository by lazy { 
        DebateRepository(
            database.debateDao(), 
            database.argumentDao(),
            database.userDao()
        ) 
    }
    
    /**
     * Servicio de Firebase para sincronización de datos.
     */
    val firebaseService by lazy { FirebaseService(context) }
    
    /**
     * Formateador de debates a JSON.
     */
    val debateJsonFormatter by lazy { DebateJsonFormatter() }
    
    companion object {
        @Volatile
        private var INSTANCE: RepositoryProvider? = null
        
        /**
         * Obtiene la instancia del proveedor de repositorios.
         */
        fun getInstance(context: Context): RepositoryProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepositoryProvider(context).also { INSTANCE = it }
            }
        }
    }
}