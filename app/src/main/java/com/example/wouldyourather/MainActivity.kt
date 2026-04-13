package com.example.wouldyourather

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wouldyourather.databinding.ActivityMainBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private var currentQuestion: Question? = null
    private val TAG = "WouldYouRatherLog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadRandomQuestion()

        binding.buttonOptionA.setOnClickListener { vote(true) }
        binding.buttonOptionB.setOnClickListener { vote(false) }
        binding.buttonNext.setOnClickListener { loadRandomQuestion() }
        binding.buttonAddQuestion.setOnClickListener {
            startActivity(Intent(this, AddQuestionActivity::class.java))
        }
    }

    private fun loadRandomQuestion() {
        showLoading(true)
        binding.textViewResultA.visibility = View.INVISIBLE
        binding.textViewResultB.visibility = View.INVISIBLE
        binding.buttonOptionA.isEnabled = true
        binding.buttonOptionB.isEnabled = true
        binding.cardOptionA.alpha = 1.0f
        binding.cardOptionB.alpha = 1.0f

        db.collection("questions").get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                if (!documents.isEmpty) {
                    val randomDoc = documents.documents.random()
                    currentQuestion = randomDoc.toObject<Question>()?.copy(id = randomDoc.id)
                    displayQuestion()
                    Log.d(TAG, "Załadowano pytanie: ${currentQuestion?.id}")
                } else {
                    Log.d(TAG, "Baza pusta, uruchamiam seedDatabase")
                    seedDatabase()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Błąd pobierania danych: ", e)
                Toast.makeText(this, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayQuestion() {
        currentQuestion?.let {
            binding.buttonOptionA.text = it.optionA
            binding.buttonOptionB.text = it.optionB
        }
    }

    private fun vote(isOptionA: Boolean) {
        val question = currentQuestion ?: return
        binding.buttonOptionA.isEnabled = false
        binding.buttonOptionB.isEnabled = false

        val fieldToUpdate = if (isOptionA) "votesA" else "votesB"
        val docRef = db.collection("questions").document(question.id)

        docRef.update(fieldToUpdate, FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d(TAG, "Głos zapisany poprawnie")
                docRef.get().addOnSuccessListener { document ->
                    val updatedQuestion = document.toObject<Question>()
                    if (updatedQuestion != null) {
                        showResults(updatedQuestion.votesA, updatedQuestion.votesB)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Błąd zapisu głosu: ", e)
                binding.buttonOptionA.isEnabled = true
                binding.buttonOptionB.isEnabled = true
                Toast.makeText(this, "Błąd zapisu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showResults(votesA: Long, votesB: Long) {
        val total = votesA + votesB
        val percentA = if (total > 0) (votesA * 100) / total else 0
        val percentB = if (total > 0) (votesB * 100) / total else 0

        binding.textViewResultA.text = "$percentA%"
        binding.textViewResultB.text = "$percentB%"
        binding.textViewResultA.visibility = View.VISIBLE
        binding.textViewResultB.visibility = View.VISIBLE
        binding.cardOptionA.alpha = 0.8f
        binding.cardOptionB.alpha = 0.8f
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun seedDatabase() {
        val initialQuestions = listOf(
            hashMapOf("optionA" to "Mieć darmową pizzę", "optionB" to "Mieć darmowe podróże", "votesA" to 0L, "votesB" to 0L),
            hashMapOf("optionA" to "Zawsze się spóźniać", "optionB" to "Zawsze być za wcześnie", "votesA" to 0L, "votesB" to 0L)
        )
        for (q in initialQuestions) {
            db.collection("questions").add(q)
                .addOnSuccessListener { Log.d(TAG, "Pytanie dodane do bazy") }
                .addOnFailureListener { e -> Log.e(TAG, "Błąd podczas seedingu: ", e) }
        }
        // Krótkie opóźnienie przed ponownym ładowaniem, aby Firebase zdążył zapisać
        binding.root.postDelayed({ loadRandomQuestion() }, 1500)
    }
}
