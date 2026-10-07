package com.example.nsselect.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val durationSeconds: Int,
    val category: String,
    val weight: Int = 5,
    val categoryWeight: Int = 5,
    val isExcluded: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCategoryExcluded: Boolean = false
)
