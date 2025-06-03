package com.example.nomnom

import androidx.room.*

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<Recipes>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipes>)

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()
}

