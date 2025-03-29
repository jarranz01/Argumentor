package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.argumentor.models.Jugador
import com.argumentor.models.Tema
import com.argumentor.R
import timber.log.Timber

class FormularioActivity : AppCompatActivity() {

    private lateinit var recyclerTemas: RecyclerView
    private lateinit var jugador: Jugador
    private lateinit var adapter: TemaAdapter
    private lateinit var btnContinuar: MaterialButton
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)

        // Configurar la toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inicializar vistas
        recyclerTemas = findViewById(R.id.recyclerTemas)
        btnContinuar = findViewById(R.id.btnContinuar)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        // Animación para la RecyclerView
        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        recyclerTemas.startAnimation(animation)

        // Crear un jugador con una lista de temas
        jugador = Jugador(
            id = "1",
            nombre = getString(R.string.player_default_name),
            listaTemas = mutableListOf(
                Tema(getString(R.string.climate_change), getString(R.string.climate_change_description)),
                Tema(getString(R.string.nuclear_energy), getString(R.string.nuclear_energy_description)),
                Tema(getString(R.string.social_media), getString(R.string.social_media_description)),
                Tema(getString(R.string.online_education), getString(R.string.online_education_description)),
                Tema(getString(R.string.artificial_intelligence)),
                Tema(getString(R.string.abortion)),
                Tema(getString(R.string.bullfighting)),
                Tema(getString(R.string.film_subsidies)),
                Tema(getString(R.string.open_borders)),
                Tema(getString(R.string.freedom_of_speech)),
                Tema(getString(R.string.marijuana))
            )
        )

        // Configurar RecyclerView
        recyclerTemas.layoutManager = LinearLayoutManager(this)
        adapter = TemaAdapter(jugador.listaTemas) { tema, opinion ->
            jugador.asignarPostura(tema, opinion)
        }
        recyclerTemas.adapter = adapter

        // Configurar evento de clic para el botón continuar
        btnContinuar.setOnClickListener {
            // Mostrar el overlay de carga
            loadingOverlay.visibility = View.VISIBLE

            // Simular proceso
            Handler(Looper.getMainLooper()).postDelayed({
                // Ocultar overlay después de procesar
                loadingOverlay.visibility = View.GONE

                // Navegar a la siguiente actividad
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)

                // Por ahora solo mostraremos un mensaje
                // Toast.makeText(this, "Opiniones guardadas", Toast.LENGTH_SHORT).show()
            }, 1500)
        }
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