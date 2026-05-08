package com.savepetti.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val sortOrder: Int = 0,
    val parentId: String? = null,
    val userCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
