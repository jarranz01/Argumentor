package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonGoToRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI elements
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonGoToRegister = findViewById(R.id.buttonGoToRegister)

        buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for login button
        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            // Simple validation
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor, complete todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // For now, just navigate to the next screen without saving or checking credentials
                val intent = Intent(this, FormularioActivity::class.java)
                startActivity(intent)
                finish() // Close login activity so user can't go back with back button
            }
        }
    }
}