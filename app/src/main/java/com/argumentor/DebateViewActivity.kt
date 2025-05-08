package com.argumentor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.argumentor.database.RepositoryProvider
import com.argumentor.databinding.ActivityDebateViewBinding
import com.argumentor.fragments.ArgumentHistoryFragment
import com.argumentor.fragments.DebateStageFragment
import com.argumentor.models.DebateStage
import com.argumentor.viewmodels.DebateViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

/**
 * Actividad principal para la visualización y participación en un debate.
 *
 * Esta actividad muestra las diferentes etapas del debate en pestañas separadas,
 * permitiendo al usuario navegar entre ellas e interactuar según corresponda en cada fase.
 */
class DebateViewActivity : BaseLocaleActivity() {

    private lateinit var binding: ActivityDebateViewBinding
    private val viewModel: DebateViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var repositoryProvider: RepositoryProvider
    private var isInHistoryView = false
    private var debateId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar configuración de idioma antes de inflar layouts
        applyStoredLanguageConfiguration()
        
        super.onCreate(savedInstanceState)
        
        try {
            // Inicializar el observador de ciclo de vida
            MyObserver(lifecycle, "DebateViewActivity")
            
            // Inicializar Data Binding
            binding = DataBindingUtil.setContentView(this, R.layout.activity_debate_view)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            
            // Inicializar repositorio y session manager
            repositoryProvider = RepositoryProvider.getInstance(this)
            sessionManager = SessionManager(this)
            
            // Obtener ID del debate desde los extras
            debateId = intent.getStringExtra("debate_id") ?: ""
            if (debateId.isEmpty()) {
                Timber.e("ID de debate no proporcionado o vacío")
                Toast.makeText(this, "Error: No se pudo cargar el debate", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Obtener el userId del SessionManager
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debate_view, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_view_history)?.isVisible = !isInHistoryView
        menu?.findItem(R.id.action_back_to_debate)?.isVisible = isInHistoryView
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_history -> {
                // Navegar al fragmento de historial
                navigateToHistoryFragment()
                true
            }
            R.id.action_back_to_debate -> {
                // Volver al fragmento de debate
                navigateToDebateFragments()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Navega al fragmento de historial de argumentos.
     */
    private fun navigateToHistoryFragment() {
        try {
            // Ocultar ViewPager y TabLayout
            binding.viewPager.visibility = android.view.View.GONE
            binding.tabLayout.visibility = android.view.View.GONE
            
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            
            // Añadir animación de transición
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            
            // Reemplazar el contenido con el fragmento de historial
            transaction.replace(
                R.id.fragmentContainer, 
                ArgumentHistoryFragment.newInstance(debateId)
            )
            
            transaction.commit()
            
            // Actualizar estado
            isInHistoryView = true
            invalidateOptionsMenu()
        } catch (e: Exception) {
            Timber.e(e, "Error al navegar al fragmento de historial")
            Toast.makeText(this, "Error al mostrar el historial", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Vuelve a los fragmentos de debate (ViewPager con tabs).
     */
    private fun navigateToDebateFragments() {
        try {
            // Mostrar ViewPager y TabLayout
            binding.viewPager.visibility = android.view.View.VISIBLE
            binding.tabLayout.visibility = android.view.View.VISIBLE
            
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            
            // Remover el fragmento de historial
            val historyFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
            if (historyFragment != null) {
                transaction.remove(historyFragment)
                transaction.commit()
            }
            
            // Actualizar estado
            isInHistoryView = false
            invalidateOptionsMenu()
        } catch (e: Exception) {
            Timber.e(e, "Error al navegar a los fragmentos de debate")
            Toast.makeText(this, "Error al volver al debate", Toast.LENGTH_SHORT).show()
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

    /**
     * Actualiza el estado del debate y avanza a la siguiente fase si es necesario.
     */
    private fun updateDebateStageIfNeeded() {
        viewModel.currentDebateId.observe(this) { debateId ->
            if (debateId.isNullOrEmpty()) return@observe
            
            val currentStage = viewModel.currentStage.value ?: return@observe
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val advanced = repositoryProvider.debateRepository
                        .advanceDebateStageIfComplete(debateId, currentStage.name)
                    
                    if (advanced) {
                        // Sincronizar con Firebase después de actualizar el debate
                        repositoryProvider.firebaseService.syncAllData()
                        
                        withContext(Dispatchers.Main) {
                            // Actualizar la UI para reflejar la nueva fase
                            viewModel.refreshDebate()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error al avanzar la fase del debate")
                }
            }
        }
    }

    /**
     * Guarda un argumento en la base de datos.
     */
    private fun saveArgument(content: String, position: String) {
        viewModel.currentDebateId.observe(this) { debateId ->
            if (debateId.isNullOrEmpty()) return@observe
            
            val currentStage = viewModel.currentStage.value ?: return@observe
            val userId = sessionManager.getUserId() ?: return@observe
            
            viewModel.saveArgument(debateId, userId, currentStage.name, position, content)
                .observe(this) { success ->
                    if (success) {
                        // Sincronizar con Firebase después de guardar un argumento
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                repositoryProvider.firebaseService.syncAllData()
                                Timber.d("Argumento sincronizado con Firebase")
                            } catch (e: Exception) {
                                Timber.e(e, "Error al sincronizar argumento con Firebase")
                            }
                        }
                        
                        // Actualizar la UI
                        updateDebateStageIfNeeded()
                        Toast.makeText(this, R.string.argument_saved_message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.error_saving_argument, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    /**
     * Aplica la configuración de idioma guardada en las preferencias.
     */
    override protected fun applyStoredLanguageConfiguration() {
        val preferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", Locale.getDefault().language) 
            ?: Locale.getDefault().language
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        // Crear un nuevo contexto con la configuración actualizada (método recomendado)
        createConfigurationContext(config)
        
        Timber.d("Configuración de idioma aplicada en DebateViewActivity: $languageCode")
    }
}