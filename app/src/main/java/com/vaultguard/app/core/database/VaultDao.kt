package com.vaultguard.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_items ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllItemsFlow(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items ORDER BY isFavorite DESC, updatedAt DESC")
    suspend fun getAllItems(): List<VaultEntity>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): VaultEntity?

    @Query("""
        SELECT * FROM vault_items 
        WHERE packageName = :target 
           OR websiteUrl LIKE '%' || :target || '%'
           OR title LIKE '%' || :target || '%'
        ORDER BY isFavorite DESC, updatedAt DESC
    """)
    suspend fun findMatchingItems(target: String): List<VaultEntity>

    @Query("""
        SELECT * FROM vault_items 
        WHERE title LIKE '%' || :query || '%' 
           OR username LIKE '%' || :query || '%'
           OR websiteUrl LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, updatedAt DESC
    """)
    fun searchItems(query: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY isFavorite DESC, updatedAt DESC")
    fun getItemsByCategory(category: String): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultEntity>)

    @Update
    suspend fun update(item: VaultEntity)

    @Delete
    suspend fun delete(item: VaultEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_items")
    suspend fun clearAll()
}
