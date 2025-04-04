package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityLoginBinding
import timber.log.Timber

/**
 * Actividad que maneja el inicio de sesión de usuarios.
 * 
 * Permite a los usuarios ingresar con su nombre de usuario y contraseña,
 * o navegar a la pantalla de registro si no tienen una cuenta.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var observer: MyObserver

    /**
     * Método llamado al crear la actividad. Inicializa el Data Binding y configura los listeners.
     * 
     * @param savedInstanceState Estado previo de la actividad si está siendo recreada.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        observer = MyObserver(
            lifecycle,
            "LoginActivity",
            this
        )
        // Vincular Data Binding con la vista
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        // Navegar al RegisterActivity
        binding.buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to register screen")
        }

        // Manejar el inicio de sesión
        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.empty_fields_error),
                    Toast.LENGTH_SHORT
                ).show()
                Timber.i("Login validation failed: empty fields")
            } else {
                // Ir a la pantalla del formulario
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish() // Evita volver atrás al login
                SessionManager.validateSession()
                Timber.i("Login successful, navigating to form")
            }
        }
    }
}