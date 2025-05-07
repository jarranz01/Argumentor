package com.argumentor

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivitySettingsBinding
import timber.log.Timber
import java.util.Locale

/**
 * Actividad para gestionar la configuración del usuario.
 *
 * Esta actividad proporciona opciones para cambiar el idioma de la aplicación (inglés/español)
 * y cerrar sesión en la sesión actual.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var observer: MyObserver
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el observador para el registro del ciclo de vida
        observer = MyObserver(lifecycle, "SettingsActivity", this)

        // Inicializar el gestor de sesión para manejar el cierre de sesión
        sessionManager = SessionManager(this)

        // Configurar el enlace de datos (data binding)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        // Configurar la barra de herramientas con el botón de retroceso
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Establecer la selección inicial basada en la configuración actual del idioma
        setupLanguageSelection()

        // Configurar el botón de cierre de sesión
        setupLogoutButton()
    }

    /**
     * Configura el grupo de botones de radio para la selección de idioma.
     */
    private fun setupLanguageSelection() {
        // Obtener la configuración regional actual para establecer el botón de radio correcto
        val currentLanguage = getCurrentLanguage()

        // Seleccionar el botón de radio apropiado según el idioma actual
        if (currentLanguage.startsWith("es")) {
            binding.radioSpanish.isChecked = true
        } else {
            // Por defecto, inglés para cualquier otro idioma
            binding.radioEnglish.isChecked = true
        }

        // Establecer el listener para los cambios de idioma
        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val languageCode = when (checkedId) {
                R.id.radioSpanish -> "es"
                else -> "en"
            }

            // Guardar la preferencia de idioma seleccionada
            changeLanguage(languageCode)

            // Notificar al usuario
            Toast.makeText(
                this,
                getString(R.string.language_changed),
                Toast.LENGTH_LONG
            ).show()

            Timber.d("Preferencia de idioma cambiada a: $languageCode")

            // Reiniciar la aplicación para aplicar los cambios de idioma correctamente
            restartApp()
        }
    }

    /**
     * Configura el botón de cierre de sesión.
     */
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Cerrar sesión usando el gestor de sesión
            sessionManager.logout()

            // Mostrar mensaje de confirmación
            Toast.makeText(
                this,
                getString(R.string.logout_success),
                Toast.LENGTH_SHORT
            ).show()

            Timber.i("Usuario cerró sesión")

            // Navegar a la actividad de inicio de sesión y limpiar la pila de actividades
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    /**
     * Cambia el idioma de la aplicación.
     *
     * @param languageCode El código de idioma ISO a establecer (por ejemplo, "en" o "es")
     */
    private fun changeLanguage(languageCode: String) {
        // Guardar el idioma seleccionado en las preferencias compartidas
        val preferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        preferences.edit().apply {
            putString("language", languageCode)
            apply()
        }

        // Aplicar la nueva configuración regional
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        // Usar createConfigurationContext en lugar de updateConfiguration
        createConfigurationContext(config)
    }

    /**
     * Reinicia la aplicación para aplicar los cambios de idioma.
     */
    private fun restartApp() {
        // Crear un intent para reiniciar la actividad principal
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Obtiene el código de idioma actual de las preferencias compartidas o del sistema por defecto.
     *
     * @return El código de idioma ISO (por ejemplo, "en" o "es")
     */
    private fun getCurrentLanguage(): String {
        val preferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return preferences.getString("language", Locale.getDefault().language) ?: "en"
    }
}