package com.argumentor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.argumentor.LoginActivity
import com.argumentor.R

object SessionManager {
    private var isSessionUp: Boolean = true

    fun invalidateSession() {
        isSessionUp = false
    }

    fun validateSession() {
        isSessionUp = true
    }

    fun checkSession(): Boolean {
        return isSessionUp
    }

    fun redirectToLogin(context: Context) {
        if (context is Activity) {
            context.runOnUiThread {
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                context.finish()
            }
        }
        showSessionExpired(context)
    }

    fun showSessionExpired(context: Context) {
        Toast.makeText(
            context,
            context.getString(R.string.session_expired_message),
            Toast.LENGTH_LONG
        ).show()
    }
}