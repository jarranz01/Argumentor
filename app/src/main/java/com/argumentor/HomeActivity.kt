package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var observer: MyObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        
        // Inicializar el observador del ciclo de vida
        observer = MyObserver(lifecycle, "HomeActivity", this)
        
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Ya no inflamos el menú
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navegar a la pantalla de configuración
                navigateToSettings()
                true
            }
            R.id.action_logout -> {
                // Cerrar sesión
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Configura los botones de la pantalla principal
     */
    private fun setupButtons() {
        // Botón para iniciar emparejamiento
        binding.btnMatchmaking.setOnClickListener {
            val intent = Intent(this, MatchmakingActivity::class.java)
            startActivity(intent)
            Timber.i("Navegando a MatchmakingActivity")
        }

        // Botón para ver debates en curso
        binding.btnMyDebates.setOnClickListener {
            val intent = Intent(this, MyDebatesActivity::class.java)
            startActivity(intent)
            Timber.i("Navegando a MyDebatesActivity")
        }

        // Botón para ver tablón de debates
        binding.btnDebateBoard.setOnClickListener {
            val intent = Intent(this, DebateBoardActivity::class.java)
            startActivity(intent)
            Timber.i("Navegando a DebateBoardActivity")
        }
        
        // Botón para ver mis posturas
        binding.btnMyStances.setOnClickListener {
            val intent = Intent(this, FormularioActivity::class.java)
            startActivity(intent)
            Timber.i("Navegando a FormularioActivity (Mis posturas)")
        }
        
        // Botón para ajustes
        binding.btnSettings.setOnClickListener {
            navigateToSettings()
            Timber.i("Navegando a SettingsActivity desde botón")
        }
    }

    /**
     * Configura el mensaje de bienvenida con el nombre del usuario
     */
    private fun setupWelcomeMessage() {
        val userName = sessionManager.getUsername()
        if (!userName.isNullOrEmpty()) {
            binding.tvWelcome.text = getString(R.string.welcome_message_with_name, userName)
        } else {
            binding.tvWelcome.text = getString(R.string.welcome_message)
        }
    }

    /**
     * Navega a la pantalla de inicio de sesión
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Limpiar la pila de actividades para que el usuario no pueda volver atrás con el botón de retorno
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Timber.i("Navegando a LoginActivity (sesión no iniciada)")
    }

    /**
     * Navega a la pantalla de configuración
     */
    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
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