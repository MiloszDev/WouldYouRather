package com.example.wouldyourather

data class Question(
    val id: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val imageA: String? = null,
    val imageB: String? = null,
    val votesA: Long = 0,
    val votesB: Long = 0
) {
    // Required for Firebase
    constructor() : this("", "", "", null, null, 0, 0)
}
