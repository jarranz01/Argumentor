package com.argumentor

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber
import java.util.Locale

/**
 * Actividad base que proporciona soporte para el cambio de idioma.
 * Todas las actividades de la aplicación deben extender esta clase para asegurar
 * que el idioma configurado se aplique correctamente.
 */
open class BaseLocaleActivity : AppCompatActivity() {

    /**
     * Método que se llama al adjuntar el contexto base a la actividad.
     * Implementa la aplicación correcta del idioma almacenado en preferencias.
     */
    override fun attachBaseContext(newBase: Context) {
        // Leer la configuración de idioma guardada
        val preferences = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", Locale.getDefault().language) 
            ?: Locale.getDefault().language
        
        try {
            // Configurar la localización
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            // Crear configuración con el idioma seleccionado
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            
            // Crear un contexto con la nueva configuración
            val updatedContext = newBase.createConfigurationContext(config)
            
            // Llamar al método de la clase base con el contexto actualizado
            super.attachBaseContext(updatedContext)
            
            Timber.d("Idioma $languageCode aplicado en ${this.javaClass.simpleName}")
        } catch (e: Exception) {
            Timber.e(e, "Error en attachBaseContext de ${this.javaClass.simpleName}: ${e.message}")
            super.attachBaseContext(newBase)
        }
    }
    
    /**
     * Aplica la configuración de idioma guardada en las preferencias.
     * Este método debe ser llamado al inicio del onCreate, antes del super.onCreate
     */
    protected open fun applyStoredLanguageConfiguration() {
        val preferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", Locale.getDefault().language) 
            ?: Locale.getDefault().language
        
        try {
            // Configurar la localización
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            // Crear una nueva configuración
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            
            // Aplicar la configuración utilizando sólo el método moderno
            createConfigurationContext(config)
            
            Timber.d("Configuración de idioma $languageCode aplicada en ${this.javaClass.simpleName}")
        } catch (e: Exception) {
            Timber.e(e, "Error al aplicar configuración de idioma en ${this.javaClass.simpleName}")
        }
    }
} 