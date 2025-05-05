package com.argumentor.models

import android.app.Application
import android.util.Log
import com.argumentor.database.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

/**
 * Clase base de la aplicación que inicializa componentes globales.
 * 
 * Configura Timber para el logging de mensajes de depuración en builds de desarrollo.
 * Inicializa la base de datos Room para persistencia de datos.
 * Inicializa Firebase para autenticación y sincronización de datos.
 */
class ArgumentorApplication : Application() {

    // CoroutineScope para operaciones de la aplicación
    val applicationScope = CoroutineScope(SupervisorJob())
    
    // Instancia lazy de la base de datos
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    
    // Instancias de Firebase
    private var _firebaseAuth: FirebaseAuth? = null
    val firebaseAuth: FirebaseAuth
        get() {
            if (_firebaseAuth == null) {
                try {
                    _firebaseAuth = FirebaseAuth.getInstance()
                } catch (e: Exception) {
                    Timber.e(e, "Error al obtener instancia de FirebaseAuth")
                    // Intento alternativo de inicialización
                    initializeFirebase()
                    _firebaseAuth = FirebaseAuth.getInstance()
                }
            }
            return _firebaseAuth!!
        }
    
    private var _firestore: FirebaseFirestore? = null
    val firestore: FirebaseFirestore
        get() {
            if (_firestore == null) {
                try {
                    _firestore = FirebaseFirestore.getInstance()
                } catch (e: Exception) {
                    Timber.e(e, "Error al obtener instancia de FirebaseFirestore")
                    // Intento alternativo de inicialización
                    initializeFirebase()
                    _firestore = FirebaseFirestore.getInstance()
                }
            }
            return _firestore!!
        }
    
    companion object {
        private var instance: ArgumentorApplication? = null
        
        fun getInstance(): ArgumentorApplication {
            return instance ?: throw IllegalStateException("ArgumentorApplication no ha sido inicializada")
        }
    }
    
    /**
     * Inicializa Firebase de manera segura, con manejo de excepciones extensivo
     */
    private fun initializeFirebase() {
        try {
            // Verificar si Firebase ya está inicializado
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Inicialización normal
                FirebaseApp.initializeApp(this)
                Timber.d("Firebase inicializado correctamente")
            } else {
                // Recuperar la instancia existente
                FirebaseApp.getInstance()
                Timber.d("Usando instancia existente de Firebase")
            }
        } catch (e: Exception) {
            // Log de error detallado para diagnóstico
            Timber.e(e, "Error al inicializar Firebase: ${e.message}")
            
            // Log adicional para Android con el mensaje completo del error
            Log.e("ArgumentorApp", "Error de inicialización de Firebase", e)
        }
    }
    
    /**
     * Método llamado al crear la aplicación. Configura el sistema de logging
     * e inicializa componentes necesarios.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Inicializar Timber para logging
        Timber.plant(Timber.DebugTree())
        
        // Inicializar Firebase de manera segura
        try {
            initializeFirebase()
            
            // Verificación adicional de inicialización correcta
            val firebaseApp = FirebaseApp.getInstance()
            Timber.d("Firebase App verificada: ${firebaseApp.name}")
            
        } catch (e: Exception) {
            Timber.e(e, "Error crítico en la inicialización de Firebase")
        }
        
        Timber.d("ArgumentorApplication inicializada")
    }
}