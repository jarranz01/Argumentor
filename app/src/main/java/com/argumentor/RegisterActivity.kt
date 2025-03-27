package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityRegisterBinding
import timber.log.Timber

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                Timber.i("Empty fields")
            } else {
                // Aquí puedes manejar el registro (guardar en BD, Firebase, etc.)
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                // Ir a la pantalla del formulario
                val intent = Intent(this, FormularioActivity::class.java)
                startActivity(intent)
                finish()
                Timber.i("Register Correct")
            }
        }
    }
}
