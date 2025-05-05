package com.argumentor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un tema de debate en la base de datos.
 */
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val topicName: String,
    val description: String
)