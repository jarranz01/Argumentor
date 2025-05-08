package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityHomeBinding
import timber.log.Timber

/**
 * Actividad principal de la aplicación que se muestra después del inicio de sesión.
 *
 * Proporciona acceso a las principales funcionalidades de la aplicación como:
 * - Iniciar un emparejamiento para un debate
 * - Ver los debates en curso
 * - Acceder a la configuración
 * - Cerrar sesión
 */
class HomeActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        
        // Registrar observador para logs del ciclo de vida
        MyObserver(lifecycle, "HomeActivity")
        
        // Inicializar session manager
        sessionManager = SessionManager(this)
        
        // Verificar si el usuario ha iniciado sesión
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        // Configurar data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        
        // Configurar los botones
        setupButtons()
        
        // Configurar el mensaje de bienvenida
        setupWelcomeMessage()
        
        Timber.i("HomeActivity creada para el usuario: ${sessionManager.getUsername() ?: "desconocido"}")
    }

    /**
     * Configura los botones de la pantalla principal
     */
    private fun setupButtons() {
        // Definir un mapa de botones y sus destinos
        val navigationMap = mapOf(
            binding.btnMatchmaking to MatchmakingActivity::class.java,
            binding.btnMyDebates to MyDebatesActivity::class.java,
            binding.btnDebateBoard to DebateBoardActivity::class.java,
            binding.btnMyStances to FormularioActivity::class.java
        )
        
        // Configurar cada botón
        navigationMap.forEach { (button, destination) ->
            button.setOnClickListener {
                startActivity(Intent(this, destination))
                Timber.i("Navegando a ${destination.simpleName}")
            }
        }
        
        // Botón de ajustes con manejo especial
        binding.btnSettings.setOnClickListener {
            navigateToSettings()
        }
    }

    /**
     * Configura el mensaje de bienvenida con el nombre del usuario
     */
    private fun setupWelcomeMessage() {
        val userName = sessionManager.getUsername()
        binding.tvWelcome.text = if (!userName.isNullOrEmpty()) {
            getString(R.string.welcome_message_with_name, userName)
        } else {
            getString(R.string.welcome_message)
        }
    }

    /**
     * Navega a la pantalla de inicio de sesión
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Timber.i("Navegando a LoginActivity (sesión no iniciada)")
    }

    /**
     * Navega a la pantalla de configuración
     */
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        Timber.i("Navegando a SettingsActivity")
    }

    /**
     * Cierra la sesión del usuario y navega a la pantalla de inicio de sesión
     */
    private fun logout() {
        // Cierra la sesión
        sessionManager.logout()
        
        // Muestra un mensaje de despedida
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()
        
        // Navega a la pantalla de inicio de sesión
        navigateToLogin()
        
        Timber.i("Sesión cerrada correctamente")
    }
}