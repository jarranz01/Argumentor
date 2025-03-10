package com.argumentor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.argumentor.models.Jugador
import com.argumentor.models.Tema
import com.argumentor.R
class FormularioActivity : AppCompatActivity() {

    private lateinit var recyclerTemas: RecyclerView
    private lateinit var jugador: Jugador
    private lateinit var adapter: TemaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)

        // Crear un jugador con una lista de temas
        jugador = Jugador(
            id = "1",
            nombre = "Jugador 1",
            listaTemas = mutableListOf(
                Tema("Cambio climático"),
                Tema("Energía nuclear"),
                Tema("Redes sociales"),
                Tema("Educación online"),
                Tema("Inteligencia artificial")
            )
        )

        recyclerTemas = findViewById(R.id.recyclerTemas)
        recyclerTemas.layoutManager = LinearLayoutManager(this)

        adapter = TemaAdapter(jugador.listaTemas) { tema, opinion ->
            jugador.asignarPostura(tema, opinion)
        }
        recyclerTemas.adapter = adapter
    }
}
