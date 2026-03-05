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
// FastingRing  (equivalent to FastingRing.tsx)
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

    val elapsedMs  = now - startTime
    val goalMs     = goalHours * 3_600_000L
    val progress   = (elapsedMs.toFloat() / goalMs).coerceIn(0f, 1f)
    val hours      = elapsedMs / 3_600_000
    val minutes    = (elapsedMs % 3_600_000) / 60_000

    val primary    = MaterialTheme.colorScheme.primary
    val surface    = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke    = 20f
            val diameter  = min(size.width, size.height) - stroke
            val topLeft   = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize   = Size(diameter, diameter)

            // Background track
            drawArc(
                color     = surface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color      = primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "%02d:%02d".format(hours, minutes),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text  = "/ ${goalHours}h",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "%.0f%%".format(progress * 100),
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
        while (true) { delay(60_000); now = System.currentTimeMillis() }
    }

    val elapsedHours = (now - startTime) / 3_600_000f
    val currentPhase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
        ?: FASTING_PHASES.first()
    val nextPhase    = FASTING_PHASES.firstOrNull { it.startHour > elapsedHours }

    Column(modifier = modifier) {
        Text(
            text  = "Fase actual: ${currentPhase.name}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text  = currentPhase.description,
            style = MaterialTheme.typography.bodyMedium
        )
        if (nextPhase != null) {
            val hoursLeft = nextPhase.startHour - elapsedHours
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Próxima fase (${nextPhase.name}) en %.1fh".format(hoursLeft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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