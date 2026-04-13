package com.example.wouldyourather

data class Question(
    val id: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val votesA: Long = 0,
    val votesB: Long = 0
) {
    // Required for Firebase
    constructor() : this("", "", "", 0, 0)
}
