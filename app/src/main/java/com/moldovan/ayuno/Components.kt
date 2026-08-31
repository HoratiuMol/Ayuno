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
import androidx.compose.runtime.mutableStateOf
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
import com.moldovan.ayuno.data.FASTING_PLANS
import com.moldovan.ayuno.data.FastingDifficulty
import com.moldovan.ayuno.data.FastingPhase
import com.moldovan.ayuno.data.FastingPlan
import com.moldovan.ayuno.data.FastingStorage
import com.moldovan.ayuno.data.fastingPlanById
import com.moldovan.ayuno.data.computeFastingStreak
import com.moldovan.ayuno.data.fastingMotivation
import com.moldovan.ayuno.data.computeAchievements
import com.moldovan.ayuno.data.AchievementProgress
import com.moldovan.ayuno.data.WeightEntry
import com.moldovan.ayuno.data.WeightStorage
import com.moldovan.ayuno.data.HydrationStorage
import com.moldovan.ayuno.data.HydrationType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.EmojiEvents
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.math.min
import com.moldovan.ayuno.data.ThemeMode
import com.moldovan.ayuno.data.FastingSession
//para areglar las invocaciones de botones
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon


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
                    text      = fastingMotivation(
                        currentPhase       = currentPhase,
                        nextPhase          = nextPhase,
                        elapsedHours       = elapsedHours,
                        minutesToNextPhase = if (nextPhase != null) remainingMs / 60_000 else null
                    ),
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
// PlanSelector — planes de ayuno con metodología (16:8, 18:6, OMAD, 5:2...)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlanSelectorSection(onSelect: (FastingPlan) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FASTING_PLANS.forEach { plan ->
                PlanCard(
                    plan    = plan,
                    onClick = { onSelect(plan) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlanEntryCard — entrada compacta a la pantalla de planes desde el inicio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlanEntryCard(onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large
    ) {
        Row(
            modifier              = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "Planes de ayuno",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "16:8, OMAD, 5:2... elige uno y empieza al instante",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = "Ver planes de ayuno",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlanPickerScreen — pantalla dedicada para elegir el plan de ayuno
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlanPickerScreen(
    onSelect: (FastingPlan) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }

        Text(
            text       = "Planes de ayuno",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "Elige la metodología de ayuno intermitente que mejor se adapte a ti. El ayuno empezará al seleccionarla.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        PlanSelectorSection(onSelect = onSelect)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlanCard(plan: FastingPlan, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text       = plan.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = plan.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DifficultyBadge(difficulty = plan.difficulty)
            }
            Text(
                text  = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = plan.schedule,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: FastingDifficulty) {
    val bg = when (difficulty) {
        FastingDifficulty.PRINCIPIANTE -> MaterialTheme.colorScheme.tertiaryContainer
        FastingDifficulty.INTERMEDIO   -> MaterialTheme.colorScheme.secondaryContainer
        FastingDifficulty.AVANZADO     -> MaterialTheme.colorScheme.errorContainer
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text  = difficulty.label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastingHistory — B06
// ─────────────────────────────────────────────────────────────────────────────

private enum class HistoryViewMode { LISTA, CALENDARIO }

private val DURATION_FILTERS = listOf(0, 16, 20, 24)

@Composable
fun FastingHistorySection(key: Int, storage: FastingStorage) {
    val history = remember(key) { storage.getHistory() }
    val fmt     = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }

    if (history.isEmpty()) return

    var viewMode      by remember { mutableStateOf(HistoryViewMode.LISTA) }
    var planFilter    by remember { mutableStateOf<String?>(null) }
    var minHourFilter by remember { mutableStateOf(0) }

    val availablePlans = remember(history) {
        history.mapNotNull { it.planId }.distinct().mapNotNull { fastingPlanById(it) }
    }

    val filtered = remember(history, planFilter, minHourFilter) {
        history.filter { s ->
            (planFilter == null || s.planId == planFilter) &&
                (minHourFilter == 0 || s.goalHours >= minHourFilter)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "Historial",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row {
                IconButton(onClick = { viewMode = HistoryViewMode.LISTA }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ViewList,
                        contentDescription = "Vista de lista",
                        tint = if (viewMode == HistoryViewMode.LISTA) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewMode = HistoryViewMode.CALENDARIO }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Vista de calendario",
                        tint = if (viewMode == HistoryViewMode.CALENDARIO) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DURATION_FILTERS.forEach { hours ->
                FilterChip(
                    selected = minHourFilter == hours,
                    onClick  = { minHourFilter = hours },
                    label    = { Text(if (hours == 0) "Todas" else "≥${hours}h") }
                )
            }
        }

        if (availablePlans.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = planFilter == null,
                    onClick  = { planFilter = null },
                    label    = { Text("Todos los planes") }
                )
                availablePlans.forEach { plan ->
                    FilterChip(
                        selected = planFilter == plan.id,
                        onClick  = { planFilter = plan.id },
                        label    = { Text(plan.name) }
                    )
                }
            }
        }

        when (viewMode) {
            HistoryViewMode.LISTA -> {
                if (filtered.isEmpty()) {
                    Text(
                        text  = "No hay ayunos que coincidan con el filtro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                filtered.forEach { session ->
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
                                fastingPlanById(session.planId)?.let { plan ->
                                    Text(
                                        text  = "Plan: ${plan.name}",
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
            HistoryViewMode.CALENDARIO -> {
                HistoryCalendar(sessions = filtered)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HistoryCalendar — vista mensual de días con ayuno completado/cancelado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryCalendar(sessions: List<FastingSession>) {
    var monthOffset by remember { mutableStateOf(0) }

    val cal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val year         = cal.get(Calendar.YEAR)
    val month        = cal.get(Calendar.MONTH)
    val daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1=domingo..7=sábado
    val leading      = ((firstWeekday - Calendar.MONDAY) + 7) % 7

    val dayStatus = remember(sessions, monthOffset) {
        val map = mutableMapOf<Int, Boolean>()
        val c = Calendar.getInstance()
        sessions.forEach { s ->
            c.timeInMillis = s.startTime
            if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
                val day = c.get(Calendar.DAY_OF_MONTH)
                if (map[day] != true) map[day] = s.completed
            }
        }
        map
    }

    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                androidx.compose.material3.Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
            }
            Text(
                text       = monthFmt.format(cal.time).replaceFirstChar { it.uppercase() },
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { monthOffset++ }) {
                androidx.compose.material3.Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { label ->
                Text(
                    text      = label,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f)
                )
            }
        }

        val totalCells = leading + daysInMonth
        val rows       = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - leading + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val status = dayStatus[day]
                            val bg = when (status) {
                                true  -> MaterialTheme.colorScheme.primary
                                false -> MaterialTheme.colorScheme.surfaceVariant
                                null  -> androidx.compose.ui.graphics.Color.Transparent
                            }
                            val fg = if (status == true) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = "$day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = MaterialTheme.colorScheme.primary, label = "Completado")
            LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = "Cancelado")
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    val streak = remember(key) { computeFastingStreak(history) }

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

// ─────────────────────────────────────────────────────────────────────────────
// ProgressChartSection — duración de los últimos ayunos completados
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressChartSection(key: Int, storage: FastingStorage) {
    val history   = remember(key) { storage.getHistory() }
    val completed = remember(history) { history.filter { it.completed }.sortedBy { it.startTime } }

    if (completed.size < 2) return

    val recent = remember(completed) { completed.takeLast(14) }

    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text       = "Duración de tus últimos ayunos",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            DurationBarChart(
                sessions = recent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}

@Composable
private fun DurationBarChart(sessions: List<FastingSession>, modifier: Modifier = Modifier) {
    val durationsH = sessions.map { ((it.endTime ?: it.startTime) - it.startTime) / 3_600_000f }
    val maxH       = (durationsH.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val barColor   = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barWidth = size.width / durationsH.size
        durationsH.forEachIndexed { i, h ->
            val barHeight = ((h / maxH) * size.height).coerceAtLeast(3f)
            val left      = i * barWidth + barWidth * 0.2f
            val top       = size.height - barHeight
            drawRoundRect(
                color        = barColor,
                topLeft      = Offset(left, top),
                size         = Size(barWidth * 0.6f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
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

// ─────────────────────────────────────────────────────────────────────────────
// ThemePickerDialog — B12
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemePickerDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apariencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeOption(
                    label    = "Claro",
                    emoji    = "☀️",
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick  = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label    = "Oscuro",
                    emoji    = "🌙",
                    selected = currentMode == ThemeMode.DARK,
                    onClick  = { onSelect(ThemeMode.DARK) }
                )
                ThemeOption(
                    label    = "Seguir sistema",
                    emoji    = "📱",
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick  = { onSelect(ThemeMode.SYSTEM) }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastingCompletedDialog — B13
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FastingCompletedDialog(
    session: FastingSession,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val durationMs  = (session.endTime ?: System.currentTimeMillis()) - session.startTime
    val durationH   = durationMs / 3_600_000
    val durationMin = (durationMs % 3_600_000) / 60_000

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Text(
                    text      = "🎉 ¡Ayuno completado!",
                    style     = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Duración
                Text(
                    text      = "${durationH}h ${"%02d".format(durationMin)}m",
                    style     = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                val planName = fastingPlanById(session.planId)?.name
                Text(
                    text      = if (planName != null) "Plan $planName (${session.goalHours}h) completado"
                    else "Objetivo de ${session.goalHours}h completado",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (session.completedPhases.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = "Fases superadas",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        session.completedPhases.forEach { phase ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector        = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(16.dp)
                                )
                                Text(
                                    text  = phase,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                shape   = MaterialTheme.shapes.extraLarge
            ) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.Share,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// WeightScreen — registro de peso con evolución
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WeightScreen(storage: WeightStorage, onBack: () -> Unit) {
    var refreshKey by remember { mutableStateOf(0) }
    val entries    = remember(refreshKey) { storage.getEntries() }
    var input      by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }

        Text(
            text       = "Registro de peso",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "Anota tu peso periódicamente para ver tu evolución junto a tus ayunos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label         = { Text("Peso (kg)") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier      = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val kg = input.replace(',', '.').toDoubleOrNull()
                    if (kg != null && kg > 0) {
                        storage.addEntry(kg)
                        input = ""
                        refreshKey++
                    }
                },
                shape = MaterialTheme.shapes.large
            ) {
                Text("Guardar")
            }
        }

        if (entries.size >= 2) {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                WeightChart(
                    entries  = entries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(16.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                text  = "Todavía no has registrado ningún peso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val fmt = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }
            entries.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text       = "%.1f kg".format(entry.weightKg),
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text  = fmt.format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            storage.deleteEntry(entry.id)
                            refreshKey++
                        }) {
                            androidx.compose.material3.Icon(
                                imageVector        = Icons.Default.Delete,
                                contentDescription = "Eliminar registro",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HydrationSection — registro rápido de agua/café/infusiones durante el ayuno
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HydrationSection(storage: HydrationStorage, modifier: Modifier = Modifier) {
    var refreshKey by remember { mutableStateOf(0) }
    val today      = remember(refreshKey) { storage.getTodayEntries() }
    val totalMl    = remember(today) { today.sumOf { it.amountMl } }

    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Hidratación de hoy",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "$totalMl ml",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HydrationType.entries.forEach { type ->
                    OutlinedButton(
                        onClick = {
                            storage.addEntry(type, 250)
                            refreshKey++
                        },
                        modifier = Modifier.weight(1f),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Text(type.emoji)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(type.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text  = "Cada toque añade 250 ml. El agua, café y las infusiones sin azúcar no rompen el ayuno.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val ordered = remember(entries) { entries.sortedBy { it.timestamp } }
    val minKg   = ordered.minOf { it.weightKg }
    val maxKg   = ordered.maxOf { it.weightKg }
    val range   = (maxKg - minKg).takeIf { it > 0.01 } ?: 1.0

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val gridColor  = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val stepX = if (ordered.size > 1) size.width / (ordered.size - 1) else size.width
        val points = ordered.mapIndexed { index, entry ->
            val x = index * stepX
            val normalized = ((entry.weightKg - minKg) / range).toFloat()
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        drawLine(
            color = gridColor,
            start = Offset(0f, size.height / 2f),
            end   = Offset(size.width, size.height / 2f),
            strokeWidth = 1f
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                color       = lineColor,
                start       = points[i],
                end         = points[i + 1],
                strokeWidth = 4f,
                cap         = StrokeCap.Round
            )
        }
        points.forEach { p ->
            drawCircle(color = pointColor, radius = 5f, center = p)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StreakStrip — franja compacta de racha visual (estilo GitHub/Duolingo)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StreakStrip(key: Int, storage: FastingStorage, modifier: Modifier = Modifier, days: Int = 14) {
    val history = remember(key) { storage.getHistory() }
    val completedDayKeys = remember(history) {
        history.filter { it.completed }.mapTo(mutableSetOf()) { s ->
            val cal = Calendar.getInstance().apply { timeInMillis = s.startTime }
            cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }
    }

    Column(modifier = modifier) {
        Text(
            text  = "Últimos $days días",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (offset in (days - 1) downTo 0) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                val active = dayKey in completedDayKeys
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AchievementsScreen — logros desbloqueados en función del historial
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AchievementsScreen(key: Int, storage: FastingStorage, onBack: () -> Unit) {
    val history      = remember(key) { storage.getHistory() }
    val streak       = remember(history) { computeFastingStreak(history) }
    val achievements = remember(history, streak) { computeAchievements(history, streak) }
    val unlockedCount = achievements.count { it.unlocked }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }

        Text(
            text       = "Logros",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "$unlockedCount de ${achievements.size} logros conseguidos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        achievements.forEach { progress ->
            AchievementRow(progress = progress)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AchievementRow(progress: AchievementProgress) {
    val achievement = progress.achievement
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = if (progress.unlocked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = achievement.emoji,
                style    = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.alpha(if (progress.unlocked) 1f else 0.35f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = achievement.title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (progress.unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (progress.unlocked) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = "Logro conseguido",
                    tint               = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}