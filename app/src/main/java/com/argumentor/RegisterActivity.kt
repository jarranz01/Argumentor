package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.database.entities.UserEntity
import com.argumentor.databinding.ActivityRegisterBinding
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
class RegisterActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivityRegisterBinding
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

        observer = MyObserver(
            lifecycle,
            "RegisterActivity",
            this
        )

        // Inicializar componentes
        sessionManager = SessionManager(this)
        repositoryProvider = RepositoryProvider.getInstance(this)
        
        // Verificar que Firebase esté inicializado
        forcefullyInitializeFirebase()
        
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
            
            // Verificar que Firebase esté inicializado
            if (auth == null) {
                forcefullyInitializeFirebase()
                if (auth == null) {
                    Toast.makeText(
                        this,
                        "Error: Firebase no está inicializado. Reinicia la app.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
            
            // Mostrar indicador de carga
            showLoading(true)
            
            // Log para diagnóstico
            Timber.d("Iniciando registro de usuario: $username / $email")
            
            // Intentar registro directo primero
            try {
                auth?.createUserWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Timber.d("Usuario registrado directamente con Firebase Auth")
                            val firebaseUser = auth?.currentUser
                            
                            // Crear usuario en base de datos local
                            if (firebaseUser != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val user = UserEntity(
                                            userId = firebaseUser.uid,
                                            name = name,
                                            email = email,
                                            password = ""  // No almacenamos la contraseña
                                        )
                                        
                                        // Insertar en la base de datos local
                                        repositoryProvider.userRepository.insertUser(user)
                                        
                                        // Actualizar UI en el hilo principal
                                        withContext(Dispatchers.Main) {
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
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error guardando usuario en base local después del registro directo")
                                        
                                        // Fallback al registro vía repositorio
                                        withContext(Dispatchers.Main) {
                                            registerViaRepository(name, email, username, password)
                                        }
                                    }
                                }
                            } else {
                                Timber.e("Usuario nulo después del registro directo con Firebase Auth")
                                registerViaRepository(name, email, username, password)
                            }
                        } else {
                            Timber.e(task.exception, "Error en registro directo con Firebase Auth")
                            registerViaRepository(name, email, username, password)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error crítico al registrar usuario directamente")
                registerViaRepository(name, email, username, password)
            }
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
     * Registra un usuario a través del repositorio
     */
    private fun registerViaRepository(name: String, email: String, username: String, password: String) {
        Timber.d("Intentando registro vía repositorio")
        
        // Registrar con Firebase Auth a través del repositorio de usuarios
        repositoryProvider.userRepository.registerUserWithFirebase(
            username = username,
            password = password,
            email = email,
            name = name,
            onSuccess = { user ->
                // Actualizar session manager con el nombre de usuario específico
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
                Log.e("RegisterActivity", "Firebase registration error completo:", exception)
                
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
    }
    
    /**
     * Inicializa Firebase de forma forzada para asegurar que esté disponible
     */
    private fun forcefullyInitializeFirebase() {
        Timber.d("Inicializando Firebase forzosamente en RegisterActivity")
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
            Timber.e(e, "Error crítico al inicializar Firebase en RegisterActivity")
            Toast.makeText(
                this,
                "Error crítico: ${e.message}. Por favor reinstala la aplicación.",
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