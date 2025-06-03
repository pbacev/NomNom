package com.example.nomnom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sync from Firestore to local DB
                repository.syncFromFirestore()
                // Then get from local DB
                val localRecipes = repository.getRecipes()
                _recipes.value = localRecipes
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
            _isLoading.value = false
        }
    }
}
