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
                        // Asegurar que Firebase está inicializado
                        initializeFirebase()
                        _firebaseAuth = FirebaseAuth.getInstance()
                    } catch (e: Exception) {
                        Timber.e(e, "Error al obtener instancia de FirebaseAuth")
                        showFirebaseError("Error inicializando Firebase Auth: ${e.message}")
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
                        // Asegurar que Firebase está inicializado
                        initializeFirebase()
                        _firestore = FirebaseFirestore.getInstance()
                    } catch (e: Exception) {
                        Timber.e(e, "Error al obtener instancia de FirebaseFirestore")
                        showFirebaseError("Error inicializando Firestore: ${e.message}")
                        throw e
                    }
                }
                _firestore!!
            }
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
            // Asegurarnos de que solo inicializamos una vez
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
            
            // Mostrar un mensaje al usuario
            showFirebaseError("Error de Firebase: ${e.message}")
            
            // Reintentar inicialización con un enfoque alternativo
            try {
                // Intentar crear una instancia con una opción diferente
                FirebaseApp.initializeApp(applicationContext)
                Timber.d("Firebase inicializado con applicationContext alternativo")
            } catch (e2: Exception) {
                Timber.e(e2, "Falló el segundo intento de inicialización de Firebase")
                showFirebaseError("Falló la inicialización de Firebase: ${e2.message}")
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
                Log.e("ArgumentorApp", "No se pudo mostrar un mensaje de error", e)
            }
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
        
        Timber.d("ArgumentorApplication.onCreate() - Inicializando componentes")
        
        // Inicializar Firebase de manera segura
        try {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    initializeFirebase()
                
                    // Inicializar explícitamente Auth y Firestore
                    _firebaseAuth = FirebaseAuth.getInstance()
                    _firestore = FirebaseFirestore.getInstance()
                
                    // Verificación adicional de inicialización
                    if (_firebaseAuth != null && _firestore != null) {
                        Timber.d("Firebase inicializado exitosamente en segundo plano")
                    } else {
                        Timber.e("Firebase inicializado pero las instancias son nulas")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error inicializando Firebase en segundo plano")
                }
            }
            
            // También inicializar en el hilo principal para asegurarnos
            initializeFirebase()
            
            // Verificación adicional de inicialización correcta
            val firebaseApp = FirebaseApp.getInstance()
            Timber.d("Firebase App verificada: ${firebaseApp.name}")
            
        } catch (e: Exception) {
            Timber.e(e, "Error crítico en la inicialización de Firebase")
            showFirebaseError("Error crítico en inicialización: ${e.message}")
        }
        
        // Aplicar el idioma guardado
        applyLanguageFromSettings()
        
        Timber.d("ArgumentorApplication inicializada")
    }
    
    /**
     * Aplica el idioma guardado en las preferencias compartidas.
     */
    private fun applyLanguageFromSettings() {
        val preferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", Locale.getDefault().language) ?: "en"

        // Aplicar la configuración regional
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        val context = createConfigurationContext(config)
        resources.updateConfiguration(context.resources.configuration, context.resources.displayMetrics)

        Timber.d("Idioma aplicado al iniciar la app: $languageCode")
    }

    override fun attachBaseContext(base: Context) {
        // Obtener el idioma guardado
        val preferences = base.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", Locale.getDefault().language) ?: "en"

        // Crear una configuración con el idioma seleccionado
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)

        // Crear un nuevo contexto con la configuración actualizada
        val updatedContext = base.createConfigurationContext(config)

        super.attachBaseContext(updatedContext)
    }
}