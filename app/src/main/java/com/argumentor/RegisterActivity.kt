package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.databinding.ActivityRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Actividad que maneja el registro de nuevos usuarios.
 * 
 * Permite a los usuarios crear una nueva cuenta proporcionando:
 * - Nombre completo
 * - Email
 * - Nombre de usuario
 * - Contraseña
 * 
 * Utiliza Firebase Authentication para el registro de usuarios.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
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

        observer = MyObserver(
            lifecycle,
            "RegisterActivity",
            this
        )

        // Inicializar componentes
        sessionManager = SessionManager(this)
        repositoryProvider = RepositoryProvider.getInstance(this)
        
        // Inicializar Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)

        // Manejar clic en el botón "Ir a Login"
        binding.buttonToLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            Timber.i("Go to login menu")
        }

        // Manejar clic en el botón de registro
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val username = binding.editTextRegisterUsername.text.toString().trim()
            val password = binding.editTextRegisterPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show()
                Timber.i("Empty fields")
            } else {
                // Registrar usuario con Firebase
                registerUser(name, email, username, password)
            }
        }
    }

    /**
     * Registra un nuevo usuario con Firebase Authentication y
     * guarda sus datos en la base de datos local y en Firestore.
     */
    private fun registerUser(name: String, email: String, username: String, password: String) {
        try {
            // Validación básica
            if (password.length < 6) {
                Toast.makeText(
                    this,
                    getString(R.string.firebase_error_weak_password),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            
            // Verificar formato de email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                    this,
                    getString(R.string.firebase_error_invalid_email),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            
            // Mostrar indicador de carga
            showLoading(true)
            
            // Log para diagnóstico
            Timber.d("Iniciando registro de usuario: $username / $email")

            // Registrar con Firebase Auth a través del repositorio de usuarios
            repositoryProvider.userRepository.registerUserWithFirebase(
                username = username,
                password = password,
                email = email,
                name = name,
                onSuccess = { user ->
                    // Actualizar session manager
                    sessionManager.createSession(user.userId, username)
                    
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.registration_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navegar a la pantalla de formulario
                    val intent = Intent(this@RegisterActivity, FormularioActivity::class.java)
                    startActivity(intent)
                    finish()
                    
                    Timber.i("User registered successfully with ID: ${user.userId}")
                },
                onError = { exception ->
                    showLoading(false)
                    
                    // Log detallado del error para diagnóstico
                    Timber.e(exception, "Firebase registration failed with message: ${exception.message}")
                    
                    val errorMessage = when {
                        // Mensajes específicos de Firebase Auth
                        exception.message?.contains("email address is already in use", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_email_already_in_use)
                        exception.message?.contains("badly formatted", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_invalid_email)
                        exception.message?.contains("password is invalid", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_weak_password)
                        exception.message?.contains("network error", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_network)
                        exception.message?.contains("timeout", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_network)
                        exception.message?.contains("interrupted connection", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_network)
                        exception.message?.contains("unreachable host", ignoreCase = true) == true -> 
                            getString(R.string.firebase_error_network)
                        exception.message?.contains("Firebase no está inicializado", ignoreCase = true) == true -> 
                            "Error de inicialización. Por favor reinicia la aplicación."
                        else -> {
                            // Si recibimos un error desconocido, mostrar el mensaje real para diagnóstico
                            val detail = exception.message ?: "Error desconocido"
                            "Error: $detail"
                        }
                    }
                    
                    Toast.makeText(
                        this@RegisterActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        } catch (e: Exception) {
            // Capturar cualquier error inesperado en la UI
            showLoading(false)
            Timber.e(e, "Error inesperado al intentar registrar usuario")
            Toast.makeText(
                this,
                "Error inesperado: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Muestra u oculta el indicador de carga y habilita/deshabilita los controles de la interfaz.
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
        binding.buttonToLogIn.isEnabled = !isLoading
        binding.editTextName.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextRegisterUsername.isEnabled = !isLoading
        binding.editTextRegisterPassword.isEnabled = !isLoading
    }

    override fun onStart() {
        super.onStart()
        Timber.i("onStart called")
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume called")
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause called")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy called")
    }
}