package com.argumentor

import android.content.Context
import android.content.SharedPreferences
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import timber.log.Timber

/**
 * Clase para gestionar la información de sesión del usuario.
 * 
 * Utiliza SharedPreferences para guardar y recuperar datos de sesión
 * como el ID de usuario, nombre de usuario y estado de autenticación.
 * Además, integra Firebase Authentication para sincronizar el estado de la sesión.
 */
class SessionManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    private val firebaseAuth: FirebaseAuth by lazy {
        ArgumentorApplication.getInstance().firebaseAuth
    }
    
    /**
     * Guarda la información de un usuario que ha iniciado sesión.
     * 
     * @param userId ID único del usuario
     * @param username Nombre de usuario
     */
    fun createSession(userId: String, username: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Timber.d("Sesión creada para $username (ID: $userId)")
    }
    
    /**
     * Crea una nueva cuenta de usuario con email y contraseña.
     * 
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param username Nombre de usuario
     * @param onSuccess Callback llamado cuando el registro es exitoso
     * @param onError Callback llamado cuando ocurre un error
     */
    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        username: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user!!
                createSession(user.uid, username)
                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Error al crear usuario")
                onError(exception)
            }
    }
    
    /**
     * Inicia sesión con email y contraseña.
     * 
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param username Nombre de usuario
     * @param onSuccess Callback llamado cuando el inicio de sesión es exitoso
     * @param onError Callback llamado cuando ocurre un error
     */
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        username: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user!!
                createSession(user.uid, username)
                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Error al iniciar sesión")
                onError(exception)
            }
    }
    
    /**
     * Comprueba si hay un usuario con sesión activa.
     * 
     * @return true si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        val localLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val firebaseUser = firebaseAuth.currentUser
        
        // Si hay discrepancia, actualizar SharedPreferences
        return when {
            localLoggedIn && firebaseUser == null -> {
                // Sesión local pero no en Firebase
                logout()
                false
            }
            !localLoggedIn && firebaseUser != null -> {
                // Sesión en Firebase pero no local
                createSession(firebaseUser.uid, firebaseUser.email ?: "Usuario")
                true
            }
            else -> localLoggedIn
        }
    }
    
    /**
     * Obtiene el ID del usuario con sesión activa.
     * 
     * @return ID del usuario o null si no hay sesión
     */
    fun getUserId(): String? {
        if (!isLoggedIn()) return null
        
        return firebaseAuth.currentUser?.uid 
            ?: sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    /**
     * Obtiene el nombre del usuario con sesión activa.
     * 
     * @return Nombre del usuario o null si no hay sesión
     */
    fun getUsername(): String? {
        return if (isLoggedIn()) {
            sharedPreferences.getString(KEY_USERNAME, null)
        } else {
            null
        }
    }
    
    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        val username = getUsername()
        
        // Cerrar sesión en Firebase
        firebaseAuth.signOut()
        
        // Limpiar datos locales
        sharedPreferences.edit().clear().apply()
        
        Timber.d("Sesión cerrada para el usuario: $username")
    }
    
    companion object {
        private const val PREFERENCES_NAME = "argumentor_preferences"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        // Clave para el correo electrónico (nuevo para Firebase)
        private const val KEY_EMAIL = "email"
    }
}