package com.example.nomnom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()
    private val repository = RecipeRepository(recipeDao)

    private val _recipes = MutableStateFlow<List<Recipes>>(emptyList())
    val recipes: StateFlow<List<Recipes>> = _recipes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        refreshRecipes()
    }

    fun refreshRecipes() {
        _isLoading.value = true

        FirebaseFirestore.getInstance()
            .collection("recipes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val recipeList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Recipes::class.java)?.copy(id = doc.id)
                    }
                    _recipes.value = recipeList
                    _errorMessage.value = null
                } else {
                    _recipes.value = emptyList()
                }

                _isLoading.value = false
            }
    }


    fun toggleFavorite(recipe: Recipes, userId: String) {
        val newFavoritedBy = if (userId in recipe.favoritedBy) {
            recipe.favoritedBy - userId
        } else {
            recipe.favoritedBy + userId
        }

        val updatedRecipe = recipe.copy(favoritedBy = newFavoritedBy)

        FirebaseFirestore.getInstance()
            .collection("recipes")
            .document(recipe.id)
            .set(updatedRecipe)
            .addOnSuccessListener {
                // optionally trigger reload if needed
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
            }
    }


}
