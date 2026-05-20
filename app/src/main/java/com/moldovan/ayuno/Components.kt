package com.moldovan.ayuno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moldovan.ayuno.data.FASTING_PHASES
import com.moldovan.ayuno.data.FastingPhase
import com.moldovan.ayuno.data.FastingStorage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// FastingRing — per-phase ring with countdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FastingRingView(
    startTime: Long,
    goalHours: Int,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { delay(1_000); now = System.currentTimeMillis() }
    }

    val elapsedMs    = now - startTime
    val elapsedHours = elapsedMs / 3_600_000f

    val currentPhase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
        ?: FASTING_PHASES.first()
    val nextPhase    = FASTING_PHASES.firstOrNull { it.startHour > elapsedHours }

    val phaseStartMs  = currentPhase.startHour * 3_600_000L
    val phaseEndMs    = (nextPhase?.startHour ?: (currentPhase.startHour + 4)) * 3_600_000L
    val phaseDurMs    = phaseEndMs - phaseStartMs
    val phaseElapsed  = elapsedMs - phaseStartMs
    val phaseProgress = (phaseElapsed.toFloat() / phaseDurMs).coerceIn(0f, 1f)

    val remainingMs  = if (nextPhase != null) phaseEndMs - elapsedMs else elapsedMs
    val remainingH   = (remainingMs / 3_600_000).coerceAtLeast(0)
    val remainingMin = ((remainingMs % 3_600_000) / 60_000).coerceAtLeast(0)

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text     = currentPhase.name.uppercase(),
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
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
                drawArc(
                    color = surface, startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primary, startAngle = -90f, sweepAngle = 360f * phaseProgress,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // B01 — solo HH:MM, sin segundos
                Text(
                    text      = "%02d:%02d".format(remainingH, remainingMin),
                    style     = MaterialTheme.typography.headlineMedium,
                    color     = MaterialTheme.colorScheme.onSurface,
                    maxLines  = 1,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text      = currentPhase.motivation,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines  = 2,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        PhaseTracker(elapsedHours = elapsedHours, currentPhase = currentPhase, nextPhase = nextPhase)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PhaseTracker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseTracker(
    elapsedHours: Float,
    currentPhase: FastingPhase,
    nextPhase: FastingPhase?
) {
    val completedPhases = FASTING_PHASES.filter { it.startHour < currentPhase.startHour }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        completedPhases.forEach { phase ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = phase.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text  = "${phase.startHour}–${phase.endHour}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (completedPhases.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                imageVector        = Icons.Default.RadioButtonChecked,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Text(
                text       = currentPhase.name,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text  = "${currentPhase.startHour}–${currentPhase.endHour}h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (nextPhase != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = nextPhase.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text  = "${nextPhase.startHour}–${nextPhase.endHour}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PhaseInfoSection — shown below buttons during active fasting
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseInfoSection(startTime: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1_000); now = System.currentTimeMillis() }
    }

    val elapsedHours = (now - startTime) / 3_600_000f
    val phase = FASTING_PHASES.lastOrNull { elapsedHours >= it.startHour }
        ?: FASTING_PHASES.first()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text(
            text  = phase.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = "✦ Beneficios",
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            phase.benefits.forEach { benefit ->
                Text(
                    text  = "• $benefit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = "⚠ Precauciones",
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            phase.cautions.forEach { caution ->
                Text(
                    text  = "• $caution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text  = phase.hungerEmoji,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text  = "Nivel de hambre: ${phase.hungerLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalSelector
// ─────────────────────────────────────────────────────────────────────────────

val GOALS = listOf(12, 14, 16, 18, 20, 24)

@Composable
fun GoalSelectorSection(selectedGoal: Int, onSelect: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text     = "Elige tu objetivo",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            GOALS.forEach { hours ->
                val selected = hours == selectedGoal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(hours) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "$hours",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "h",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastingHistory — B06
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
        history.take(30).forEach { session ->
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
                    modifier              = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = fmt.format(Date(session.startTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "Duración: %dh %02dm".format(durationH, durationMin),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (session.completedPhases.isNotEmpty()) {
                            Text(
                                text  = "Última fase: ${session.completedPhases.last()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (session.completed) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("✓ ${session.goalHours}h")
                        }
                    } else if (session.endTime != null) {
                        Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text  = "Cancelado",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KnowledgeBase
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KnowledgeBaseScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }

        Text(
            text       = "Guía del ayuno",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "Conoce qué ocurre en tu cuerpo durante cada fase del ayuno, sus beneficios y las precauciones a tener en cuenta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        FASTING_PHASES.forEach { phase ->
            PhaseGuideCard(phase = phase)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.large,
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = "Consideraciones generales",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    "Mantente siempre bien hidratado: agua, infusiones y café solo están permitidos.",
                    "No se recomienda en niños, embarazadas, ancianos ni personas con TCA.",
                    "Ayunos de más de 24h requieren supervisión médica.",
                    "Si experimentas mareos persistentes, atracones, irritabilidad extrema o bajada de rendimiento, detén el ayuno.",
                    "Lo ideal es empezar de forma progresiva: 12/12, luego 14/10, después 16/8."
                ).forEach { tip ->
                    Text(
                        text  = "• $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PhaseGuideCard(phase: FastingPhase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = phase.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "${phase.startHour}–${phase.endHour}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text  = phase.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = "✦ Beneficios",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                phase.benefits.forEach { b ->
                    Text(text = "• $b", style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = "⚠ Precauciones",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                phase.cautions.forEach { c ->
                    Text(text = "• $c", style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = phase.hungerEmoji, style = MaterialTheme.typography.titleSmall)
                Text(
                    text  = "Nivel de hambre: ${phase.hungerLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(key: Int, storage: FastingStorage) {
    val history   = remember(key) { storage.getHistory() }
    val completed = history.filter { it.completed }

    val streak = remember(key) {
        if (completed.isEmpty()) return@remember 0
        val calendar    = java.util.Calendar.getInstance()
        var streakCount = 0
        var checkDay    = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year        = calendar.get(java.util.Calendar.YEAR)
        for (session in completed) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = session.startTime
            val sessionDay  = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val sessionYear = cal.get(java.util.Calendar.YEAR)
            if (sessionYear == year && sessionDay == checkDay) {
                streakCount++
                checkDay--
            } else break
        }
        streakCount
    }

    val avgHours = remember(key) {
        if (completed.isEmpty()) return@remember 0.0
        val totalMs = completed.sumOf { (it.endTime ?: it.startTime) - it.startTime }
        totalMs.toDouble() / completed.size / 3_600_000.0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            icon     = Icons.Default.Whatshot,
            value    = "$streak",
            label    = "días racha",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon     = Icons.Default.CheckCircle,
            value    = "${completed.size}",
            label    = "completados",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon     = Icons.Default.Timer,
            value    = "%.1f".format(avgHours) + "h",
            label    = "promedio",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
                minLines  = 2
            )
        }
    }
}