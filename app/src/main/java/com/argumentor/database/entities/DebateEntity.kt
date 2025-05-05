package com.argumentor.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un debate en la base de datos.
 */
@Entity(
    tableName = "debates",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["authorUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["authorUserId"])
    ]
)
data class DebateEntity(
    @PrimaryKey
    val debateId: String,
    val title: String,
    val description: String,
    val authorUserId: String? = null,
    val participantFavorUserId: String,
    val participantContraUserId: String,
    val status: String,  // PENDIENTE, ACTIVO, TERMINADO
    val timestamp: Long = System.currentTimeMillis(),
    val category: String? = null
)