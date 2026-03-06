package com.example.ayuno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ayuno.data.FastingStorage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// FastingRing — per-phase ring with countdown/countup
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FastingRingView(
    startTime: Long,
    goalHours: Int,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val elapsedMs    = now - startTime
    val elapsedHours = elapsedMs / 3_600_000f

    // Current and next phase
    val currentPhase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
        ?: FASTING_PHASES.first()
    val nextPhase    = FASTING_PHASES.firstOrNull { it.startHour > elapsedHours }

    // Phase ring: progress within current phase (0..1)
    val phaseStartMs = currentPhase.startHour * 3_600_000L
    val phaseEndMs   = (nextPhase?.startHour ?: (currentPhase.startHour + 4)) * 3_600_000L
    val phaseDurMs   = phaseEndMs - phaseStartMs
    val phaseElapsed = elapsedMs - phaseStartMs
    val phaseProgress = (phaseElapsed.toFloat() / phaseDurMs).coerceIn(0f, 1f)

    // Time remaining until next phase (countdown) or elapsed if last phase
    val remainingMs  = if (nextPhase != null) phaseEndMs - elapsedMs else elapsedMs
    val remainingH   = (remainingMs / 3_600_000).coerceAtLeast(0)
    val remainingMin = ((remainingMs % 3_600_000) / 60_000).coerceAtLeast(0)
    val remainingSec = ((remainingMs % 60_000) / 1_000).coerceAtLeast(0)

    val isLastPhase  = nextPhase == null
    val showSeconds  = remainingH == 0L  // show seconds only when < 1h left

    val primary  = MaterialTheme.colorScheme.primary
    val surface  = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Phase name above the ring
        Text(
            text  = currentPhase.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke   = 18f
                val diameter = min(size.width, size.height) - stroke
                val topLeft  = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize  = Size(diameter, diameter)

                // Background track
                drawArc(
                    color      = surface,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // Phase progress arc
                drawArc(
                    color      = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * phaseProgress,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Main timer
                Text(
                    text  = if (showSeconds)
                        "%02d:%02d".format(remainingMin, remainingSec)
                    else
                        "%02d:%02d".format(remainingH, remainingMin),
                    style = MaterialTheme.typography.headlineLarge
                )
                // Seconds row when showing h:min
                if (!showSeconds && !isLastPhase) {
                    Text(
                        text  = ":%02d".format(remainingSec),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Label below
                Text(
                    text  = if (isLastPhase) "Ayuno completado 🎉"
                    else "Tu ayuno comienza pronto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Next phase hint
        if (nextPhase != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Siguiente: ${nextPhase.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PhaseInfo  (equivalent to PhaseInfo.tsx)
// ─────────────────────────────────────────────────────────────────────────────

data class FastingPhase(val name: String, val startHour: Int, val description: String)

val FASTING_PHASES = listOf(
    FastingPhase("Digestión",         0,  "El cuerpo procesa la última comida y almacena glucosa."),
    FastingPhase("Estado de ayuno",   4,  "Los niveles de insulina bajan; el cuerpo empieza a quemar grasa."),
    FastingPhase("Gluconeogénesis",   8,  "El hígado produce glucosa a partir de reservas no glucídicas."),
    FastingPhase("Cetosis leve",      12, "Comienza la producción de cuerpos cetónicos como fuente de energía."),
    FastingPhase("Cetosis",           16, "La cetosis se intensifica; mayor quema de grasa."),
    FastingPhase("Autofagia",         18, "Las células inician procesos de limpieza y reciclaje celular."),
    FastingPhase("Cetosis profunda",  24, "Los niveles de cetona alcanzan su pico; beneficios cognitivos."),
    FastingPhase("Regeneración",      36, "Aumento de la hormona de crecimiento y reparación de tejidos.")
)

@Composable
fun PhaseInfoSection(startTime: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { delay(1_000); now = System.currentTimeMillis() }
    }

    val elapsedHours = (now - startTime) / 3_600_000f
    val currentPhase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
        ?: FASTING_PHASES.first()

    Column(modifier = modifier) {
        Text(
            text  = currentPhase.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalSelector  (equivalent to GoalSelector.tsx)
// ─────────────────────────────────────────────────────────────────────────────

val GOALS = listOf(12, 14, 16, 18, 20, 24)

@Composable
fun GoalSelectorSection(selectedGoal: Int, onSelect: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = "Elige tu objetivo",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GOALS.forEach { hours ->
                val selected = hours == selectedGoal
                FilterChip(
                    selected = selected,
                    onClick  = { onSelect(hours) },
                    label    = { Text("${hours}h") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastingHistory  (equivalent to FastingHistory.tsx)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FastingHistorySection(key: Int, storage: FastingStorage) {
    val history = remember(key) { storage.getHistory() }
    val fmt     = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }

    if (history.isEmpty()) return

    Column {
        Text(
            text     = "Historial",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        history.take(10).forEach { session ->
            val durationMs  = (session.endTime ?: System.currentTimeMillis()) - session.startTime
            val durationH   = durationMs / 3_600_000
            val durationMin = (durationMs % 3_600_000) / 60_000
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text  = fmt.format(Date(session.startTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "Duración: %dh %02dm".format(durationH, durationMin),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (session.completed) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("✓ ${session.goalHours}h")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KnowledgeBase  (equivalent to KnowledgeBase.tsx)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KnowledgeBaseScreen(onBack: () -> Unit) {
    Column {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }
        Text(
            text     = "Guía del ayuno intermitente",
            style    = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        FASTING_PHASES.forEach { phase ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text  = "Hora ${phase.startHour}+ — ${phase.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = phase.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}