package com.argumentor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityHomeBinding
import timber.log.Timber

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnMatchmaking.setOnClickListener {
            // To-do: Navegación a pantalla de emparejamiento
            Timber.i("Matchmaking clicked - Not implemented yet")
        }

        binding.btnDebateBoard.setOnClickListener {
            val intent = Intent(this, DebateBoardActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to debate board")
        }

        binding.btnMyDebates.setOnClickListener {
            // To-do: Navegación a pantalla de emparejamiento
            Timber.i("My Debates clicked - Not implemented yet")
        }

        binding.btnMyStances.setOnClickListener {
            val intent = Intent(this, FormularioActivity::class.java)
            startActivity(intent)
            Timber.i("Navigate to my stances")
        }

        binding.btnSettings.setOnClickListener {
            // To-do: Navegación a pantalla de ajustes
            Timber.i("Settings clicked - Not implemented yet")
        }
    }
}