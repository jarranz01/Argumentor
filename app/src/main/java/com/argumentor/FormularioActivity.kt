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
            nombre = "Jugador 1",
            listaTemas = mutableListOf(
                Tema("Cambio climático",
                    "Este tema aborda la existencia y la realidad del cambio climático, un fenómeno que se refiere a las variaciones significativas y duraderas en los patrones climáticos de la Tierra. El debate se centra en si el cambio climático es un hecho comprobado o una teoría sin suficiente evidencia."),
                Tema("Energía nuclear",
                    "Este tema se centra en el uso de la energía nuclear como fuente de electricidad. El debate gira en torno a sus ventajas y desventajas, incluyendo la seguridad, la gestión de residuos y su papel en la transición energética."),
                Tema("Redes sociales",
                    "Este tema explora el impacto del uso diario de las redes sociales en la vida de las personas. Se discuten tanto los beneficios como los perjuicios de su uso constante."),
                Tema("Educación online",
                    "Este tema se centra en la educación a distancia a través de plataformas digitales. Se debate sobre su efectividad, accesibilidad y las diferencias con la educación tradicional."),
                Tema("Inteligencia artificial"),
                Tema("Aborto"),
                Tema("Tauromaquia"),
                Tema("Subvenciones al cine"),
                Tema("Fronteras abiertas"),
                Tema("Libertad de expresión"),
                Tema("Marihuana")
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
}