package com.example.nomnom

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecipeRepository(private val recipeDao: RecipeDao) {

    private val firestore = FirebaseFirestore.getInstance()

    // Get recipes from local database
    suspend fun getRecipes(): List<Recipes> {
        val localRecipes = recipeDao.getAllRecipes()
        Log.d("RecipeRepository", "Loaded ${localRecipes.size} recipes from local DB")
        return localRecipes
    }


    // Sync from Firestore to local Room database
    suspend fun syncFromFirestore() {
        try {
            val snapshot = firestore.collection("recipes").get().await()
            val recipes = snapshot.documents.mapNotNull { it.toObject(Recipes::class.java) }

            Log.d("RecipeRepository", "Syncing ${recipes.size} recipes from Firestore")

            // Add this here to log each recipe's id and title
            recipes.forEach {
                Log.d("RecipeRepository", "Recipe id=${it.id}, title=${it.title}")
            }

            recipeDao.clearRecipes()
            recipeDao.insertAll(recipes)
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error syncing from Firestore", e)
        }
    }
}


