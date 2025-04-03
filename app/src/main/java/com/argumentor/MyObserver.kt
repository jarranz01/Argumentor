package com.argumentor

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

/**
 * Observador del ciclo de vida que registra eventos importantes para depuración.
 * 
 * @property componentName Nombre del componente que está siendo observado (usado para logging).
 * @constructor Crea un observador y lo añade al lifecycle proporcionado.
 * @param lifecycle El lifecycle que será observado.
 * @param componentName Nombre identificativo del componente observado.
 */
class MyObserver(lifecycle: Lifecycle, private val componentName: String) : DefaultLifecycleObserver {
    init {
        lifecycle.addObserver(this)
    }

    /**
     * Llamado cuando el componente entra en estado STARTED.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("$componentName - onStart called")
    }

    /**
     * Llamado cuando el componente entra en estado STOPPED.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("$componentName - onStop called")
    }

    /**
     * Llamado cuando el componente entra en estado RESUMED.
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.d("$componentName - onResume called")
    }

    /**
     * Llamado cuando el componente es destruido.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Timber.d("$componentName - onDestroy called")
    }

    /**
     * Llamado cuando el componente entra en estado PAUSED.
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Timber.d("$componentName - OnPause called")
    }
}