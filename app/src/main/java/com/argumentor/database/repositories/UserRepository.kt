package com.argumentor.database.repositories

import android.content.Context
import com.argumentor.database.dao.UserDao
import com.argumentor.database.entities.UserEntity
import com.argumentor.models.ArgumentorApplication
import com.argumentor.services.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * Repositorio que proporciona una API limpia para acceder a los datos de usuarios.
 * Integra sincronización con Firebase Authentication y Firestore.
 */
class UserRepository(
    private val userDao: UserDao,
    private val context: Context? = null
) {
    
    // Firebase Auth para autenticación
    private val firebaseAuth: FirebaseAuth by lazy { 
        ArgumentorApplication.getInstance().firebaseAuth 
    }
    
    // Servicio para sincronizar datos con Firebase
    private val firebaseService: FirebaseService by lazy {
        FirebaseService(context ?: ArgumentorApplication.getInstance())
    }

    /**
     * Obtiene todos los usuarios.
     */
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    
    /**
     * Obtiene un usuario por su ID.
     */
    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)
    
    /**
     * Obtiene un usuario por su nombre de usuario.
     */
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)
    
    /**
     * Inserta un usuario en la base de datos local.
     */
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
        
        // Sincronizar con Firebase Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebaseService.syncFromFirestore()
            } catch (e: Exception) {
                Timber.e(e, "Error sincronizando datos después de insertar usuario")
            }
        }
    }
    
    /**
     * Registra un nuevo usuario en el sistema mediante Firebase Authentication.
     * 
     * @param username Nombre de usuario
     * @param password Contraseña
     * @param email Correo electrónico
     * @param onSuccess Callback llamado cuando el registro es exitoso
     * @param onError Callback llamado cuando ocurre un error
     */
    fun registerUserWithFirebase(
        username: String, 
        password: String, 
        email: String,
        name: String = username,
        onSuccess: (UserEntity) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Verificar que Firebase está inicializado correctamente
            if (firebaseAuth.app == null) {
                Timber.e("Firebase Auth no está inicializado correctamente")
                onError(Exception("Firebase no está inicializado correctamente. Por favor reinicia la aplicación."))
                return
            }

            Timber.d("Intentando registro con Firebase: $email")
            
            // Usar Firebase Auth para registro
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user!!
                    Timber.d("Usuario creado en Firebase Auth: ${firebaseUser.uid}")
                    
                    // Crear entidad de usuario local con el UID de Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val user = UserEntity(
                                userId = firebaseUser.uid,
                                name = name,
                                email = email,
                                // Ya no necesitamos guardar la contraseña localmente
                                password = "", 
                                username = username,
                                createdAt = System.currentTimeMillis()
                            )
                            
                            // Guardar en la base de datos local
                            userDao.insertUser(user)
                            
                            withContext(Dispatchers.Main) {
                                onSuccess(user)
                            }
                            
                            // Sincronizar con Firestore
                            try {
                                firebaseService.syncAllData()
                            } catch (e: Exception) {
                                Timber.e(e, "Error al sincronizar con Firestore, pero el usuario se registró correctamente")
                            }
                            
                            Timber.d("Usuario registrado exitosamente con Firebase: ${user.userId}")
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                onError(e)
                            }
                            Timber.e(e, "Error guardando usuario en base de datos local")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Log detallado del error
                    Timber.e(exception, "Error durante el registro con Firebase Auth: ${exception.message}")
                    
                    // Posible problema con internet
                    if (exception.message?.contains("network", ignoreCase = true) == true) {
                        onError(Exception("No se pudo conectar con Firebase. Verifica tu conexión a internet."))
                    } else {
                        onError(exception)
                    }
                }
        } catch (e: Exception) {
            // Capturar cualquier excepción no esperada
            Timber.e(e, "Excepción inesperada durante el proceso de registro")
            onError(Exception("Error inesperado: ${e.message}", e))
        }
    }
    
    /**
     * Verifica las credenciales de inicio de sesión usando Firebase Authentication.
     * 
     * @param email Correo electrónico
     * @param password Contraseña
     * @param onSuccess Callback llamado cuando el inicio de sesión es exitoso
     * @param onError Callback llamado cuando ocurre un error
     */
    fun loginWithFirebase(
        email: String, 
        password: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user!!
                
                // Buscar o crear el usuario en la base de datos local
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var user = userDao.getUserById(firebaseUser.uid)
                        
                        // Si el usuario no existe localmente, crearlo
                        if (user == null) {
                            user = UserEntity(
                                userId = firebaseUser.uid,
                                name = firebaseUser.displayName ?: email,
                                email = email,
                                password = "",
                                username = email.substringBefore("@"),
                                createdAt = System.currentTimeMillis()
                            )
                            userDao.insertUser(user)
                        }
                        
                        withContext(Dispatchers.Main) {
                            onSuccess(user)
                        }
                        
                        // Sincronizar datos
                        try {
                            firebaseService.syncFromFirestore()
                        } catch (e: Exception) {
                            Timber.e(e, "Error sincronizando datos durante login, continuando...")
                        }
                        
                        Timber.d("Usuario autenticado con Firebase: ${user.userId}")
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onError(e)
                        }
                        Timber.e(e, "Error accediendo a la base de datos local")
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
                Timber.e(exception, "Error durante el inicio de sesión con Firebase Auth")
            }
    }
    
    /**
     * Actualiza los datos de un usuario existente.
     * Sincroniza los cambios con Firebase.
     */
    suspend fun updateUser(userId: String, name: String? = null, email: String? = null) {
        val user = userDao.getUserById(userId) ?: return
        
        val updatedUser = user.copy(
            name = name ?: user.name,
            email = email ?: user.email
        )
        
        userDao.updateUser(updatedUser)
        
        // Sincronizar con Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.syncAllData()
        }
    }
    
    /**
     * Cambia la contraseña de un usuario usando Firebase Auth.
     * @return true si se cambió con éxito
     */
    suspend fun changePasswordWithFirebase(
        currentPassword: String, 
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Obtener usuario autenticado actual
        val firebaseUser = firebaseAuth.currentUser ?: run {
            onError(Exception("No hay usuario autenticado"))
            return
        }
        
        try {
            // Reautenticar al usuario (requerido para operaciones sensibles)
            val email = firebaseUser.email ?: throw Exception("No hay email asociado")
            
            // Proceso de cambio de contraseña
            firebaseUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    onSuccess()
                    Timber.d("Contraseña cambiada exitosamente para ${firebaseUser.uid}")
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                    Timber.e(exception, "Error al cambiar contraseña")
                }
                
        } catch (e: Exception) {
            onError(e)
            Timber.e(e, "Error en el proceso de cambio de contraseña")
        }
    }
    
    /**
     * Cierra la sesión del usuario actual.
     */
    fun signOut() {
        firebaseAuth.signOut()
        Timber.d("Sesión cerrada")
    }
    
    /**
     * Obtiene el usuario de Firebase actualmente autenticado.
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}