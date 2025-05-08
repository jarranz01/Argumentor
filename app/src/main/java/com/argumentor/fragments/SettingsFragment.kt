package com.argumentor.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.argumentor.HomeActivity
import com.argumentor.LoginActivity
import com.argumentor.R
import com.argumentor.SessionManager
import timber.log.Timber
import java.util.Locale

/**
 * Fragmento para gestionar las preferencias del usuario con PreferenceFragmentCompat.
 * Implementa la interfaz moderna de preferencias de Android Jetpack.
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sessionManager: SessionManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        try {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            sessionManager = SessionManager(requireContext())
            
            // Configurar preferencia de idioma
            try {
                val languagePref = findPreference<ListPreference>("language")
                languagePref?.apply {
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error configurando preferencia de idioma")
            }
            
            // Configurar preferencia de logout
            try {
                val logoutPref = findPreference<Preference>("logout")
                logoutPref?.setOnPreferenceClickListener {
                    try {
                        // Cerrar sesión
                        sessionManager.logout()
                        
                        // Mostrar mensaje
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.logout_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        Timber.i("Usuario cerró sesión desde SettingsFragment")
                        
                        // Navegar a Login
                        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        activity?.finish()
                        
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Error en logout")
                        false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error configurando preferencia de logout")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en onCreatePreferences")
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            Timber.e(e, "Error en onResume")
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            Timber.e(e, "Error en onPause")
        }
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        try {
            if (key == "language") {
                val languagePref = findPreference<ListPreference>("language")
                if (languagePref != null) {
                    // Aplicar el cambio de idioma
                    val languageCode = languagePref.value
                    if (!languageCode.isNullOrEmpty()) {
                        changeLanguage(languageCode)
                        
                        // Notificar al usuario
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.language_changed),
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Reiniciar la app para aplicar los cambios
                        restartApp()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en onSharedPreferenceChanged")
        }
    }
    
    /**
     * Cambia el idioma de la aplicación.
     */
    private fun changeLanguage(languageCode: String) {
        try {
            // Guardar el idioma seleccionado en las preferencias compartidas
            val preferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            preferences.edit().apply {
                putString("language", languageCode)
                apply()
            }
            
            // Aplicar la nueva configuración regional
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            // Crear una nueva configuración con el idioma seleccionado
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            
            // Aplicar la configuración - método moderno
            // La configuración solo se aplicará efectivamente cuando se reinicie la aplicación
            // Crear nuevo contexto (esto es lo recomendado en lugar de updateConfiguration)
            requireContext().createConfigurationContext(config)
            
            Timber.d("Idioma cambiado a $languageCode en SettingsFragment")
        } catch (e: Exception) {
            Timber.e(e, "Error cambiando idioma")
        }
    }
    
    /**
     * Reinicia la aplicación para aplicar los cambios de idioma.
     */
    private fun restartApp() {
        try {
            // Crear un intent para reiniciar la actividad principal
            val intent = Intent(requireContext(), HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Timber.e(e, "Error al reiniciar la app")
        }
    }
} 