package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.database.RepositoryProvider
import com.argumentor.databinding.ActivityLoginBinding
import timber.log.Timber

/**
 * Actividad que maneja el inicio de sesión de usuarios.
 * 
 * Permite a los usuarios ingresar con su email y contraseña usando Firebase Authentication,
 * o navegar a la pantalla de registro si no tienen una cuenta.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
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
        observer = MyObserver(this.lifecycle, "LoginActivity")
        
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
            
            // Intentar inicio de sesión con Firebase a través del repositorio
            repositoryProvider.userRepository.loginWithFirebase(
                email = email,
                password = password,
                onSuccess = { user ->
                    // Guardar datos de sesión
                    sessionManager.createSession(user.userId, user.name)
                    
                    showLoading(false)
                    Timber.i("Login successful with Firebase: ${user.email}")
                    Toast.makeText(
                        this,
                        getString(R.string.login_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToHome()
                },
                onError = { exception ->
                    showLoading(false)
                    
                    val errorMessage = when (exception.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                            getString(R.string.firebase_error_user_not_found)
                        "The password is invalid or the user does not have a password." -> 
                            getString(R.string.firebase_error_wrong_password)
                        "The email address is badly formatted." -> 
                            getString(R.string.firebase_error_invalid_email)
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                            getString(R.string.firebase_error_network)
                        else -> getString(R.string.firebase_error_generic)
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