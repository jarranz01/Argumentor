package com.argumentor

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import timber.log.Timber
import java.util.*

/**
 * ESTA CLASE ESTÁ DESHABILITADA. 
 * Se mantiene aquí solo por referencia pero no se usa.
 * La implementación activa está en com.argumentor.models.ArgumentorApplication
 *
 * Clase de aplicación personalizada para Argumentor.
 *
 * Se utiliza para inicializar configuraciones globales como el idioma de la aplicación
 * y el registro con Timber.
 */
class ArgumentorApplicationDISABLED : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Timber para el registro
        Timber.plant(Timber.DebugTree())

        // Aplicar el idioma guardado
        // applyLanguageFromSettings()
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
        
        // Crear un nuevo contexto con la configuración actualizada
        createConfigurationContext(config)
        
        // No es necesario llamar a updateConfiguration, que está obsoleto

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