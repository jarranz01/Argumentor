package com.argumentor

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class MyObserver(
    private val lifecycle: Lifecycle,
    private val componentName: String,
    private val activity: Activity? = null,
    private val sessionTimeoutHandler: (() -> Unit)? = null,
    private var isSessionUp: Boolean = true
) : DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("$componentName - onStart called")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("$componentName - onStop called")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.d("$componentName - onResume called")

        if (!SessionManager.checkSession()) {
            Timber.d("Session invalid - redirecting to login")
            activity?.let {
                SessionManager.validateSession() // Resetear el estado
                SessionManager.redirectToLogin(it)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycle.removeObserver(this)
        Timber.d("$componentName - Observer destroyed")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Timber.d("$componentName - OnPause called")
        
        if (activity != null && !activity.isFinishing && !activity.isChangingConfigurations) {
            SessionManager.invalidateSession()
        }
    }
}