package com.example.wouldyourather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onAddQuestionClick = {
                            startActivity(Intent(this, AddQuestionActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onAddQuestionClick: () -> Unit) {
    val question = viewModel.currentQuestion
    val isLoading = viewModel.isLoading
    val hasVoted = viewModel.hasVoted
    val isPartyMode = viewModel.isPartyMode
    val waitingForNextPlayer = viewModel.waitingForNextPlayer
    val isBlocked = viewModel.isBlocked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Would You Rather", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.togglePartyMode() }) {
                        Icon(
                            imageVector = if (isPartyMode) Icons.Default.Face else Icons.Default.Person,
                            contentDescription = "Toggle Party Mode",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (!waitingForNextPlayer && !isBlocked) {
                FloatingActionButton(
                    onClick = onAddQuestionClick,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Question")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (question != null) {
                    
                    if (isPartyMode && !hasVoted) {
                        Text(
                            text = "Player ${viewModel.currentPlayerIndex + 1} of ${viewModel.playerCount}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Text(
                        text = "Would you rather...",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OptionCard(
                        text = question.optionA,
                        percentage = if (hasVoted) calculatePercentage(question.votesA, question.votesB) else null,
                        onClick = { if (!hasVoted && !isBlocked) viewModel.vote(true) },
                        containerColor = colorResource(id = R.color.option_a),
                        contentColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            "OR",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    OptionCard(
                        text = question.optionB,
                        percentage = if (hasVoted) calculatePercentage(question.votesB, question.votesA) else null,
                        onClick = { if (!hasVoted && !isBlocked) viewModel.vote(false) },
                        containerColor = colorResource(id = R.color.option_b),
                        contentColor = Color.White
                    )

                    if (hasVoted) {
                        if (isPartyMode && viewModel.partyResultsRevealed) {
                            PartyResultsSummary(viewModel)
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.loadRandomQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = MaterialTheme.shapes.medium,
                            enabled = !isBlocked
                        ) {
                            Text("Next Question", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (isPartyMode && !hasVoted && viewModel.currentPlayerIndex == 0 && viewModel.partyVotes.isEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        PlayerSelector(viewModel)
                    }
                }
            }

            // Party Mode Pass Overlay
            AnimatedVisibility(
                visible = waitingForNextPlayer,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Vote Recorded!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pass the phone to Player ${viewModel.currentPlayerIndex + 2}",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.nextPlayerReady() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("I'm Ready!", fontSize = 18.sp)
                        }
                    }
                }
            }

            // Anti-Spam Block Overlay
            AnimatedVisibility(
                visible = isBlocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Slow Down!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            "Too many requests. Please wait a few seconds before continuing.",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PartyResultsSummary(viewModel: MainViewModel) {
    val votesA = viewModel.partyVotes.count { it }
    val votesB = viewModel.partyVotes.size - votesA
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Group Breakdown", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Option A", fontSize = 14.sp)
                    Text("$votesA", fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Option B", fontSize = 14.sp)
                    Text("$votesB", fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelector(viewModel: MainViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Number of Players:", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Row {
            for (i in 2..4) {
                FilterChip(
                    selected = viewModel.playerCount == i,
                    onClick = { viewModel.playerCount = i },
                    label = { Text("$i Players") },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun OptionCard(
    text: String,
    percentage: Int?,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
                if (percentage != null) {
                    Text(
                        text = "$percentage%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

fun calculatePercentage(votes: Long, otherVotes: Long): Int {
    val total = votes + otherVotes
    return if (total > 0) ((votes * 100) / total).toInt() else 0
}
