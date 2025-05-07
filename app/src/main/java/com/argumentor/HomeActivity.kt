package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.databinding.ActivityHomeBinding
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private lateinit var repositoryProvider: RepositoryProvider

    /**
     * Método llamado al crear la actividad. Inicializa el Data Binding y configura los listeners.
     * 
     * @param savedInstanceState Estado previo de la actividad si está siendo recreada.
     */
    override fun onCreate(savedInstanceState: Bundle?) {        
        super.onCreate(savedInstanceState)
        
        // Inicializar el observador de ciclo de vida
        observer = MyObserver(lifecycle, "HomeActivity", this)
        
        // Verificar si hay sesión activa
        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        // Inicializar el repositorio
        repositoryProvider = RepositoryProvider.getInstance(this)
        
        // Sincronizar datos con Firebase
        syncDataWithFirebase()
        
        // Inicializar Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        
        // Configurar la UI
        setupUI()
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
            val intent = Intent(this, MatchmakingActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to matchmaking")
        }

        binding.btnDebateBoard.setOnClickListener {
            val intent = Intent(this, DebateBoardActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to debate board")
        }

        binding.btnMyDebates.setOnClickListener {
            val intent = Intent(this, MyDebatesActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to my debates")
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

    /**
     * Sincroniza los datos locales con Firebase.
     * Esto asegura que los datos estén actualizados al iniciar la aplicación.
     */
    private fun syncDataWithFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar si el usuario está autenticado
                val firebaseUser = repositoryProvider.firebaseService.auth.currentUser
                if (firebaseUser == null) {
                    Timber.e("Firebase Auth: Usuario no autenticado, la sincronización será cancelada")
                    runOnUiThread {
                        Toast.makeText(
                            this@HomeActivity,
                            "Error de sincronización: No hay usuario autenticado en Firebase",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                } else {
                    Timber.d("Firebase Auth: Usuario autenticado correctamente - ${firebaseUser.uid}")
                }
                
                // Sincronizar datos desde Firebase a la base de datos local
                repositoryProvider.firebaseService.syncFromFirestore()
                
                // También enviar datos locales a Firebase
                repositoryProvider.firebaseService.syncAllData()
                
                Timber.d("Sincronización con Firebase completada en HomeActivity")
                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        "Sincronización con Firebase completada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al sincronizar con Firebase en HomeActivity")
                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        "Error al sincronizar con Firebase: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupUI() {
        // Configurar el mensaje de bienvenida personalizado
        setupWelcomeMessage()
        
        // Configurar listeners para los botones
        setupClickListeners()
    }

    private fun navigateToLogin() {
        // Implementa la navegación a la actividad de inicio de sesión
        // Por ejemplo, usando un Intent
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}