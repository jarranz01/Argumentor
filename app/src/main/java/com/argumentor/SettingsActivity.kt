package com.argumentor

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivitySettingsBinding
import com.argumentor.fragments.SettingsFragment
import timber.log.Timber

/**
 * Actividad para gestionar la configuración del usuario.
 *
 * Esta actividad aloja el SettingsFragment que implementa PreferenceFragmentCompat
 * para proporcionar una interfaz de preferencias moderna utilizando la biblioteca de preferencias
 * de Android Jetpack.
 */
class SettingsActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)

        // Inicializar el observador para el registro del ciclo de vida
        MyObserver(lifecycle, "SettingsActivity")

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
}