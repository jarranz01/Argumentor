package com.argumentor.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un argumento presentado en un debate.
 */
@Entity(
    tableName = "arguments",
    foreignKeys = [
        ForeignKey(
            entity = DebateEntity::class,
            parentColumns = ["debateId"],
            childColumns = ["debateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["debateId"]),
        Index(value = ["userId"]),
        Index(value = ["debateId", "stage"])
    ]
)
data class ArgumentEntity(
    @PrimaryKey(autoGenerate = true)
    val argumentId: Long = 0,
    val debateId: String = "",
    val userId: String = "",
    val stage: String = "",  // INTRODUCCION, REFUTACION1, REFUTACION2, CONCLUSION
    val position: String = "", // A_FAVOR, EN_CONTRA
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos requerido para Firebase Firestore
    constructor() : this(
        argumentId = 0,
        debateId = "",
        userId = "",
        stage = "",
        position = "",
        content = "",
        timestamp = System.currentTimeMillis()
    )
}