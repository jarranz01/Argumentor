package com.argumentor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.argumentor.databinding.ActivityHomeBinding
import timber.log.Timber

/**
 * Actividad principal que sirve como hub de navegación para las diferentes funcionalidades de la app.
 * 
 * Esta actividad muestra los botones principales para acceder a:
 * - Emparejamiento (Matchmaking)
 * - Tablero de debates (Debate Board)
 * - Mis debates (My Debates)
 * - Mis posturas (My Stances)
 * - Ajustes (Settings)
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
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
            "HomeActivity",
            this
        )
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        setupClickListeners()
    }

    /**
     * Configura los listeners para los botones de la interfaz.
     * 
     * Cada botón maneja la navegación a una actividad diferente o muestra un mensaje
     * si la funcionalidad no está implementada aún.
     */
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