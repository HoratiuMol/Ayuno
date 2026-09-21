package com.moldovan.ayuno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar
import com.moldovan.ayuno.ui.theme.AyunoTheme
import com.moldovan.ayuno.data.FastingSession
import com.moldovan.ayuno.data.FastingStorage
import com.moldovan.ayuno.data.ThemePreference
import com.moldovan.ayuno.data.ThemeMode
//añadidos tras implementar compartir
import androidx.compose.ui.platform.LocalContext
import com.moldovan.ayuno.data.ShareHelper
import android.content.Intent

//añadido imports tras implementar notificaiones
import android.os.Build
import com.moldovan.ayuno.data.NotificationHelper
import com.moldovan.ayuno.data.FASTING_PHASES
import com.moldovan.ayuno.data.FREE_FASTING_GOAL_HOURS
import com.moldovan.ayuno.data.fastingPlanById
import com.moldovan.ayuno.data.BackupHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.moldovan.ayuno.data.WeightStorage
import com.moldovan.ayuno.data.HydrationStorage
import com.moldovan.ayuno.data.computeFastingStreak
import com.moldovan.ayuno.data.hasCompletedFastToday
import com.moldovan.ayuno.data.streakMotivation

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            NotificationHelper.markPermissionAsked(this)
        }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted || NotificationHelper.hasAskedForPermission(this)) return
        NotificationHelper.markPermissionAsked(this)
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage          = FastingStorage(this)
        val weightStorage    = WeightStorage(this)
        val hydrationStorage = HydrationStorage(this)
        val themePreference  = ThemePreference(this)
        NotificationHelper.createChannel(this)
        ensureNotificationPermission()
        com.moldovan.ayuno.widget.WidgetTickWorker.start(this)
        setContent {
            var themeMode by remember { mutableStateOf(themePreference.getThemeMode()) }
            AyunoTheme(themeMode = themeMode) {
                AyunoApp(
                    storage          = storage,
                    weightStorage    = weightStorage,
                    hydrationStorage = hydrationStorage,
                    themeMode        = themeMode,
                    onThemeChange = { selected ->
                        themePreference.setThemeMode(selected)
                        themeMode = selected
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyunoApp(
    storage: FastingStorage,
    weightStorage: WeightStorage,
    hydrationStorage: HydrationStorage,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    var activeSession   by remember { mutableStateOf<FastingSession?>(storage.getActiveSession()) }
    var historyKey      by remember { mutableIntStateOf(0) }
    var showKnowledge    by remember { mutableStateOf(false) }
    var showWeight       by remember { mutableStateOf(false) }
    var showPlanPicker   by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    //añadidos tras compartir
    var showCompletedDialog    by remember { mutableStateOf(false) }
    var lastCompletedSession   by remember { mutableStateOf<FastingSession?>(null) }

    val context = LocalContext.current

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(BackupHelper.exportJson(context).toByteArray())
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (json != null && BackupHelper.importJson(context, json).isSuccess) {
                (context as? android.app.Activity)?.recreate()
            }
        }
    }

    if (showBackupDialog) {
        BackupDialog(
            onExport = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                exportBackupLauncher.launch("ayuno_backup_$timestamp.json")
                showBackupDialog = false
            },
            onImport = {
                importBackupLauncher.launch(arrayOf("application/json"))
                showBackupDialog = false
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = themeMode,
            onSelect    = { onThemeChange(it); showThemePicker = false },
            onDismiss   = { showThemePicker = false }
        )
    }

    if (showStartDialog) {
        StartFastingDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { offsetHours ->
                val adjustedStart = System.currentTimeMillis() - (offsetHours * 3_600_000f).toLong()
                activeSession = storage.startSession(FREE_FASTING_GOAL_HOURS, adjustedStart)
                showStartDialog = false
            }
        )
    }

    val shareChooserTitle = stringResource(R.string.share_chooser_title)

    if (showCompletedDialog && lastCompletedSession != null) {
        FastingCompletedDialog(
            session   = lastCompletedSession!!,
            onShare   = {
                val intent = ShareHelper.createShareIntent(context, lastCompletedSession!!)
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            },
            onDismiss = {
                showCompletedDialog  = false
                lastCompletedSession = null
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
                            imageVector        = Icons.Default.EmojiFoodBeverage,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "Ayuno",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showWeight = true }) {
                        Icon(
                            imageVector        = Icons.Default.MonitorWeight,
                            contentDescription = stringResource(R.string.cd_weight_log)
                        )
                    }
                    IconButton(onClick = { showThemePicker = true }) {
                        Icon(
                            imageVector        = Icons.Default.Palette,
                            contentDescription = stringResource(R.string.cd_change_theme)
                        )
                    }
                    IconButton(onClick = { showBackupDialog = true }) {
                        Icon(
                            imageVector        = Icons.Default.SettingsBackupRestore,
                            contentDescription = stringResource(R.string.cd_backup)
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
                .padding(paddingValues)        // ← aquí se usa paddingValues
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text     = stringResource(R.string.home_subtitle),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when {
                showKnowledge -> {
                    KnowledgeBaseScreen(onBack = { showKnowledge = false })
                }

                showWeight -> {
                    WeightScreen(storage = weightStorage, onBack = { showWeight = false })
                }

                showAchievements -> {
                    AchievementsScreen(
                        key     = historyKey,
                        storage = storage,
                        onBack  = { showAchievements = false }
                    )
                }

                showPlanPicker -> {
                    PlanPickerScreen(
                        onSelect = { plan ->
                            activeSession = storage.startSession(plan.goalHours, planId = plan.id)
                            showPlanPicker = false
                        },
                        onBack = { showPlanPicker = false }
                    )
                }

                activeSession == null -> {
                    MotivationCard(key = historyKey, storage = storage)
                    Spacer(modifier = Modifier.height(24.dp))
                    PlanEntryCard(onClick = { showPlanPicker = true })
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { activeSession = storage.startSession(FREE_FASTING_GOAL_HOURS) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_start_free_fast), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick  = { showStartDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_already_fasting), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showKnowledge = true }) {
                            Icon(
                                Icons.Default.MenuOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_guide), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showAchievements = true }) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_achievements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    StatsRow(key = historyKey, storage = storage)
                    Spacer(modifier = Modifier.height(16.dp))
                    StreakStrip(key = historyKey, storage = storage, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    ProgressChartSection(key = historyKey, storage = storage)
                    Spacer(modifier = Modifier.height(8.dp))
                    MedicalDisclaimerCard()
                    Spacer(modifier = Modifier.height(32.dp))
                    FastingHistorySection(key = historyKey, storage = storage)
                }

                else -> {
                    val session = activeSession!!
                    val activePlan = fastingPlanById(session.planId)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (activePlan != null) {
                        PlanProgressCard(plan = activePlan, startTime = session.startTime)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    FastingRingView(
                        startTime = session.startTime,
                        goalHours = session.goalHours
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        //remplazado al comparitr
                        Button(
                            onClick = {
                                val completed = storage.endSession()   // ahora devuelve FastingSession?
                                lastCompletedSession = completed
                                activeSession = null
                                historyKey++
                                if (completed != null) showCompletedDialog = true
                            },
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_i_ate))
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
                            Text(stringResource(R.string.action_cancel_fast))
                        }
                    }
                    TextButton(onClick = { showKnowledge = true }) {
                        Icon(
                            Icons.Default.MenuOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_guide), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HydrationSection(storage = hydrationStorage, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text       = stringResource(R.string.body_section_title),
                                style      = MaterialTheme.typography.labelMedium,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PhaseInfoSection(startTime = session.startTime)
                        }
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
        title = { Text(stringResource(R.string.start_dialog_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text      = stringResource(R.string.start_dialog_hours_ago, sliderValue),
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
                    Text(stringResource(R.string.start_dialog_min_label), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.start_dialog_max_label), style = MaterialTheme.typography.labelSmall,
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
                            text  = stringResource(R.string.start_dialog_current_phase, stringResource(currentPhase.nameRes)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (remainingH != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text  = stringResource(R.string.start_dialog_next_phase_in, remainingH),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text      = stringResource(currentPhase.descriptionRes),
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
                Text(stringResource(R.string.start_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel_fast)) }
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
                text = stringResource(R.string.medical_disclaimer),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta motivacional de la pantalla de inicio: mensaje contextual de racha
// (streakMotivation) si aplica; si no, la cita diaria que rota por día del año.
// ─────────────────────────────────────────────────────────────────────────────

private data class QuoteRes(@androidx.annotation.StringRes val textRes: Int, @androidx.annotation.StringRes val authorRes: Int)

private val QUOTES = listOf(
    QuoteRes(R.string.quote_1_text, R.string.quote_1_author),
    QuoteRes(R.string.quote_2_text, R.string.quote_2_author),
    QuoteRes(R.string.quote_3_text, R.string.quote_3_author),
    QuoteRes(R.string.quote_4_text, R.string.quote_4_author),
    QuoteRes(R.string.quote_5_text, R.string.quote_5_author),
    QuoteRes(R.string.quote_6_text, R.string.quote_6_author),
    QuoteRes(R.string.quote_7_text, R.string.quote_7_author),
    QuoteRes(R.string.quote_8_text, R.string.quote_8_author),
    QuoteRes(R.string.quote_9_text, R.string.quote_9_author),
    QuoteRes(R.string.quote_10_text, R.string.quote_10_author),
    QuoteRes(R.string.quote_11_text, R.string.quote_11_author),
    QuoteRes(R.string.quote_12_text, R.string.quote_12_author),
    QuoteRes(R.string.quote_13_text, R.string.quote_13_author),
    QuoteRes(R.string.quote_14_text, R.string.quote_14_author),
    QuoteRes(R.string.quote_15_text, R.string.quote_15_author)
)

@Composable
fun MotivationCard(key: Int, storage: FastingStorage) {
    val context        = LocalContext.current
    val history        = remember(key) { storage.getHistory() }
    val contextualMsg  = remember(history) {
        streakMotivation(
            context        = context,
            streak         = computeFastingStreak(history),
            completedToday = hasCompletedFastToday(history),
            hasHistory     = history.isNotEmpty()
        )
    }
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
            text      = "\"${contextualMsg ?: stringResource(quote.textRes)}\"",
            style     = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (contextualMsg == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text      = "— ${stringResource(quote.authorRes)}",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}