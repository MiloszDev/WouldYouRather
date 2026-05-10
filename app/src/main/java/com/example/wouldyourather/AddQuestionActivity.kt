package com.example.wouldyourather

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class AddQuestionActivity : ComponentActivity() {
    private val viewModel: AddQuestionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AddQuestionScreen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onShowToast = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionScreen(
    viewModel: AddQuestionViewModel,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Question") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.optionA,
                onValueChange = { viewModel.optionA = it },
                label = { Text("Option A") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            )

            OutlinedTextField(
                value = viewModel.optionB,
                onValueChange = { viewModel.optionB = it },
                label = { Text("Option B") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.saveQuestion(
                            onSuccess = {
                                onShowToast("Question added successfully!")
                                onBack()
                            },
                            onError = { error ->
                                onShowToast(error)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Save Question", fontSize = 18.sp)
                }
            }
        }
    }
}
