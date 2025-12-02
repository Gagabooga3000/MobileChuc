package com.example.chuc

data class News(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String,
    val date: String,
    val url: String = "",
    val isImportant: Boolean = false
)
