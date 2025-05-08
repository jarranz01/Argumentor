package com.argumentor.models

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.argumentor.database.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

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
    
    // Instancias de Firebase - inicialización directa
    private var _firebaseAuth: FirebaseAuth? = null
    val firebaseAuth: FirebaseAuth
        get() {
            return _firebaseAuth ?: synchronized(this) {
                if (_firebaseAuth == null) {
                    try {
                        initializeFirebase()
                        _firebaseAuth = FirebaseAuth.getInstance()
                    } catch (e: Exception) {
                        Timber.e(e, "Error obteniendo FirebaseAuth")
                        showFirebaseError("Error: ${e.message}")
                        throw e
                    }
                }
                _firebaseAuth!!
            }
        }
    
    private var _firestore: FirebaseFirestore? = null
    val firestore: FirebaseFirestore
        get() {
            return _firestore ?: synchronized(this) {
                if (_firestore == null) {
                    try {
                        initializeFirebase()
                        _firestore = FirebaseFirestore.getInstance()
                    } catch (e: Exception) {
                        Timber.e(e, "Error obteniendo FirebaseFirestore")
                        showFirebaseError("Error: ${e.message}")
                        throw e
                    }
                }
                _firestore!!
            }
        }
    
    companion object {
        private var instance: ArgumentorApplication? = null
        
        fun getInstance(): ArgumentorApplication {
            return instance ?: throw IllegalStateException("ArgumentorApplication no inicializada")
        }
        
        // Constantes para preferencias
        private const val PREF_NAME = "app_settings"
        private const val PREF_LANGUAGE = "language"
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
        
        Timber.d("ArgumentorApplication.onCreate()")
        
        initFirebaseWithRetry()
        applyLanguageFromSettings()
        
        Timber.d("ArgumentorApplication inicializada")
    }
    
    private fun initFirebaseWithRetry() {
        try {
            // Inicializar en el hilo principal
            initializeFirebase()
            
            // También inicializar en background para tareas adicionales
            applicationScope.launch(Dispatchers.IO) {
                try {
                    initializeFirebase()
                    
                    _firebaseAuth = FirebaseAuth.getInstance()
                    _firestore = FirebaseFirestore.getInstance()
                    
                    if (_firebaseAuth != null && _firestore != null) {
                        // Iniciar sincronización periódica con Firebase
                        try {
                            val firebaseService = com.argumentor.services.FirebaseService(applicationContext)
                            firebaseService.startPeriodicSync()
                            Timber.d("Sincronización periódica iniciada")
                        } catch (e: Exception) {
                            Timber.e(e, "Error iniciando sincronización periódica")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error inicializando Firebase en background")
                }
            }
            
            // Verificación de inicialización
            FirebaseApp.getInstance()
            Timber.d("Firebase inicializado correctamente")
        } catch (e: Exception) {
            Timber.e(e, "Error crítico inicializando Firebase")
            showFirebaseError("Error crítico: ${e.message}")
        }
    }
    
    /**
     * Inicializa Firebase de manera segura, con manejo de excepciones extensivo
     */
    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Primera inicialización
                FirebaseApp.initializeApp(this)?.apply {
                    setAutomaticResourceManagementEnabled(true)
                }
            } else {
                // Ya inicializado
                FirebaseApp.getInstance()?.apply {
                    setAutomaticResourceManagementEnabled(true)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error inicializando Firebase")
            
            // Intento alternativo con applicationContext
            try {
                FirebaseApp.initializeApp(applicationContext)
                Timber.d("Firebase inicializado con applicationContext alternativo")
            } catch (e2: Exception) {
                Timber.e(e2, "Falló el segundo intento de inicialización")
                throw e2 // Propagar el error
            }
        }
    }
    
    /**
     * Muestra un error de Firebase en la UI
     */
    private fun showFirebaseError(message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    applicationContext,
                    "Error: $message. Por favor reinicia la app.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("ArgumentorApp", "No se pudo mostrar mensaje de error", e)
            }
        }
    }
    
    /**
     * Aplica el idioma guardado en las preferencias compartidas.
     * Si no hay un idioma guardado, utiliza el idioma del sistema.
     */
    private fun applyLanguageFromSettings() {
        val languageCode = getLanguageFromPrefs()
        Timber.d("Aplicando idioma: $languageCode")
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        createConfigurationContext(config)
    }
    
    private fun getLanguageFromPrefs(): String {
        val preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(PREF_LANGUAGE, Locale.getDefault().language) 
            ?: Locale.getDefault().language
    }

    override fun attachBaseContext(base: Context) {
        val languageCode = getLanguageFromPrefs(base)
        Timber.d("attachBaseContext: Aplicando idioma: $languageCode")
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        
        val updatedContext = base.createConfigurationContext(config)
        super.attachBaseContext(updatedContext)
    }
    
    private fun getLanguageFromPrefs(context: Context): String {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(PREF_LANGUAGE, Locale.getDefault().language) 
            ?: Locale.getDefault().language
    }
}