package com.argumentor

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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
        
        // Inicializar el observador de ciclo de vida
        observer = MyObserver(lifecycle, "DebateViewActivity", this)
        
        // Inicializar Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_debate_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        
        // Obtener ID del debate desde los extras (por ahora usamos un valor de ejemplo)
        val debateId = intent.getStringExtra("debate_id") ?: "1"
        
        // En un caso real, el userId vendría de un sistema de autenticación
        val userId = "Usuario1"
        
        // Inicializar el ViewModel con el ID del debate
        viewModel.initialize(debateId, userId)
        
        // Configurar la barra de herramientas
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        // Configurar ViewPager2 y TabLayout
        setupViewPager()
        
        // Log para verificar funcionamiento
        Timber.i("DebateViewActivity created for debate $debateId")
    }

    /**
     * Configura el ViewPager2 y el TabLayout para navegar entre las etapas del debate.
     */
    private fun setupViewPager() {
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
            binding.viewPager.setCurrentItem(stage.ordinal, true)
        }
    }
}