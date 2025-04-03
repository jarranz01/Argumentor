package com.argumentor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.argumentor.databinding.ActivityFormularioBinding
import com.argumentor.viewmodels.FormularioViewModel
import timber.log.Timber

class FormularioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormularioBinding
    private lateinit var observer: MyObserver

    // ViewModel para mantener los datos al girar la pantalla
    private val viewModel: FormularioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar el observador del ciclo de vida
        observer = MyObserver(this.lifecycle, "FormularioActivity")
        
        // Inicializar el data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_formulario)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Configurar la toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Animación para la RecyclerView
        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.recyclerTemas.startAnimation(animation)

        // Configurar RecyclerView cuando cambian los datos del ViewModel
        viewModel.jugador.observe(this) { jugador ->
            binding.recyclerTemas.layoutManager = LinearLayoutManager(this)
            binding.recyclerTemas.adapter = TemaAdapter(jugador.listaTemas) { nombreTema, opinion ->
                viewModel.asignarPostura(nombreTema, opinion)
            }
        }

        // Configurar evento de clic para el botón continuar
        binding.btnContinuar.setOnClickListener {
            // Mostrar el overlay de carga manualmente
            binding.loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                // Ocultar el overlay de carga
                binding.loadingOverlay.visibility = View.GONE

                // Navegar a la siguiente actividad
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                Timber.i("Navegando desde FormularioActivity a HomeActivity")
            }, 1500)
        }
    }
}