package com.example.chuc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class News(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String,
    val content: String? = null,
    val imageUrl: String? = null,
    val date: String,
    val isImportant: Boolean = false,
    val category: String? = null,
    val author: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
