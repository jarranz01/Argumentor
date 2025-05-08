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
     */
    override fun attachBaseContext(newBase: Context) {
        val languageCode = getLanguageCodeFromPrefs(newBase)
        val updatedContext = applyLocale(newBase, languageCode)
        super.attachBaseContext(updatedContext)
    }
    
    /**
     * Aplica la configuración de idioma guardada. Llamar al inicio del onCreate.
     */
    protected open fun applyStoredLanguageConfiguration() {
        val languageCode = getLanguageCodeFromPrefs(this)
        applyLocale(this, languageCode)
    }
    
    /**
     * Obtiene el código de idioma guardado en preferencias.
     */
    private fun getLanguageCodeFromPrefs(context: Context): String {
        val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return preferences.getString("language", Locale.getDefault().language) 
            ?: Locale.getDefault().language
    }
    
    /**
     * Aplica la configuración de localización al contexto.
     */
    private fun applyLocale(context: Context, languageCode: String): Context {
        return try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            
            val updatedContext = context.createConfigurationContext(config)
            Timber.d("Idioma $languageCode aplicado en ${this.javaClass.simpleName}")
            
            updatedContext
        } catch (e: Exception) {
            Timber.e(e, "Error al aplicar idioma en ${this.javaClass.simpleName}: ${e.message}")
            context
        }
    }
} 