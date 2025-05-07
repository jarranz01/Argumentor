package com.argumentor.database

import android.content.Context
import com.argumentor.database.repositories.*
import com.argumentor.database.utils.DebateJsonFormatter
import com.argumentor.models.ArgumentorApplication
import com.argumentor.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Proveedor de repositorios para la aplicación.
 * 
 * Esta clase sigue el patrón Singleton y proporciona acceso centralizado a todos los repositorios
 * de datos de la aplicación.
 */
class RepositoryProvider private constructor(private val context: Context) {
    
    // Coroutine scope para la base de datos
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob())
    
    // Base de datos
    private val database: AppDatabase = AppDatabase.getDatabase(context, applicationScope)
    
    // Repositorios
    val userRepository: UserRepository by lazy { 
        UserRepository(database.userDao(), context) 
    }
    
    val debateRepository: DebateRepository by lazy { 
        DebateRepository(
            database.debateDao(),
            database.argumentDao(),
            database.userDao()
        )
    }
    
    val topicRepository: TopicRepository by lazy { 
        TopicRepository(database.topicDao()) 
    }
    
    val userStanceRepository: UserStanceRepository by lazy { 
        UserStanceRepository(database.userStanceDao()) 
    }
    
    // Servicio de Firebase para sincronización
    val firebaseService: FirebaseService by lazy { 
        FirebaseService(context) 
    }
    
    /**
     * Formateador de debates a JSON.
     */
    val debateJsonFormatter: DebateJsonFormatter by lazy { 
        DebateJsonFormatter() 
    }
    
    companion object {
        @Volatile
        private var INSTANCE: RepositoryProvider? = null
        
        /**
         * Obtiene la instancia del proveedor de repositorios o la crea si no existe.
         * 
         * @param context Contexto de la aplicación
         * @return Instancia del proveedor de repositorios
         */
        fun getInstance(context: Context): RepositoryProvider {
            return INSTANCE ?: synchronized(this) {
                val instance = RepositoryProvider(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}