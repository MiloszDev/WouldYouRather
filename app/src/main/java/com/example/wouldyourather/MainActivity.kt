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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip

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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (question != null) {
                    
                    if (isPartyMode && !hasVoted) {
                        Text(
                            text = "Player ${viewModel.currentPlayerIndex + 1} of ${viewModel.playerCount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = "Would you rather...",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OptionCard(
                        text = question.optionA,
                        imageUrl = question.imageA,
                        percentage = if (hasVoted) calculatePercentage(question.votesA, question.votesB) else null,
                        onClick = { if (!hasVoted && !isBlocked) viewModel.vote(true) },
                        containerColor = colorResource(id = R.color.option_a),
                        contentColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            "OR",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OptionCard(
                        text = question.optionB,
                        imageUrl = question.imageB,
                        percentage = if (hasVoted) calculatePercentage(question.votesB, question.votesA) else null,
                        onClick = { if (!hasVoted && !isBlocked) viewModel.vote(false) },
                        containerColor = colorResource(id = R.color.option_b),
                        contentColor = Color.White
                    )

                    if (hasVoted) {
                        if (isPartyMode && viewModel.partyResultsRevealed) {
                            PartyResultsSummary(viewModel)
                        }
                        
                        // Show a loading indicator for the auto-advance
                        Spacer(modifier = Modifier.height(32.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "Next question in a moment...",
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isPartyMode && !hasVoted && viewModel.currentPlayerIndex == 0 && viewModel.partyVotes.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PlayerSelector(viewModel)
                    }
                }
            }

            // Overlays
            AnimatedVisibility(
                visible = waitingForNextPlayer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Vote Recorded!", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        Text("Pass the phone to Player ${viewModel.currentPlayerIndex + 2}", fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp))
                        Button(onClick = { viewModel.nextPlayerReady() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("I'm Ready!")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isBlocked, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(80.dp), tint = Color.Red)
                        Text("Slow Down!", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                        Text("Too many requests. Wait a bit.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun OptionCard(
    text: String,
    imageUrl: String?,
    percentage: Int?,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 0f
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    color = Color.White
                )
                if (percentage != null) {
                    Text(
                        text = "$percentage%",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PartyResultsSummary(viewModel: MainViewModel) {
    val votesA = viewModel.partyVotes.count { it }
    val votesB = viewModel.partyVotes.size - votesA
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Group Results", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Option A", fontSize = 12.sp)
                    Text("$votesA", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Option B", fontSize = 12.sp)
                    Text("$votesB", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelector(viewModel: MainViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Players:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Row {
            for (i in 2..4) {
                FilterChip(
                    selected = viewModel.playerCount == i,
                    onClick = { viewModel.playerCount = i },
                    label = { Text("$i") },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

fun calculatePercentage(votes: Long, otherVotes: Long): Int {
    val total = votes + otherVotes
    return if (total > 0) ((votes * 100) / total).toInt() else 0
}
