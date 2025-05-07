package com.argumentor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un usuario en la base de datos.
 * Compatible con Firebase Authentication.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String? = null,
    val password: String = "", // Ya no almacenamos la contraseña real, solo un marcador vacío
    val username: String = "", // Nombre de usuario específico para mostrar en la UI
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0, // Marca de tiempo para seguimiento de sincronización
    val profilePhotoUrl: String? = null // URL de la foto de perfil en Firebase Storage
)