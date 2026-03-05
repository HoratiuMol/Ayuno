package com.example.ayuno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ayuno.ui.theme.AyunoTheme
import com.example.ayuno.data.FastingSession
import com.example.ayuno.data.FastingStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = FastingStorage(this)
        setContent {
            AyunoTheme {
                AyunoApp(storage = storage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyunoApp(storage: FastingStorage) {
    var activeSession by remember { mutableStateOf<FastingSession?>(storage.getActiveSession()) }
    var goalHours by remember { mutableIntStateOf(16) }
    var historyKey by remember { mutableIntStateOf(0) }
    var showKnowledge by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiFoodBeverage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ayuno",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Handle sign out - implement AuthViewModel for full auth
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gestiona tu ayuno intermitente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when {
                showKnowledge -> {
                    KnowledgeBaseScreen(onBack = { showKnowledge = false })
                }

                activeSession == null -> {
                    // Goal Selector
                    GoalSelectorSection(
                        selectedGoal = goalHours,
                        onSelect = { goalHours = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Start Button
                    Button(
                        onClick = {
                            activeSession = storage.startSession(goalHours)
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .wrapContentWidth(),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comenzar ayuno", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showKnowledge = true }
                    ) {
                        Icon(
                            Icons.Default.MenuOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Guía del ayuno",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // History
                    FastingHistorySection(key = historyKey, storage = storage)
                }

                else -> {
                    val session = activeSession!!

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fasting Ring
                    FastingRingView(
                        startTime = session.startTime,
                        goalHours = session.goalHours
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                storage.endSession()
                                activeSession = null
                                historyKey++
                            },
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("He comido")
                        }

                        OutlinedButton(
                            onClick = {
                                storage.cancelSession()
                                activeSession = null
                            },
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancelar")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phase Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        PhaseInfoSection(
                            startTime = session.startTime,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}