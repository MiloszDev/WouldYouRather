package com.example.wouldyourather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class AddQuestionViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var optionA by mutableStateOf("")
    var optionB by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set

    fun saveQuestion(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (optionA.isBlank() || optionB.isBlank()) {
            onError("Please fill in both options")
            return
        }

        isLoading = true
        val newQuestion = hashMapOf(
            "optionA" to optionA.trim(),
            "optionB" to optionB.trim(),
            "votesA" to 0L,
            "votesB" to 0L
        )

        db.collection("questions").add(newQuestion)
            .addOnSuccessListener {
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = false
                onError(e.localizedMessage ?: "Error adding question")
            }
    }
}
