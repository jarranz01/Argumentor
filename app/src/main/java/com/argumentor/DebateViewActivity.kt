package com.argumentor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.argumentor.databinding.ActivityDebateViewBinding
import com.argumentor.fragments.DebateStageFragment
import com.argumentor.models.DebateStage
import com.argumentor.viewmodels.DebateViewModel
import com.google.android.material.tabs.TabLayoutMediator
import timber.log.Timber

/**
 * Actividad principal para la visualización y participación en un debate.
 *
 * Esta actividad muestra las diferentes etapas del debate en pestañas separadas,
 * permitiendo al usuario navegar entre ellas e interactuar según corresponda en cada fase.
 */
class DebateViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebateViewBinding
    private val viewModel: DebateViewModel by viewModels()
    private lateinit var observer: MyObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Inicializar el observador de ciclo de vida
            observer = MyObserver(lifecycle, "DebateViewActivity", this)
            
            // Inicializar Data Binding
            binding = DataBindingUtil.setContentView(this, R.layout.activity_debate_view)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            
            // Obtener ID del debate desde los extras
            val debateId = intent.getStringExtra("debate_id")
            if (debateId.isNullOrEmpty()) {
                Timber.e("ID de debate no proporcionado o vacío")
                Toast.makeText(this, "Error: No se pudo cargar el debate", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Obtener el userId del SessionManager
            val sessionManager = SessionManager(this)
            val userId = sessionManager.getUserId()
            if (userId.isNullOrEmpty()) {
                Timber.e("Usuario no autenticado")
                Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Configurar la barra de herramientas
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            
            // Observar errores en el ViewModel
            viewModel.errorMessage.observe(this, Observer { errorMessage ->
                if (!errorMessage.isNullOrEmpty()) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    Timber.e("Error en ViewModel: $errorMessage")
                    finish()
                }
            })
            
            // Inicializar el ViewModel con el ID del debate
            viewModel.initialize(debateId, userId)
            
            // Configurar ViewPager2 y TabLayout
            setupViewPager()
            
            // Log para verificar funcionamiento
            Timber.i("DebateViewActivity created for debate $debateId")
            
        } catch (e: Exception) {
            Timber.e(e, "Error al inicializar DebateViewActivity")
            Toast.makeText(this, "Error al cargar el debate", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Configura el ViewPager2 y el TabLayout para navegar entre las etapas del debate.
     */
    private fun setupViewPager() {
        try {
            // Configurar el adaptador del ViewPager
            binding.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = DebateStage.values().size
                
                override fun createFragment(position: Int) = DebateStageFragment.newInstance(
                    DebateStage.values()[position]
                )
            }
            
            // Vincular el TabLayout con el ViewPager
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.stage_introduction)
                    1 -> getString(R.string.stage_refutation1)
                    2 -> getString(R.string.stage_refutation2)
                    3 -> getString(R.string.stage_conclusion)
                    else -> ""
                }
            }.attach()
            
            // Observar la etapa actual del debate para actualizar el tab seleccionado
            viewModel.currentStage.observe(this) { stage ->
                if (stage != null) {
                    binding.viewPager.setCurrentItem(stage.ordinal, true)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al configurar ViewPager")
        }
    }

    /**
     * Navega a una etapa específica del debate.
     *
     * @param stage La etapa a la que se debe navegar
     */
    fun navigateToStage(stage: DebateStage) {
        try {
            binding.viewPager.setCurrentItem(stage.ordinal, true)
        } catch (e: Exception) {
            Timber.e(e, "Error al navegar a la etapa $stage")
        }
    }
}