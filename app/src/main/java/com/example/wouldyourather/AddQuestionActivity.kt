package com.example.wouldyourather

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wouldyourather.databinding.ActivityAddQuestionBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddQuestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddQuestionBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSave.setOnClickListener {
            val optionA = binding.editTextOptionA.text.toString().trim()
            val optionB = binding.editTextOptionB.text.toString().trim()

            if (optionA.isNotEmpty() && optionB.isNotEmpty()) {
                saveQuestion(optionA, optionB)
            } else {
                Toast.makeText(this, "Proszę wypełnić obie opcje", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveQuestion(optionA: String, optionB: String) {
        showLoading(true)
        val newQuestion = hashMapOf(
            "optionA" to optionA,
            "optionB" to optionB,
            "votesA" to 0,
            "votesB" to 0
        )

        db.collection("questions").add(newQuestion)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Pytanie zostało dodane!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Błąd podczas dodawania pytania", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarAdd.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSave.isEnabled = !isLoading
        binding.editTextOptionA.isEnabled = !isLoading
        binding.editTextOptionB.isEnabled = !isLoading
    }
}
