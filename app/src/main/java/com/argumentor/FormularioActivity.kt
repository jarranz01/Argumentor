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

/**
 * Activity para mostrar el formulario de selección de temas y asignación de posturas.
 *
 * Esta Activity utiliza Data Binding para interactuar con su layout y observar cambios en
 * el ViewModel [FormularioViewModel]. Además, configura la toolbar, la animación de la lista
 * de temas, la asignación de posturas y la navegación a la siguiente pantalla.
 */
class FormularioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormularioBinding
    private lateinit var observer: MyObserver

    /**
     * ViewModel que mantiene la información del jugador y la lista de temas.
     */
    private val viewModel: FormularioViewModel by viewModels()

    /**
     * Se invoca al crear la Activity.
     *
     * Inicializa el Data Binding, configura la toolbar, la RecyclerView, y el botón continuar.
     * También se observa el ViewModel para actualizar los datos mostrados.
     *
     * @param savedInstanceState Estado previo de la Activity (si existe).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el observador del ciclo de vida
        observer = MyObserver(this.lifecycle, "FormularioActivity")

        // Inicializar el Data Binding y vincular el layout a la Activity
        binding = DataBindingUtil.setContentView(this, R.layout.activity_formulario)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Configurar la toolbar como ActionBar e implementar el botón de retorno
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Aplicar animación de desvanecimiento a la RecyclerView de temas
        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.recyclerTemas.startAnimation(animation)

        // Configurar el RecyclerView observando los cambios en el ViewModel
        viewModel.jugador.observe(this) { jugador ->
            binding.recyclerTemas.layoutManager = LinearLayoutManager(this)
            binding.recyclerTemas.adapter =
                TemaAdapter(jugador.listaTemas) { nombreTema, opinion ->
                    viewModel.asignarPostura(nombreTema, opinion)
                }
        }

        // Configurar el evento de clic para el botón "Continuar"
        binding.btnContinuar.setOnClickListener {
            // Mostrar el overlay de carga manualmente
            binding.loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                // Ocultar el overlay de carga después de 1500ms
                binding.loadingOverlay.visibility = View.GONE

                // Navegar a la siguiente Activity (HomeActivity)
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                Timber.i("Navegando desde FormularioActivity a HomeActivity")
            }, 1500)
        }
    }
}