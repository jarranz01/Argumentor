package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import timber.log.Timber

class FormularioActivity : AppCompatActivity() {

    private lateinit var recyclerTemas: RecyclerView
    private lateinit var btnContinuar: MaterialButton
    private lateinit var loadingOverlay: View

    // ViewModel para mantener los datos al girar la pantalla
    private val formularioViewModel: FormularioViewModel by viewModels()

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

        // Configurar RecyclerView cuando cambian los datos del ViewModel
        formularioViewModel.jugador.observe(this) { jugador ->
            recyclerTemas.layoutManager = LinearLayoutManager(this)
            recyclerTemas.adapter = TemaAdapter(jugador.listaTemas) { nombreTema, opinion ->
                formularioViewModel.asignarPostura(nombreTema, opinion)
            }

        }

        // Configurar evento de clic para el botón continuar
        btnContinuar.setOnClickListener {
            loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                loadingOverlay.visibility = View.GONE

                // Navegar a la siguiente actividad
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
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
