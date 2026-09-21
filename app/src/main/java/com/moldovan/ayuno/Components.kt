package com.moldovan.ayuno

import com.moldovan.ayuno.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.moldovan.ayuno.data.fastingPhaseById
import com.moldovan.ayuno.data.hasEatingWindow
import com.moldovan.ayuno.data.eatingWindowHours
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Restore
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
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text     = stringResource(currentPhase.nameRes).uppercase(),
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
                        context            = context,
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
                    text  = stringResource(phase.nameRes),
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
                text       = stringResource(currentPhase.nameRes),
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
                    text  = stringResource(nextPhase.nameRes),
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
// PlanProgressCard — hace visible el plan elegido durante el ayuno activo,
// con sus partes diferenciadas: ventana de ayuno vs. ventana de alimentación
// (o "día completo" para planes como ADF/5:2 sin ventana de comida).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlanProgressCard(
    plan: FastingPlan,
    startTime: Long,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1_000); now = System.currentTimeMillis() }
    }

    val elapsedHours = (now - startTime) / 3_600_000f
    val goalHours     = plan.goalHours
    val goalReached   = elapsedHours >= goalHours

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text       = stringResource(R.string.plan_progress_title, stringResource(plan.nameRes)),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text  = stringResource(plan.subtitleRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                DifficultyBadge(difficulty = plan.difficulty)
            }

            PlanWindowBar(
                elapsedHours = elapsedHours,
                goalHours    = goalHours,
                totalHours   = if (plan.hasEatingWindow) 24 else goalHours
            )

            if (plan.hasEatingWindow) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = stringResource(R.string.plan_progress_fast_window, goalHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text  = stringResource(R.string.plan_progress_eat_window, plan.eatingWindowHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = if (!goalReached)
                        stringResource(R.string.plan_progress_fast_remaining, goalHours - elapsedHours, plan.eatingWindowHours)
                    else
                        stringResource(R.string.plan_progress_eat_open, plan.eatingWindowHours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text  = stringResource(R.string.plan_progress_full_day, goalHours),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (!goalReached)
                        stringResource(R.string.plan_progress_full_day_remaining, goalHours - elapsedHours)
                    else
                        stringResource(R.string.plan_progress_full_day_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PlanWindowBar(elapsedHours: Float, goalHours: Int, totalHours: Int) {
    val fastFraction = (goalHours.toFloat() / totalHours).coerceIn(0f, 1f)
    val progress     = (elapsedHours / totalHours).coerceIn(0f, 1f)

    val fastColor   = MaterialTheme.colorScheme.primary
    val eatColor    = MaterialTheme.colorScheme.tertiary
    val markerColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(fastFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(fastColor)
            )
            if (fastFraction < 0.999f) {
                Box(
                    modifier = Modifier
                        .weight((1f - fastFraction).coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(eatColor)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0.02f, 1f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(3.dp, 14.dp)
                    .background(markerColor)
            )
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
            text  = stringResource(phase.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = stringResource(R.string.label_benefits),
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            phase.benefitsRes.forEach { benefitRes ->
                Text(
                    text  = "• ${stringResource(benefitRes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = stringResource(R.string.label_cautions),
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            phase.cautionsRes.forEach { cautionRes ->
                Text(
                    text  = "• ${stringResource(cautionRes)}",
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
                text  = stringResource(R.string.label_hunger_level, stringResource(phase.hungerLevelRes)),
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
                    text       = stringResource(R.string.plan_entry_title),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = stringResource(R.string.plan_entry_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_view_plans),
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
            Text(stringResource(R.string.action_back))
        }

        Text(
            text       = stringResource(R.string.plan_entry_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = stringResource(R.string.plan_picker_subtitle),
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
                        text       = stringResource(plan.nameRes),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = stringResource(plan.subtitleRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DifficultyBadge(difficulty = plan.difficulty)
            }
            Text(
                text  = stringResource(plan.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = stringResource(plan.scheduleRes),
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
            text  = stringResource(difficulty.labelRes),
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
                text     = stringResource(R.string.history_title),
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row {
                IconButton(onClick = { viewMode = HistoryViewMode.LISTA }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ViewList,
                        contentDescription = stringResource(R.string.cd_view_list),
                        tint = if (viewMode == HistoryViewMode.LISTA) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewMode = HistoryViewMode.CALENDARIO }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.cd_view_calendar),
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
                    label    = { Text(if (hours == 0) stringResource(R.string.filter_all) else stringResource(R.string.filter_min_hours, hours)) }
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
                    label    = { Text(stringResource(R.string.filter_all_plans)) }
                )
                availablePlans.forEach { plan ->
                    FilterChip(
                        selected = planFilter == plan.id,
                        onClick  = { planFilter = plan.id },
                        label    = { Text(stringResource(plan.nameRes)) }
                    )
                }
            }
        }

        when (viewMode) {
            HistoryViewMode.LISTA -> {
                if (filtered.isEmpty()) {
                    Text(
                        text  = stringResource(R.string.history_empty_filtered),
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
                                    text  = stringResource(R.string.history_duration, durationH, durationMin),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (session.completedPhases.isNotEmpty()) {
                                    val lastPhaseId = session.completedPhases.last()
                                    val lastPhaseName = fastingPhaseById(lastPhaseId)?.let { stringResource(it.nameRes) }
                                        ?: lastPhaseId
                                    Text(
                                        text  = stringResource(R.string.history_last_phase, lastPhaseName),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                fastingPlanById(session.planId)?.let { plan ->
                                    Text(
                                        text  = stringResource(R.string.history_plan_label, stringResource(plan.nameRes)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (session.completed) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(stringResource(R.string.history_completed_badge, session.goalHours))
                                }
                            } else if (session.endTime != null) {
                                Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(
                                        text  = stringResource(R.string.history_cancelled_badge),
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

    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                androidx.compose.material3.Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.cd_prev_month))
            }
            Text(
                text       = monthFmt.format(cal.time).replaceFirstChar { it.uppercase() },
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { monthOffset++ }) {
                androidx.compose.material3.Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.cd_next_month))
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
                R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun
            ).forEach { labelRes ->
                Text(
                    text      = stringResource(labelRes),
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
            LegendDot(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.legend_completed))
            LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = stringResource(R.string.history_cancelled_badge))
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
            Text(stringResource(R.string.action_back))
        }

        Text(
            text       = stringResource(R.string.guide_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = stringResource(R.string.guide_subtitle),
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
                    text       = stringResource(R.string.guide_considerations_title),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    R.string.guide_tip_1,
                    R.string.guide_tip_2,
                    R.string.guide_tip_3,
                    R.string.guide_tip_4,
                    R.string.guide_tip_5
                ).forEach { tipRes ->
                    val tip = stringResource(tipRes)
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
                    text       = stringResource(phase.nameRes),
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
                text  = stringResource(phase.descriptionRes),
                style = MaterialTheme.typography.bodyMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = stringResource(R.string.label_benefits),
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                phase.benefitsRes.forEach { b ->
                    Text(text = "• ${stringResource(b)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = stringResource(R.string.label_cautions),
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                phase.cautionsRes.forEach { c ->
                    Text(text = "• ${stringResource(c)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = phase.hungerEmoji, style = MaterialTheme.typography.titleSmall)
                Text(
                    text  = stringResource(R.string.label_hunger_level, stringResource(phase.hungerLevelRes)),
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
            label    = stringResource(R.string.stat_streak_label),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon     = Icons.Default.CheckCircle,
            value    = "${completed.size}",
            label    = stringResource(R.string.stat_completed_label),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon     = Icons.Default.Timer,
            value    = "%.1f".format(avgHours) + "h",
            label    = stringResource(R.string.stat_avg_label),
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
                text       = stringResource(R.string.chart_duration_title),
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
// BackupDialog — exportar/importar copia de seguridad manual (JSON)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BackupDialog(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = stringResource(R.string.backup_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick  = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_export_btn))
                }
                OutlinedButton(
                    onClick  = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_import_btn))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
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
        title = { Text(stringResource(R.string.theme_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeOption(
                    label    = stringResource(R.string.theme_light),
                    emoji    = "☀️",
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick  = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label    = stringResource(R.string.theme_dark),
                    emoji    = "🌙",
                    selected = currentMode == ThemeMode.DARK,
                    onClick  = { onSelect(ThemeMode.DARK) }
                )
                ThemeOption(
                    label    = stringResource(R.string.theme_system),
                    emoji    = "📱",
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick  = { onSelect(ThemeMode.SYSTEM) }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
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
                    text      = stringResource(R.string.completed_title),
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
                val plan = fastingPlanById(session.planId)
                Text(
                    text      = if (plan != null)
                        stringResource(R.string.completed_duration_plan, stringResource(plan.nameRes), session.goalHours)
                    else
                        stringResource(R.string.completed_duration_goal, session.goalHours),
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
                            text       = stringResource(R.string.completed_phases_title),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        session.completedPhases.forEach { phaseId ->
                            val phaseName = fastingPhaseById(phaseId)?.let { stringResource(it.nameRes) } ?: phaseId
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
                                    text  = phaseName,
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
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
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
            Text(stringResource(R.string.action_back))
        }

        Text(
            text       = stringResource(R.string.weight_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = stringResource(R.string.weight_subtitle),
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
                label         = { Text(stringResource(R.string.weight_input_label)) },
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
                Text(stringResource(R.string.action_save))
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
                text  = stringResource(R.string.weight_empty),
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
                                text       = stringResource(R.string.weight_value, entry.weightKg),
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
                                contentDescription = stringResource(R.string.cd_delete_entry),
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
                    text       = stringResource(R.string.hydration_title),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = stringResource(R.string.hydration_ml, totalMl),
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
                        Text(stringResource(type.labelRes), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text  = stringResource(R.string.hydration_hint),
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
            text  = stringResource(R.string.streak_strip_label, days),
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
            Text(stringResource(R.string.action_back))
        }

        Text(
            text       = stringResource(R.string.achievements_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = stringResource(R.string.achievements_subtitle, unlockedCount, achievements.size),
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
                    text       = stringResource(achievement.titleRes),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (progress.unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = stringResource(achievement.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (progress.unlocked) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_achievement_unlocked),
                    tint               = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}