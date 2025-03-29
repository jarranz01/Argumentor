package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityLoginBinding
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val intent = Intent(this, FormularioActivity::class.java)
                startActivity(intent)
                finish() // Evita volver atrás al login
                Timber.i("Login successful, navigating to form")
            }
        }
    }
}
