package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.databinding.ActivityLoginBinding
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

/**
 * Actividad que maneja el inicio de sesión de usuarios.
 * 
 * Permite a los usuarios ingresar con su email y contraseña usando Firebase Authentication,
 * o navegar a la pantalla de registro si no tienen una cuenta.
 */
class LoginActivity : BaseLocaleActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var observer: MyObserver
    private lateinit var sessionManager: SessionManager
    private lateinit var repositoryProvider: RepositoryProvider
    
    // Acceso directo a FirebaseAuth
    private var auth: FirebaseAuth? = null

    /**
     * Método llamado al crear la actividad. Inicializa el Data Binding y configura los listeners.
     * 
     * @param savedInstanceState Estado previo de la actividad si está siendo recreada.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        observer = MyObserver(this.lifecycle, "LoginActivity")
        
        Timber.d("LoginActivity.onCreate() - Inicializando Firebase...")
        
        // Inicializar Firebase directamente aquí para la autenticación
        forcefullyInitializeFirebase()
        
        // Inicializar componentes
        sessionManager = SessionManager(this)
        repositoryProvider = RepositoryProvider.getInstance(this)
        
        // Verificar si hay sesión activa
        if (sessionManager.isLoggedIn()) {
            navigateToHome()
            return
        }
        
        // Vincular Data Binding con la vista
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        // Navegar al RegisterActivity
        binding.buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to register screen")
        }

        // Manejar el inicio de sesión con Firebase
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.empty_fields_error),
                    Toast.LENGTH_SHORT
                ).show()
                Timber.i("Login validation failed: empty fields")
                return@setOnClickListener
            }
            
            // Mostrar indicador de carga
            showLoading(true)
            
            // Verificar que auth esté inicializado
            if (auth == null) {
                forcefullyInitializeFirebase()
                if (auth == null) {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Error: Firebase no está inicializado. Reinicia la app.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }
            
            // Probar a iniciar sesión directamente primero
            try {
                Timber.d("Intentando iniciar sesión directamente con Firebase Auth")
                auth?.signInWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth?.currentUser
                            Timber.d("Inicio de sesión exitoso directamente con Firebase Auth")
                            
                            // Intentar obtener usuario desde UserRepository
                            loginWithRepository(email, password)
                        } else {
                            Timber.e(task.exception, "Error en inicio de sesión directo con Firebase Auth")
                            
                            // Intentar iniciar sesión a través del repositorio como respaldo
                            loginWithRepository(email, password)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error crítico al intentar iniciar sesión directamente: ${e.message}")
                
                // Intenta el método tradicional como respaldo
                loginWithRepository(email, password)
            }
        }
    }
    
    /**
     * Intenta iniciar sesión utilizando el repositorio
     */
    private fun loginWithRepository(email: String, password: String) {
        Timber.d("Intentando iniciar sesión a través del repositorio")
        
        // Intentar inicio de sesión con Firebase a través del repositorio
        repositoryProvider.userRepository.loginWithFirebase(
            email = email,
            password = password,
            onSuccess = { user ->
                // Guardar datos de sesión con el nombre de usuario específico
                sessionManager.createSession(user.userId, user.username.ifEmpty { user.name })
                
                // Sincronizar datos desde Firebase después de iniciar sesión
                try {
                    repositoryProvider.firebaseService.syncFromFirestore()
                    Timber.d("Sincronización iniciada después del login")
                } catch (e: Exception) {
                    Timber.e(e, "Error al iniciar sincronización después del login")
                }
                
                showLoading(false)
                Timber.i("Login successful with Firebase: ${user.email}")
                Toast.makeText(
                    this,
                    getString(R.string.login_successful),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Esperar un momento para permitir que la sincronización comience
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToHome()
                }, 500) // Pequeño retraso para permitir que la sincronización inicie
            },
            onError = { exception ->
                showLoading(false)
                
                Timber.e(exception, "Firebase auth error: ${exception.message}")
                Log.e("LoginActivity", "Firebase auth error completo:", exception)
                
                val errorMessage = when (exception.message) {
                    "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                        getString(R.string.firebase_error_user_not_found)
                    "The password is invalid or the user does not have a password." -> 
                        getString(R.string.firebase_error_wrong_password)
                    "The email address is badly formatted." -> 
                        getString(R.string.firebase_error_invalid_email)
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                        getString(R.string.firebase_error_network)
                    else -> {
                        Timber.e(exception, "Error específico de Firebase: ${exception.message}")
                        "Error: ${exception.message}"
                    }
                }
                
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
                Timber.e(exception, "Login failed with Firebase")
            }
        )
    }
    
    /**
     * Inicializa Firebase de forma forzada para asegurar que esté disponible
     */
    private fun forcefullyInitializeFirebase() {
        Timber.d("Inicializando Firebase forzosamente en LoginActivity")
        try {
            // Intentar primero a través de la aplicación
            try {
                val appInstance = ArgumentorApplication.getInstance()
                auth = appInstance.firebaseAuth
                Timber.d("Firebase Auth obtenido de ArgumentorApplication: $auth")
            } catch (e: Exception) {
                Timber.e(e, "Error al obtener Firebase Auth de la aplicación")
            }
            
            // Si aún no está inicializado, hacerlo directamente
            if (auth == null) {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this)
                }
                auth = FirebaseAuth.getInstance()
                Timber.d("Firebase Auth inicializado directamente: $auth")
            }
            
            // Verificar si auth es null
            if (auth == null) {
                Timber.e("Firebase Auth sigue siendo null después de inicialización")
                Toast.makeText(
                    this,
                    "Error crítico al inicializar Firebase. Por favor reinstala la aplicación.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error crítico al inicializar Firebase en LoginActivity")
            Toast.makeText(
                this,
                "Error crítico: ${e.message}. Por favor reinstala la aplicación.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Navega a la pantalla principal y finaliza esta actividad.
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Evita volver atrás al login
    }
    
    /**
     * Muestra u oculta el indicador de carga y deshabilita/habilita los controles de interfaz.
     * 
     * @param isLoading true para mostrar el indicador de carga, false para ocultarlo
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGoToRegister.isEnabled = !isLoading
        binding.editTextUsername.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
    }
}