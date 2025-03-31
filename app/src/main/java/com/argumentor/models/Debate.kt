package com.argumentor.models

data class Debate(
    val id: String = "",
    val title: String,
    val description: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis()
)