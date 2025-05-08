package com.argumentor

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

/**
 * Observador del ciclo de vida para registrar eventos en Timber.
 */
class MyObserver(
    private val lifecycle: Lifecycle,
    private val componentName: String
) : DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("$componentName - onStart")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("$componentName - onStop")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.d("$componentName - onResume")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycle.removeObserver(this)
        Timber.d("$componentName - onDestroy")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Timber.d("$componentName - onPause")
    }
}