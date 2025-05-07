package com.argumentor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityHomeBinding
import timber.log.Timber

/**
 * Actividad principal que sirve como hub de navegación para las diferentes funcionalidades de la app.
 * 
 * Esta actividad muestra los botones principales para acceder a:
 * - Emparejamiento (Matchmaking)
 * - Tablero de debates (Debate Board)
 * - Mis debates (My Debates)
 * - Mis posturas (My Stances)
 * - Ajustes (Settings)
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var observer: MyObserver
    private lateinit var sessionManager: SessionManager

    /**
     * Método llamado al crear la actividad. Inicializa el Data Binding y configura los listeners.
     * 
     * @param savedInstanceState Estado previo de la actividad si está siendo recreada.
     */
    override fun onCreate(savedInstanceState: Bundle?) {        
        super.onCreate(savedInstanceState)
        
        observer = MyObserver(
            lifecycle,
            "HomeActivity",
            this
        )
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        
        // Inicializar sessionManager
        sessionManager = SessionManager(this)
        
        // Configurar el mensaje de bienvenida personalizado
        setupWelcomeMessage()
        
        // Configurar listeners para los botones
        setupClickListeners()
    }
    
    /**
     * Configura el mensaje de bienvenida personalizado con el nombre del usuario.
     */
    private fun setupWelcomeMessage() {
        val username = sessionManager.getUsername()
        
        if (username != null) {
            // Mostrar mensaje con nombre de usuario
            val welcomeMessage = getString(R.string.welcome_message_with_name, username)
            binding.tvWelcome.text = welcomeMessage
            Timber.d("Mostrando mensaje de bienvenida para el usuario: $username")
        } else {
            // Si no hay sesión activa, mostrar mensaje genérico
            binding.tvWelcome.text = getString(R.string.welcome_message)
            Timber.d("Usuario no encontrado, mostrando mensaje de bienvenida genérico")
        }
    }

    /**
     * Configura los listeners para los botones de la interfaz.
     * 
     * Cada botón maneja la navegación a una actividad diferente o muestra un mensaje
     * si la funcionalidad no está implementada aún.
     */
    private fun setupClickListeners() {
        binding.btnMatchmaking.setOnClickListener {
            // To-do: Navegación a pantalla de emparejamiento
            Timber.i("Matchmaking clicked - Not implemented yet")
        }

        binding.btnDebateBoard.setOnClickListener {
            val intent = Intent(this, DebateBoardActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to debate board")
        }

        binding.btnMyDebates.setOnClickListener {
            // To-do: Navegación a pantalla de emparejamiento
            Timber.i("My Debates clicked - Not implemented yet")
        }

        binding.btnMyStances.setOnClickListener {
            val intent = Intent(this, FormularioActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to my stances")
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to settings")
        }
    }
}