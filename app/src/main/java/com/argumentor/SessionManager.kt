package com.argumentor

import android.content.Context
import android.content.SharedPreferences
import com.argumentor.models.ArgumentorApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
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
        with(sharedPreferences.edit()) {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Timber.d("Sesión creada para el usuario: $username (ID: $userId)")
    }
    
    /**
     * Crea una nueva cuenta de usuario con email y contraseña usando Firebase Authentication.
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
                // Guardar el nombre de usuario en SharedPreferences
                createSession(user.uid, username)
                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Error al crear usuario con Firebase Auth")
                onError(exception)
            }
    }
    
    /**
     * Inicia sesión con email y contraseña usando Firebase Authentication.
     * 
     * @param email Email del usuario
     * @param password Contraseña del usuario
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
                // Guardar el nombre de usuario en SharedPreferences
                createSession(user.uid, username)
                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Error al iniciar sesión con Firebase Auth")
                onError(exception)
            }
    }
    
    /**
     * Comprueba si hay un usuario con sesión activa.
     * 
     * @return true si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        // Verificar tanto en SharedPreferences como en Firebase
        val localLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val firebaseUser = firebaseAuth.currentUser
        
        // Si hay discrepancia entre Firebase y SharedPreferences, actualizar SharedPreferences
        if (localLoggedIn && firebaseUser == null) {
            // Usuario dice estar logueado localmente pero no en Firebase
            logout()
            return false
        } else if (!localLoggedIn && firebaseUser != null) {
            // Usuario logueado en Firebase pero no localmente
            createSession(firebaseUser.uid, firebaseUser.email ?: "Usuario")
            return true
        }
        
        return localLoggedIn
    }
    
    /**
     * Obtiene el ID del usuario con sesión activa.
     * 
     * @return ID del usuario o null si no hay sesión
     */
    fun getUserId(): String? {
        val firebaseUser = firebaseAuth.currentUser
        return if (isLoggedIn()) {
            firebaseUser?.uid ?: sharedPreferences.getString(KEY_USER_ID, null)
        } else {
            null
        }
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
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
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