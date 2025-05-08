package com.argumentor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivitySettingsBinding
import com.argumentor.fragments.SettingsFragment
import timber.log.Timber
import java.util.Locale

/**
 * Actividad para gestionar la configuración del usuario.
 *
 * Esta actividad aloja el SettingsFragment que implementa PreferenceFragmentCompat
 * para proporcionar una interfaz de preferencias moderna utilizando la biblioteca de preferencias
 * de Android Jetpack.
 */
class SettingsActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var observer: MyObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)

        // Inicializar el observador para el registro del ciclo de vida
        observer = MyObserver(lifecycle, "SettingsActivity", this)

        // Configurar el enlace de datos (data binding)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        // Configurar la barra de herramientas con el botón de retroceso
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Cargar el fragmento de preferencias si no se ha cargado previamente
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        
        Timber.d("SettingsActivity creada con SettingsFragment")
    }
    
    /**
     * Método que se llama al adjuntar el contexto base a la actividad.
     * Es el mejor lugar para aplicar configuraciones de localización.
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
            
            Timber.d("Idioma aplicado en attachBaseContext: $languageCode")
        } catch (e: Exception) {
            Timber.e(e, "Error en attachBaseContext: ${e.message}")
            super.attachBaseContext(newBase)
        }
    }
    
    /**
     * Aplica la configuración de idioma guardada en las preferencias.
     */
    override protected fun applyStoredLanguageConfiguration() {
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
            
            // Aplicar la configuración utilizando sólo el método moderno (no-deprecated)
            // Esto crea un contexto con la nueva configuración
            val context = createConfigurationContext(config)
            
            // Nota: No usamos updateConfiguration porque está deprecado
            // La configuración de idioma se aplicará totalmente cuando se reinicie la aplicación
            
            Timber.d("Configuración de idioma aplicada en SettingsActivity: $languageCode")
        } catch (e: Exception) {
            Timber.e(e, "Error al aplicar configuración de idioma")
        }
    }
}