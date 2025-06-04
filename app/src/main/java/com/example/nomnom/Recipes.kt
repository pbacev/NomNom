package com.example.nomnom

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipes(
    @PrimaryKey val id: String = "", // Firestore document ID as primary key
    val title: String = "",
    val description: String = "",
    val favoritedBy: List<String> = emptyList() // list of user IDs who favorited this recipe
)
