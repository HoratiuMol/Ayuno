package com.example.ayuno

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ayuno.data.FastingStorage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class FastingPhase(
    val name: String,
    val startHour: Int,
    val endHour: Int,           // used for display range
    val description: String,
    val motivation: String,
    val benefits: List<String>,
    val cautions: List<String>,
    val hungerLevel: String,    // e.g. "Nulo", "Creciente", "Alto"
    val hungerEmoji: String
)

val FASTING_PHASES = listOf(
    FastingPhase(
        name        = "Fase postprandial",
        startHour   = 0,
        endHour     = 6,
        description = "Tu cuerpo está utilizando la energía de los alimentos que acabas de comer. El páncreas produce insulina para usar la glucosa y almacenar el exceso como glucógeno y grasa.",
        motivation  = "La disciplina empieza cuando termina comer",
        benefits    = listOf(
            "Digestión activa y absorción de nutrientes",
            "Almacenamiento de glucógeno hepático y muscular"
        ),
        cautions    = listOf(
            "No es recomendable hacer ejercicio intenso justo después de comer"
        ),
        hungerLevel = "Nulo",
        hungerEmoji = "😌"
    ),
    FastingPhase(
        name        = "Quema de reservas",
        startHour   = 6,
        endHour     = 16,
        description = "Tu cuerpo comienza a usar las reservas de glucógeno. La glucosa almacenada en el hígado mantiene los niveles en sangre. Se activan la gluconeogénesis y la lipólisis.",
        motivation  = "Tu cuerpo aprende a usar reservas",
        benefits    = listOf(
            "Se inicia la quema de grasa almacenada",
            "Producción de cuerpos cetónicos para energía",
            "Comienza la autofagia (renovación celular)",
            "Aumento de sensibilidad a la insulina"
        ),
        cautions    = listOf(
            "Puedes sentir hambre o ligera irritabilidad",
            "Mantente bien hidratado/a"
        ),
        hungerLevel = "Creciente",
        hungerEmoji = "😐"
    ),
    FastingPhase(
        name        = "Cetosis temprana",
        startHour   = 16,
        endHour     = 24,
        description = "La glucosa en las células y el glucógeno se agotan. Tu cuerpo quema grasa almacenada como fuente principal de energía. La autofagia se intensifica.",
        motivation  = "La incomodidad forja control y claridad",
        benefits    = listOf(
            "Quema activa de grasa corporal",
            "Autofagia más intensa: limpieza celular",
            "Regulación del perfil lipídico",
            "Mejora de la sensibilidad a la insulina"
        ),
        cautions    = listOf(
            "Posible dolor de cabeza si no estás hidratado",
            "No recomendado sin experiencia previa en ayunos"
        ),
        hungerLevel = "Alto",
        hungerEmoji = "😣"
    ),
    FastingPhase(
        name        = "Cetosis profunda",
        startHour   = 24,
        endHour     = 72,
        description = "Tu cuerpo entra en cetosis plena: quema reservas de grasa para energía. Los cuerpos cetónicos actúan como combustible para el cerebro.",
        motivation  = "Ahora quemas grasa, sigue adelante",
        benefits    = listOf(
            "Rendimiento cognitivo mejorado y claridad mental",
            "Mayor sensación de energía y bienestar",
            "Reducción de triglicéridos y colesterol LDL",
            "Renovación celular profunda (autofagia)",
            "Posible efecto preventivo contra el cáncer y el envejecimiento"
        ),
        cautions    = listOf(
            "Requiere supervisión médica",
            "No apto para principiantes",
            "Asegúrate de tomar agua, infusiones y electrolitos",
            "Detener si aparecen mareos persistentes o debilidad"
        ),
        hungerLevel = "Decreciente",
        hungerEmoji = "🙂"
    ),
    FastingPhase(
        name        = "Cetosis extendida",
        startHour   = 72,
        endHour     = 96,
        description = "Estado profundo de cetosis. Todos los órganos usan cuerpos cetónicos y grasas. Las hormonas tiroideas pueden verse afectadas.",
        motivation  = "Tu cuerpo se renueva desde dentro",
        benefits    = listOf(
            "Máxima autofagia y renovación celular",
            "Mayor resistencia al estrés y toxinas",
            "El hambre tiende a disminuir a partir del tercer día"
        ),
        cautions    = listOf(
            "⚠️ SOLO con supervisión médica estricta",
            "Las hormonas tiroideas pueden alterarse",
            "El metabolismo puede verse afectado negativamente",
            "No apto para personas con TCA, embarazadas, niños o ancianos",
            "Detener inmediatamente si hay desmayos o confusión"
        ),
        hungerLevel = "Bajo",
        hungerEmoji = "😶"
    )
)

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
    val remainingSec = ((remainingMs % 60_000) / 1_000).coerceAtLeast(0)

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
                Text(
                    text      = "%02d:%02d:%02d".format(remainingH, remainingMin, remainingSec),
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

        // Description
        Text(
            text  = phase.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Benefits
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

        // Cautions
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

        // Hunger indicator
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
                FilterChip(
                    selected = hours == selectedGoal,
                    onClick  = { onSelect(hours) },
                    label    = { Text("${hours}h") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastingHistory
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape    = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp).fillMaxWidth(),
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
// KnowledgeBase — full guide from PDF
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
            text     = "Guía del ayuno",
            style    = MaterialTheme.typography.headlineSmall,
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

        // General considerations
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
            // Header
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

            // Description
            Text(
                text  = phase.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Benefits
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = "✦ Beneficios",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                phase.benefits.forEach { b ->
                    Text(
                        text  = "• $b",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Cautions
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = "⚠ Precauciones",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                phase.cautions.forEach { c ->
                    Text(
                        text  = "• $c",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Hunger
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
// StatsRow — racha, completados, promedio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(key: Int, storage: FastingStorage) {
    val history = remember(key) { storage.getHistory() }

    // Completed sessions only
    val completed = history.filter { it.completed }

    // Current streak: consecutive days ending today with a completed session
    val streak = remember(key) {
        if (completed.isEmpty()) return@remember 0
        val calendar = java.util.Calendar.getInstance()
        var streakCount = 0
        var checkDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)

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

    // Average duration in hours
    val avgHours = remember(key) {
        if (completed.isEmpty()) return@remember 0.0
        val totalMs = completed.sumOf {
            (it.endTime ?: it.startTime) - it.startTime
        }
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
            modifier            = Modifier
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
                text      = value,
                style     = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
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