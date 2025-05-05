package com.argumentor.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entidad Room que representa la postura de un usuario sobre un tema específico.
 */
@Entity(
    tableName = "user_stances",
    primaryKeys = ["userId", "topicName"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicName"],
            childColumns = ["topicName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["topicName"])
    ]
)
data class UserStanceEntity(
    val userId: String,
    val topicName: String,
    val stance: String,  // Por ejemplo: "A_FAVOR", "EN_CONTRA", "NEUTRAL"
    val updatedAt: Long = System.currentTimeMillis()
)