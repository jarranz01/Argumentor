package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonToLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar elementos de la UI
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextUsername = findViewById(R.id.editTextRegisterUsername)
        editTextPassword = findViewById(R.id.editTextRegisterPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonToLogin = findViewById(R.id.buttonToLogIn)

        //Manejar clic en el botón buttonToLogin

        buttonToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        // Manejar clic en el botón de registro
        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Aquí puedes manejar el registro (guardar en BD, Firebase, etc.)
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                // Ir a la pantalla de login
                val intent = Intent(this, FormularioActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
