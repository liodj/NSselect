package com.example.nsselect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItemsFlow(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE isExcluded = 0")
    suspend fun getIncludedItems(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
    
    @Query("UPDATE items SET isExcluded = :isExcluded WHERE category = :category")
    suspend fun updateCategoryExclusion(category: String, isExcluded: Boolean)
    
    @Query("UPDATE items SET categoryWeight = :weight WHERE category = :category")
    suspend fun updateCategoryWeight(category: String, weight: Int)
}
