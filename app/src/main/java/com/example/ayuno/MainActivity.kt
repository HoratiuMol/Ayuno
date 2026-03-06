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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
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
    // NUEVO: controla si se muestra el diálogo de "ya llevo horas"
    var showStartDialog by remember { mutableStateOf(false) }

    // NUEVO: diálogo para indicar horas transcurridas desde última ingesta
    if (showStartDialog) {
        StartFastingDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { offsetHours ->
                val adjustedStart = System.currentTimeMillis() - (offsetHours * 3_600_000f).toLong()
                activeSession = storage.startSession(goalHours, adjustedStart)
                showStartDialog = false
            }
        )
    }

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
                    // Daily motivational quote
                    DailyQuoteCard()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Goal Selector
                    GoalSelectorSection(
                        selectedGoal = goalHours,
                        onSelect = { goalHours = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón 1: Comenzar ahora
                    Button(
                        onClick = {
                            activeSession = storage.startSession(goalHours)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comenzar ayuno", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // NUEVO — Botón 2: Ya llevo horas en ayuno
                    OutlinedButton(
                        onClick = { showStartDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ya llevo horas en ayuno", style = MaterialTheme.typography.bodyLarge)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Medical disclaimer
                    MedicalDisclaimerCard()

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

// ─────────────────────────────────────────────────────────────────────────────
// NUEVO: StartFastingDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StartFastingDialog(
    onDismiss: () -> Unit,
    onConfirm: (offsetHours: Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(2f) }

    val currentPhase = FASTING_PHASES.lastOrNull { sliderValue >= it.startHour }
        ?: FASTING_PHASES.first()
    val nextPhase    = FASTING_PHASES.firstOrNull { it.startHour > sliderValue }
    val remainingH   = nextPhase?.let { it.startHour - sliderValue }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuándo fue tu última comida?") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text      = "Hace %.1f horas".format(sliderValue),
                    style     = MaterialTheme.typography.headlineSmall,
                    color     = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value         = sliderValue,
                    onValueChange = { sliderValue = (it * 2).toInt() / 2f }, // saltos de 0.5h
                    valueRange    = 0.5f..23f,
                    steps         = 44,
                    modifier      = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.5h", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("23h", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vista previa de fase
                Card(
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = "📍 Fase actual: ${currentPhase.name}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (remainingH != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text  = "⏭ Siguiente fase en %.1fh".format(remainingH),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text      = currentPhase.description,
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(sliderValue) }) {
                Text("Iniciar ayuno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Medical disclaimer card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MedicalDisclaimerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Text(
                text = "Recuerda consultar con tu médico para ayunos de largo plazo y seguir su consejo y guía. Esta app es tan solo un gestor del ayuno, no una recomendación médica. El ayuno puede ser diferente para cada persona en función de su fisiología y necesidades médicas y metabólicas.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily motivational quote — rotates by day of year
// ─────────────────────────────────────────────────────────────────────────────

private data class Quote(val text: String, val author: String)

private val QUOTES = listOf(
    Quote("El ayuno es la puerta hacia la claridad.", "Proverbio"),
    Quote("Tu cuerpo es un templo, cuídalo con sabiduría.", "Hipócrates"),
    Quote("La disciplina es el puente entre metas y logros.", "Jim Rohn"),
    Quote("Comer es una necesidad, pero comer con inteligencia es un arte.", "La Rochefoucauld"),
    Quote("El ayuno del cuerpo es alimento del alma.", "San Juan Crisóstomo"),
    Quote("La simplicidad es la sofisticación definitiva.", "Leonardo da Vinci"),
    Quote("Cuando el estómago descansa, la mente despierta.", "Proverbio árabe"),
    Quote("No es lo que comes, sino lo que digieres, lo que te hace fuerte.", "Proverbio"),
    Quote("El descanso es tan importante como el movimiento.", "Sabiduría popular"),
    Quote("Menos es más cuando se trata de alimentar el cuerpo.", "Paracelso"),
    Quote("La paciencia es amarga, pero sus frutos son dulces.", "Aristóteles"),
    Quote("Cuida tu cuerpo, es el único lugar donde vives.", "Jim Rohn"),
    Quote("El que conquista a otros es fuerte; el que se conquista a sí mismo es poderoso.", "Lao Tse"),
    Quote("La salud no es valorada hasta que llega la enfermedad.", "Thomas Fuller"),
    Quote("Cada día es una nueva oportunidad para cuidarte.", "Sabiduría popular")
)

@Composable
fun DailyQuoteCard() {
    val quote = remember {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        QUOTES[dayOfYear % QUOTES.size]
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text      = "\"${quote.text}\"",
            style     = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = "— ${quote.author}",
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}