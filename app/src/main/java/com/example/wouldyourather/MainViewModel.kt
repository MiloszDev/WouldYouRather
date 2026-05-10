package com.example.wouldyourather

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "WouldYouRatherLog"

    var currentQuestion by mutableStateOf<Question?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasVoted by mutableStateOf(false)
        private set

    // Party Mode States
    var isPartyMode by mutableStateOf(false)
        private set
    var playerCount by mutableStateOf(2)
    var currentPlayerIndex by mutableStateOf(0)
        private set
    val partyVotes = mutableStateListOf<Boolean>()
    var partyResultsRevealed by mutableStateOf(false)
        private set
    var waitingForNextPlayer by mutableStateOf(false)
        private set

    // Anti-Spam States
    var isBlocked by mutableStateOf(false)
        private set
    private var lastLoadTime = 0L
    private var quickLoadCount = 0

    init {
        loadRandomQuestion()
    }

    fun togglePartyMode() {
        isPartyMode = !isPartyMode
        resetParty()
    }

    fun startParty(players: Int) {
        playerCount = players
        resetParty()
    }

    private fun resetParty() {
        currentPlayerIndex = 0
        partyVotes.clear()
        partyResultsRevealed = false
        waitingForNextPlayer = false
        hasVoted = false
    }

    fun loadRandomQuestion() {
        if (isBlocked) return

        // Anti-spam logic for single player
        if (!isPartyMode) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLoadTime < 1000) { // If clicks are less than 1 second apart
                quickLoadCount++
            } else {
                quickLoadCount = 0
            }
            lastLoadTime = currentTime

            if (quickLoadCount >= 5) { // Block after 5 rapid clicks
                isBlocked = true
                viewModelScope.launch {
                    delay(10000) // Block for 10 seconds
                    isBlocked = false
                    quickLoadCount = 0
                }
                return
            }
        }

        isLoading = true
        hasVoted = false
        partyResultsRevealed = false
        waitingForNextPlayer = false
        currentPlayerIndex = 0
        partyVotes.clear()
        
        db.collection("questions").get()
            .addOnSuccessListener { documents ->
                isLoading = false
                if (!documents.isEmpty) {
                    val randomDoc = documents.documents.random()
                    currentQuestion = randomDoc.toObject<Question>()?.copy(id = randomDoc.id)
                } else {
                    seedDatabase()
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Log.e(TAG, "Error fetching data: ", e)
            }
    }

    fun vote(isOptionA: Boolean) {
        if (isBlocked) return
        
        if (isPartyMode) {
            partyVotes.add(isOptionA)
            if (partyVotes.size < playerCount) {
                waitingForNextPlayer = true
            } else {
                // All players voted
                partyResultsRevealed = true
                hasVoted = true
                // Submit one global vote for the majority
                val votesForA = partyVotes.count { it }
                submitGlobalVote(votesForA > playerCount / 2)
                updateGlobalStatsAfterParty()
            }
        } else {
            hasVoted = true
            submitGlobalVote(isOptionA)
        }
    }

    fun nextPlayerReady() {
        if (waitingForNextPlayer) {
            currentPlayerIndex++
            waitingForNextPlayer = false
        }
    }

    private fun updateGlobalStatsAfterParty() {
        val question = currentQuestion ?: return
        db.collection("questions").document(question.id).get()
            .addOnSuccessListener { document ->
                currentQuestion = document.toObject<Question>()?.copy(id = document.id)
            }
    }

    private fun submitGlobalVote(isOptionA: Boolean) {
        val question = currentQuestion ?: return
        val fieldToUpdate = if (isOptionA) "votesA" else "votesB"
        val docRef = db.collection("questions").document(question.id)

        docRef.update(fieldToUpdate, FieldValue.increment(1))
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving vote: ", e)
            }
    }

    private fun seedDatabase() {
        val initialQuestions = listOf(
            hashMapOf("optionA" to "Have free pizza for life", "optionB" to "Have free travel for life", "votesA" to 0L, "votesB" to 0L),
            hashMapOf("optionA" to "Always be late", "optionB" to "Always be too early", "votesA" to 0L, "votesB" to 0L)
        )
        for (q in initialQuestions) {
            db.collection("questions").add(q)
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loadRandomQuestion() }, 1500)
    }
}
