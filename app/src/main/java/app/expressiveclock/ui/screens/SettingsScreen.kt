package app.expressiveclock.ui.screens

import app.expressiveclock.data.IslandProfile
import app.expressiveclock.data.IslandCalibration
import app.expressiveclock.data.calibration
import app.expressiveclock.data.legacyCalibration
import app.expressiveclock.data.withCalibration
import app.expressiveclock.overlay.IslandDisplayMonitor
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.selection.selectableGroup
import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Timer
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import app.expressiveclock.ui.theme.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.outlined.Home
import app.expressiveclock.data.DefaultStartupPage
import app.expressiveclock.data.SILENT
import app.expressiveclock.data.formatPresetLabel
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Expand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.DEFAULT_TIMER_PRESETS
import app.expressiveclock.data.DarkMode
import app.expressiveclock.data.FACTORY_OVERLAY_HEIGHT_OFFSET
import app.expressiveclock.data.FACTORY_OVERLAY_OFFSET_X
import app.expressiveclock.data.FACTORY_OVERLAY_OFFSET_Y
import app.expressiveclock.data.FACTORY_OVERLAY_WIDTH_OFFSET
import app.expressiveclock.data.WeekStart
import app.expressiveclock.overlay.OverlayAccessibilityService
import app.expressiveclock.overlay.OverlayManager
import app.expressiveclock.ui.elasticClickable
import app.expressiveclock.ui.responsive.LocalAppWindowSizeInfo
import app.expressiveclock.ui.responsive.TwoPaneLayout

@Composable
fun SettingsScreen(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
    onBack: () -> Unit,
    onReopenOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    var picker by remember { mutableStateOf<SettingsPicker?>(null) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showAccessibilityPromptDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val fadeDistance = with(LocalDensity.current) { 32.dp.toPx() }
    val headerFalloff by animateFloatAsState(
        targetValue = (scrollState.value / fadeDistance).coerceIn(0f, 1f),
        label = "settingsHeaderFalloff",
    )

    val appUpdateManager = remember(context) { AppUpdateManagerFactory.create(context) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    DisposableEffect(appUpdateManager) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Toast.makeText(context, "Update ready to install.", Toast.LENGTH_LONG).show()
                appUpdateManager.completeUpdate()
            }
        }
        appUpdateManager.registerListener(listener)
        onDispose {
            appUpdateManager.unregisterListener(listener)
        }
    }

    fun checkForUpdates() {
        isCheckingUpdates = true
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            isCheckingUpdates = false
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                val activity = context as? Activity
                if (activity != null) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            activity,
                            9001,
                        )
                    }.onFailure {
                        Toast.makeText(context, "Couldn't check for updates.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Couldn't check for updates.", Toast.LENGTH_SHORT).show()
                }
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Toast.makeText(context, "An update is already in progress.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "You're up to date", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { ex ->
            isCheckingUpdates = false
            Log.e("SettingsScreen", "Failed to check for updates", ex)
            Toast.makeText(context, "Couldn't check for updates.", Toast.LENGTH_SHORT).show()
        }
    }

    val timerSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val uriString = picked?.toString() ?: SILENT
            onChange { it.copy(timers = it.timers.copy(soundUri = uriString)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Keep the app's edge-to-edge behaviour, while giving the title enough breathing room
        // below the status bar on every device.
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .elasticClickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(18.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        SettingsContentLayout(
            modifier = Modifier.weight(1f),
            foldedScroll = scrollState,
            foldedFalloff = headerFalloff,
        ) { category ->
            if (category == null || category == SettingsCategory.APPEARANCE) SettingsCard(title = "Appearance") {
                SettingsNavigateRow(
                    icon = Icons.Outlined.SettingsBrightness,
                    title = "Theme",
                    subtitle = settings.theme.darkMode.label,
                    trailing = settings.theme.darkMode.label,
                    onClick = { picker = SettingsPicker.THEME },
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.Contrast,
                    title = "Pure black",
                    subtitle = "Use true black surfaces in dark mode",
                    checked = settings.theme.amoledBlack,
                    onCheckedChange = { value ->
                        onChange { it.copy(theme = it.theme.copy(amoledBlack = value)) }
                    },
                )
                SettingsDivider()
                PalettePickerRow(
                    dark = when (settings.theme.darkMode) {
                        DarkMode.SYSTEM -> isSystemInDarkTheme()
                        DarkMode.LIGHT -> false
                        DarkMode.DARK -> true
                    },
                    dynamic = settings.theme.dynamicColor,
                    selected = settings.theme.palette,
                    onDynamic = {
                        onChange { it.copy(theme = it.theme.copy(dynamicColor = true)) }
                    },
                    onSelect = { value ->
                        onChange {
                            it.copy(theme = it.theme.copy(palette = value, dynamicColor = false))
                        }
                    },
                )
            }

            if (category == null || category == SettingsCategory.ALARMS) SettingsCard(title = "Alarms") {
                // Alarm times are always 24-hour, so there is no time format choice here.
                SettingsNavigateRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Silence after",
                    subtitle = minutesLabel(settings.alarms.silenceAfterMinutes),
                    onClick = { picker = SettingsPicker.SILENCE },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.Snooze,
                    title = "Snooze length",
                    subtitle = minutesLabel(settings.alarms.snoozeMinutes),
                    onClick = { picker = SettingsPicker.SNOOZE },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Start week on",
                    subtitle = settings.alarms.weekStart.label,
                    onClick = { picker = SettingsPicker.WEEK_START },
                )


            }

            if (category == null || category == SettingsCategory.ALARMS) SettingsCard(title = "Shake to dismiss") {
                ShakeSettingsRows(settings.alarms) { update -> onChange { it.copy(alarms = update(it.alarms)) } }
            }

            if (category == null || category == SettingsCategory.ALARMS) SettingsCard(title = "Volume") {
                VolumeSettingsRows(settings.alarms) { update -> onChange { it.copy(alarms = update(it.alarms)) } }
            }

            if (category == null || category == SettingsCategory.TIMERS) SettingsCard(title = "Timers") {
                val timerSoundSubtitle = remember(settings.timers.soundUri) {
                    when (val u = settings.timers.soundUri) {
                        null -> "Default alarm sound"
                        SILENT -> "Silent"
                        else -> runCatching {
                            RingtoneManager.getRingtone(context, Uri.parse(u))?.getTitle(context)
                        }.getOrNull() ?: "Default alarm sound"
                    }
                }
                SettingsNavigateRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Timer sound",
                    subtitle = timerSoundSubtitle,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Timer sound")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            val currentUri = settings.timers.soundUri
                            if (currentUri != SILENT) {
                                val uriToPass = currentUri?.let(Uri::parse)
                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uriToPass)
                            }
                        }
                        timerSoundLauncher.launch(intent)
                    },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.Timer,
                    title = "Timer presets",
                    subtitle = settings.timers.presets.joinToString(", ") { formatPresetLabel(it) },
                    onClick = { picker = SettingsPicker.PRESETS },
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.HourglassEmpty,
                    title = "Timer vibration",
                    subtitle = "Vibrate when a timer finishes",
                    checked = settings.timers.vibrate,
                    onCheckedChange = { value ->
                        onChange { it.copy(timers = it.timers.copy(vibrate = value)) }
                    },
                )
                SettingsDivider()
                val isAccConnected = checkAccessibilityGranted(context)
                SettingsSwitchRow(
                    icon = Icons.Outlined.Timer,
                    title = "Dynamic Island overlay",
                    subtitle = "Show timer & stopwatch controls over other apps",
                    checked = settings.theme.dynamicIsland,
                    onCheckedChange = { enable ->
                        if (enable) {
                            onChange { it.copy(theme = it.theme.copy(dynamicIsland = true)) }
                            if (!isAccConnected) {
                                showAccessibilityPromptDialog = true
                            }
                        } else {
                            onChange { it.copy(theme = it.theme.copy(dynamicIsland = false)) }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = settings.theme.dynamicIsland,
                    enter = expandVertically(animationSpec = spring(stiffness = 450f, dampingRatio = 0.8f)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = 450f, dampingRatio = 0.8f)) + fadeOut(animationSpec = tween(150)),
                ) {
                    Column {
                        SettingsDivider()
                        SettingsNavigateRow(
                            icon = Icons.Outlined.Timer,
                            title = "Position calibration",
                            subtitle = "Fine-tune horizontal & vertical alignment",
                            onClick = { showCalibrationDialog = true },
                        )
                        if (!isAccConnected) {
                            SettingsDivider()
                            SettingsNavigateRow(
                                icon = Icons.Outlined.Timer,
                                title = "Enable status bar overlay",
                                subtitle = "Draw above system status bars via Accessibility",
                                onClick = { showAccessibilityPromptDialog = true },
                            )
                        }
                    }
                }
            }

            if (category == null || category == SettingsCategory.MISCELLANEOUS) SettingsCard(title = "MISCELLANEOUS") {
                SettingsNavigateRow(
                    icon = Icons.Outlined.Home,
                    title = "Default startup page",
                    subtitle = settings.defaultStartupPage.label,
                    onClick = { picker = SettingsPicker.STARTUP_PAGE },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.HourglassEmpty,
                    title = "Show setup again",
                    subtitle = "Reopen the first-launch permissions setup",
                    onClick = onReopenOnboarding,
                )
            }

            if (category == null || category == SettingsCategory.ABOUT) SettingsCard(title = "About") {
                SettingsNavigateRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "Check for updates",
                    subtitleContent = {
                        AnimatedContent(
                            targetState = if (isCheckingUpdates) "Checking for updates" else "Keep Expressive Clock up to date",
                            transitionSpec = {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                    initialOffsetY = { height -> height / 2 },
                                ) + fadeIn(tween(durationMillis = 200))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                        targetOffsetY = { height -> -height / 2 },
                                    ) + fadeOut(tween(durationMillis = 180)))
                            },
                            label = "updateSubtitleTransition",
                        ) { targetText ->
                            Text(
                                text = targetText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    },
                    onClick = {
                        if (!isCheckingUpdates) {
                            checkForUpdates()
                        }
                    },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy",
                    subtitle = "No accounts, no telemetry, no network",
                    onClick = { picker = SettingsPicker.PRIVACY },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy policy",
                    subtitle = "Read how Expressive Clock handles your data",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://samuelbeckman.github.io/expressive-clock/privacy-policy.html"),
                        )
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                Toast.makeText(context, "Couldn't open the privacy policy.", Toast.LENGTH_SHORT).show()
                            }
                    },
                )
                SettingsDivider()
                SettingsNavigateRow(
                    icon = Icons.Outlined.Info,
                    title = "App version",
                    subtitle = "Clock ${app.expressiveclock.BuildConfig.VERSION_NAME}",
                    onClick = {},
                )
            }

            Spacer(Modifier.height(4.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    picker?.let { current ->
        when (current) {
            SettingsPicker.THEME -> ThemePickerDialog(
                value = settings.theme.darkMode,
                onDismiss = { picker = null },
                onSelect = { value ->
                    onChange { it.copy(theme = it.theme.copy(darkMode = value)) }
                    picker = null
                },
            )
            SettingsPicker.ALARM_CARD_FORMAT -> ChoicePickerDialog(
                title = "Alarm card time format",
                value = settings.alarms.cards24Hour,
                options = listOf(false, true),
                label = { if (it) "24-hour" else "12-hour" },
                onDismiss = { picker = null },
                onSelect = { value ->
                    onChange { it.copy(alarms = it.alarms.copy(cards24Hour = value)) }
                    picker = null
                },
            )
            SettingsPicker.SILENCE -> ChoicePickerDialog(
                title = "Silence after",
                value = settings.alarms.silenceAfterMinutes,
                options = listOf(1, 5, 10, 15, 20, 25, 30, 0),
                label = ::minutesLabel,
                onDismiss = { picker = null },
                onSelect = { value ->
                    onChange { it.copy(alarms = it.alarms.copy(silenceAfterMinutes = value)) }
                    picker = null
                },
            )
            SettingsPicker.SNOOZE -> ChoicePickerDialog(
                title = "Snooze length",
                value = settings.alarms.snoozeMinutes,
                options = listOf(1, 5, 10, 15, 20, 30),
                label = ::minutesLabel,
                onDismiss = { picker = null },
                onSelect = { value ->
                    onChange { it.copy(alarms = it.alarms.copy(snoozeMinutes = value)) }
                    picker = null
                },
            )
            SettingsPicker.WEEK_START -> ChoicePickerDialog(
                title = "Start week on",
                value = settings.alarms.weekStart,
                options = WeekStart.entries,
                label = { it.label },
                onDismiss = { picker = null },
                onSelect = { value ->
                    onChange { it.copy(alarms = it.alarms.copy(weekStart = value)) }
                    picker = null
                },
            )
            SettingsPicker.PRESETS -> TimerPresetsDialog(
                presets = settings.timers.presets,
                onDismiss = { picker = null },
                onSave = { newPresets ->
                    onChange { it.copy(timers = it.timers.copy(presets = newPresets)) }
                    picker = null
                },
            )
            SettingsPicker.STARTUP_PAGE -> DefaultStartupPageDialog(
                current = settings.defaultStartupPage,
                onDismiss = { picker = null },
                onSelect = { selected ->
                    onChange { it.copy(defaultStartupPage = selected) }
                    picker = null
                },
            )
            SettingsPicker.PRIVACY -> PrivacyDialog(onDismiss = { picker = null })
        }
    }

    if (showCalibrationDialog) {
        OverlayCalibrationDialog(
            settings = settings,
            onDismiss = { showCalibrationDialog = false },
            onChange = onChange,
        )
    }

    if (showAccessibilityPromptDialog) {
        AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
            onDismissRequest = { showAccessibilityPromptDialog = false },
            title = { Text("Accessibility service required") },
            text = {
                Text(
                    "Expressive Clock uses Android's AccessibilityService API only to display " +
                        "active timer countdowns and stopwatch controls over other apps and system bars. " +
                        "The service does not read screen content, collect personal or sensitive user data, " +
                        "or share data. If you choose Continue, Android will open Accessibility settings " +
                        "where you can enable the service. This access is optional and is used for the " +
                        "Dynamic Island overlay feature.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityPromptDialog = false
                        runCatching {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            Toast.makeText(context, "Tap Expressive Clock and turn on Accessibility.", Toast.LENGTH_LONG).show()
                        }
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityPromptDialog = false
                        onChange { it.copy(theme = it.theme.copy(dynamicIsland = false)) }
                    },
                ) {
                    Text("Not now")
                }
            },
        )
    }
}

private enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Appearance", "Theme, colours and display", Icons.Outlined.Palette),
    ALARMS("Alarms", "Silence, snooze and calendar", Icons.Outlined.NotificationsNone),
    TIMERS("Timers", "Sounds, presets and vibration", Icons.Outlined.HourglassEmpty),
    MISCELLANEOUS("Miscellaneous", "Startup page and setup", Icons.Outlined.Home),
    ABOUT("About", "Updates and privacy", Icons.Outlined.Info),
}

// Only the expanded presentation changes; the same controls and callbacks serve both layouts.
private val LocalSettingsCategory = staticCompositionLocalOf<SettingsCategory?> { null }

@Composable
private fun SettingsContentLayout(
    foldedScroll: ScrollState,
    foldedFalloff: Float,
    modifier: Modifier = Modifier,
    content: @Composable (SettingsCategory?) -> Unit,
) {
    if (!LocalAppWindowSizeInfo.current.isExpanded) {
        Column(
            modifier.fillMaxWidth()
                .progressiveListEdgeMask(topStrength = foldedFalloff, bottomStrength = 0f)
                .verticalScroll(foldedScroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content(null) }
        return
    }

    var selected by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }
    val categoryScrolls = SettingsCategory.entries.associateWith { category ->
        androidx.compose.runtime.key(category) { rememberScrollState() }
    }
    val panelShape = RoundedCornerShape(28.dp)
    val panelBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    val panelBottomInset = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    val titleStyle = MaterialTheme.typography.titleMediumEmphasized
    val textMeasurer = rememberTextMeasurer()
    val titleWidth = with(LocalDensity.current) {
        SettingsCategory.entries.maxOf {
            textMeasurer.measure(it.title, style = titleStyle, softWrap = false, maxLines = 1).size.width
        }.toDp()
    }
    BoxWithConstraints(modifier) {
    // Reserve a complete title plus compact icon, chevron, padding and gaps on narrow windows.
    val availableWidth = (maxWidth - 48.dp).coerceAtLeast(1.dp)
    val navigationFraction = maxOf(.35f, (titleWidth + 110.dp) / availableWidth).coerceAtMost(.65f)
    TwoPaneLayout(
        modifier = Modifier.padding(bottom = 16.dp),
        leftWeight = navigationFraction,
        rightWeight = 1f - navigationFraction,
        paneSpacing = 16.dp,
        contentPaddingDp = 16.dp,
        leftPane = {
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val compactMenu = maxWidth < titleWidth + 140.dp
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(panelBottomInset)
                    .clip(panelShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, panelBorder, panelShape)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsCategory.entries.forEach { category ->
                    val active = category == selected
                    val background by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(240), label = "settingsCategoryHighlight",
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                            .background(background)
                            .border(1.dp, panelBorder, RoundedCornerShape(22.dp))
                            .selectable(active, role = Role.Tab, onClick = { selected = category })
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compactMenu) 8.dp else 12.dp),
                    ) {
                        if (compactMenu) {
                            Icon(category.icon, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        } else {
                            SettingIcon(category.icon)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(category.title, style = titleStyle, maxLines = 1, softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(category.subtitle, style = MaterialTheme.typography.bodySmall.copy(
                                lineBreak = LineBreak.Paragraph, hyphens = Hyphens.None),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            }
        },
        rightPane = {
            // Keep the panel outline outside AnimatedContent's transition and clipping layers.
            Box(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(panelBottomInset)
                    .clip(panelShape)
                    .border(1.dp, panelBorder, panelShape),
            ) {
            AnimatedContent(
                targetState = selected,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 24 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 32 })
                },
                label = "settingsCategoryContent",
            ) { category ->
                val scroll = categoryScrolls.getValue(category)
                val falloffDistance = with(LocalDensity.current) { 32.dp.toPx() }
                val falloff by animateFloatAsState(
                    (scroll.value / falloffDistance).coerceIn(0f, 1f),
                    label = "settingsCategoryFalloff",
                )
                CompositionLocalProvider(LocalSettingsCategory provides category) {
                    Column(
                        Modifier.fillMaxSize()
                            .progressiveListEdgeMask(topStrength = falloff, bottomStrength = 0f)
                            .verticalScroll(scroll).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) { content(category) }
                }
            }
            }
        },
    )
    }
}

@Composable
private fun Modifier.expandedSettingsTile(): Modifier = if (LocalSettingsCategory.current != null) {
    clip(RoundedCornerShape(22.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .25f), RoundedCornerShape(22.dp))
} else this

@Composable
private fun CalibrationControlRow(
    label: String,
    valueDp: Float,
    minValue: Float,
    maxValue: Float,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    centreDefault: Boolean = false,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(if (centreDefault && kotlin.math.abs(valueDp) < .05f) "Default" else "${java.text.DecimalFormat("0.#").format(valueDp)} dp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = {
                    val newValue = (valueDp - 1).toFloat().coerceIn(minValue, maxValue)
                    onValueChange(newValue)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = leftIcon,
                    contentDescription = "Decrease $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            Slider(
                value = if (centreDefault) app.expressiveclock.overlay.verticalSliderPosition(valueDp, minValue, maxValue) else valueDp,
                onValueChange = { onValueChange(if (centreDefault)
                    app.expressiveclock.overlay.verticalSliderOffset(it, minValue, maxValue) else it) },
                valueRange = if (centreDefault) -1f..1f else minValue..maxValue,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = {
                    val newValue = (valueDp + 1).toFloat().coerceIn(minValue, maxValue)
                    onValueChange(newValue)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = "Increase $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun OverlayCalibrationDialog(
    settings: ClockSettings,
    onDismiss: () -> Unit,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
) {
    val context = LocalContext.current
    // Only the calibration flow may rotate on a portrait-locked slab. Both its
    // overview and editor share this lifetime; leaving restores the prior policy.
    DisposableEffect(context) {
        var host: android.content.Context? = context
        while (host is android.content.ContextWrapper && host !is Activity) host = host.baseContext
        val activity = host as? Activity
        val previous = activity?.requestedOrientation
        val temporarilyUnlock = previous == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (temporarilyUnlock) activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            if (temporarilyUnlock && previous != null) activity?.requestedOrientation = previous
        }
    }
    val display by IslandDisplayMonitor.state.collectAsState()
    var selected by remember { mutableStateOf<IslandProfile?>(null) }
    var editingCalibration by remember(selected) { mutableStateOf(selected?.let { settings.theme.calibration(it) }) }
    val baseline = remember(selected) { selected?.let { settings.theme.calibration(it) } }
    var showUnsaved by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<IslandProfile?>(null) }
    val scope = rememberCoroutineScope()
    val pageProgress = remember { Animatable(0f) }
    val editorProgress = remember { Animatable(0f) }
    var navigatingBack by remember { mutableStateOf(false) }
    var gestureActive by remember { mutableStateOf(false) }
    var backDirection by remember { mutableFloatStateOf(1f) }
    suspend fun finishBack(force: Boolean = false) {
        if (!force && selected != null && editingCalibration != baseline) {
            editorProgress.animateTo(1f, spring(stiffness = 380f, dampingRatio = .86f))
            showUnsaved = true
            return
        }
        navigatingBack = true
        if (selected != null) {
            editorProgress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            selected = null
        } else {
            pageProgress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            onDismiss()
        }
        navigatingBack = false
    }
    fun goBack() {
        if (!navigatingBack && !gestureActive) { backDirection = 1f; scope.launch { finishBack() } }
    }
    fun openProfile(profile: IslandProfile) {
        if (selected == null && !navigatingBack && !gestureActive) {
            backDirection = 1f
            if (profile == display.calibrationProfile) selected = profile else pendingProfile = profile
        }
    }
    LaunchedEffect(pendingProfile, display.calibrationProfile) {
        pendingProfile?.takeIf { it == display.calibrationProfile }?.let {
            pendingProfile = null
            selected = it
        }
    }
    val profiles = if (display.foldable) IslandProfile.entries else IslandProfile.entries.take(2)
    fun label(profile: IslandProfile): String = if (display.foldable) profile.label else
        if (profile == IslandProfile.FOLDED_PORTRAIT) "Portrait" else "Landscape"
    LaunchedEffect(selected) {
        if (selected == null) {
            editorProgress.snapTo(0f)
        } else {
            editorProgress.snapTo(0f)
            editorProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { goBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        LaunchedEffect(Unit) { pageProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        PredictiveBackHandler(enabled = !navigatingBack && !showUnsaved && pendingProfile == null) { events ->
            val progress = if (selected != null) editorProgress else pageProgress
            gestureActive = true
            try {
                events.collect { event ->
                    backDirection = if (event.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT) -1f else 1f
                    progress.snapTo(1f - event.progress.coerceIn(0f, 1f))
                }
                finishBack()
            } catch (_: CancellationException) {
                withContext(NonCancellable) {
                    progress.animateTo(1f, spring(stiffness = 380f, dampingRatio = .86f))
                }
            } finally { gestureActive = false }
        }
        pendingProfile?.let { requested ->
            CalibrationProfileGuidance(
                profile = requested, currentProfile = display.calibrationProfile, foldable = display.foldable,
                onBack = { pendingProfile = null },
                onContinue = { pendingProfile = null; selected = requested },
            )
        }
        if (showUnsaved) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showUnsaved = false },
                title = { Text("Unsaved changes") },
                text = { Text("Save your calibration changes before leaving?") },
                confirmButton = {
                    TextButton(onClick = {
                        val profile = selected
                        val draft = editingCalibration
                        if (profile != null && draft != null)
                            onChange { current -> current.copy(theme = current.theme.withCalibration(profile, draft)) }
                        showUnsaved = false
                        scope.launch { finishBack(force = true) }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showUnsaved = false }) { Text("Keep editing") }
                        TextButton(onClick = {
                            showUnsaved = false
                            scope.launch { finishBack(force = true) }
                        }) { Text("Discard") }
                    }
                },
            )
        }
        val dialogView = LocalView.current
        val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
        DisposableEffect(dialogView, lightBars) {
            val window = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
            val controller = window?.let { androidx.core.view.WindowInsetsControllerCompat(it, dialogView) }
            val oldStatus = controller?.isAppearanceLightStatusBars
            val oldNavigation = controller?.isAppearanceLightNavigationBars
            controller?.isAppearanceLightStatusBars = lightBars
            controller?.isAppearanceLightNavigationBars = lightBars
            onDispose {
                if (oldStatus != null) controller?.isAppearanceLightStatusBars = oldStatus
                if (oldNavigation != null) controller?.isAppearanceLightNavigationBars = oldNavigation
            }
        }
        val renderPage: @Composable (IslandProfile?) -> Unit = { profile ->
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { goBack() },
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(profile?.let { "${label(it)} Calibration" } ?: "Position Calibration",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                }
                val calibrationScroll = rememberScrollState()
                val edgeDistance = with(LocalDensity.current) { 32.dp.toPx() }
                val topEdge by animateFloatAsState(
                    (calibrationScroll.value / edgeDistance).coerceIn(0f, 1f), label = "calibrationTopFalloff")
                val bottomEdge by animateFloatAsState(
                    ((calibrationScroll.maxValue - calibrationScroll.value) / edgeDistance).coerceIn(0f, 1f),
                    label = "calibrationBottomFalloff")
                Column(Modifier.weight(1f)
                    .progressiveListEdgeMask(topStrength = topEdge, bottomStrength = bottomEdge, edgeBandsOnly = true)
                    .verticalScroll(calibrationScroll).padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (profile == null) {
                        Text("Adjust the position and size of the Dynamic Island to match your device and preferences.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("DISPLAY CONFIGURATIONS", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        profiles.forEach { entry ->
                            val active = entry == display.calibrationProfile
                            val value = settings.theme.calibration(entry)
                            androidx.compose.material3.Surface(onClick = { openProfile(entry) },
                                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (active) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center) {
                                        Icon(if (entry.ordinal < 2) Icons.Outlined.Smartphone else Icons.Outlined.TabletAndroid,
                                            contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.graphicsLayer { rotationZ = if (entry.ordinal % 2 == 1) 90f else 0f })
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(label(entry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (active) Text("Current", color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium)
                                        Text(if (entry.ordinal < 2) "Standard ${if (entry.ordinal == 0) "vertical" else "horizontal"} orientation"
                                            else "Inner display ${if (entry.ordinal == 2) "vertical" else "horizontal"} orientation",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    androidx.compose.material3.Switch(checked = value.enabled, onCheckedChange = { enabled ->
                                        onChange { current -> current.copy(theme = current.theme.withCalibration(entry,
                                            current.theme.calibration(entry).copy(enabled = enabled))) }
                                    })
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Edit ${label(entry)}")
                                }
                            }
                        }
                    } else {
                        val value = editingCalibration ?: settings.theme.calibration(profile)
                        fun update(next: IslandCalibration) { editingCalibration = next }
                        Text("Adjust position and dimensions for this display mode. Preview changes here, then tap Save to apply.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Active: ${label(display.calibrationProfile)}", style = MaterialTheme.typography.labelLarge)
                        if (profile != display.calibrationProfile) Text("Switch to this display configuration to see its live preview.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Horizontal alignment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(4.dp)) {
                            val segmentWidth = maxWidth / 3
                            val indicatorX by androidx.compose.animation.core.animateDpAsState(
                                targetValue = segmentWidth * value.alignment.ordinal,
                                animationSpec = spring(stiffness = 380f, dampingRatio = .86f),
                                label = "alignmentIndicator")
                            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(indicatorX.roundToPx(), 0) }
                                .width(segmentWidth).height(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)))
                            Row(Modifier.fillMaxWidth().selectableGroup()) {
                                app.expressiveclock.data.IslandAlignment.entries.forEach { alignment ->
                                    val checked = value.alignment == alignment
                                    val labelColor by androidx.compose.animation.animateColorAsState(
                                        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "alignmentLabel")
                                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                                        .selectable(selected = checked, role = androidx.compose.ui.semantics.Role.RadioButton,
                                            onClick = { update(value.copy(alignment = alignment)) }),
                                        contentAlignment = Alignment.Center) {
                                        Text(alignment.name.lowercase().replaceFirstChar { it.uppercase() },
                                            color = labelColor, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                        CalibrationControlRow("Horizontal offset", value.x - FACTORY_OVERLAY_OFFSET_X, -200f, 200f,
                            Icons.AutoMirrored.Rounded.ArrowBack, Icons.AutoMirrored.Rounded.ArrowForward) {
                            update(value.copy(x = FACTORY_OVERLAY_OFFSET_X + it)) }
                        val verticalRange = app.expressiveclock.overlay.calibrationVerticalRange(value.height)
                        CalibrationControlRow("Vertical offset", (value.y - FACTORY_OVERLAY_OFFSET_Y).coerceIn(verticalRange), verticalRange.start, verticalRange.endInclusive,
                            Icons.Rounded.ArrowUpward, Icons.Rounded.ArrowDownward, centreDefault = true) {
                            update(value.copy(y = FACTORY_OVERLAY_OFFSET_Y + it)) }
                        CalibrationControlRow("Capsule width", value.width - FACTORY_OVERLAY_WIDTH_OFFSET, -60f, 300f,
                            Icons.Rounded.Compress, Icons.Rounded.Expand) {
                            update(value.copy(width = FACTORY_OVERLAY_WIDTH_OFFSET + it)) }
                        CalibrationControlRow("Capsule height", value.height - FACTORY_OVERLAY_HEIGHT_OFFSET, -20f, 20f,
                            Icons.Rounded.Compress, Icons.Rounded.Expand) {
                            update(value.copy(height = FACTORY_OVERLAY_HEIGHT_OFFSET + it)) }
                        androidx.compose.material3.Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Expanded card position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Choose whether expanded cards avoid or overlap the camera cutout. Saved separately for each profile.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                app.expressiveclock.data.IslandExpandedPosition.entries.forEach { position ->
                                    val selectedPosition = value.expandedPosition == position
                                    val selectionColour = animateColorAsState(
                                        targetValue = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = if (selectedPosition) 1f else 0f),
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        label = "expandedPositionSelection")
                                    Row(Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .graphicsLayer()
                                        .drawBehind { drawRect(selectionColour.value) }
                                        .selectable(selected = selectedPosition,
                                            role = androidx.compose.ui.semantics.Role.RadioButton,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                            onClick = { update(value.copy(expandedPosition = position)) })
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.RadioButton(selected = selectedPosition,
                                            onClick = null)
                                        Text(if (position == app.expressiveclock.data.IslandExpandedPosition.BELOW_STATUS_BAR)
                                            "Below status bar" else "Overlap camera cutout")
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            androidx.compose.material3.FilledTonalButton(
                                onClick = { update(IslandCalibration()) },
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary),
                            ) { Text("Reset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            androidx.compose.material3.Button(
                                onClick = {
                                    onChange { current -> current.copy(theme = current.theme.withCalibration(profile, value)) }
                                    scope.launch { finishBack(force = true) }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                shape = CircleShape,
                            ) { Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
        }
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val p = pageProgress.value.coerceIn(0f, 1f)
            translationX = size.width * (1f - p) * backDirection
            scaleX = .97f + .03f * p; scaleY = scaleX
            shape = RoundedCornerShape((24f * (1f - p)).dp)
            clip = p < .999f
            shadowElevation = (12f * (1f - p)).dp.toPx()
        }) {
            renderPage(null)
            selected?.let { profile ->
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val p = editorProgress.value.coerceIn(0f, 1f)
                    translationX = size.width * (1f - p) * backDirection
                    scaleX = .97f + .03f * p; scaleY = scaleX
                    shape = RoundedCornerShape((24f * (1f - p)).dp)
                    clip = p < .999f
                    shadowElevation = (12f * (1f - p)).dp.toPx()
                }) { renderPage(profile) }
            }
        }
        if (selected != null) {
            // Render above the editor in its own window. Calibration must not
            // depend on an accessibility-service token surviving app updates.
            val calibration = if (selected == display.calibrationProfile) editingCalibration
                ?: settings.theme.calibration(display.calibrationProfile)
                else settings.theme.calibration(display.calibrationProfile)
            app.expressiveclock.overlay.DynamicIslandOverlayContent(
                timer = null, stopwatch = null, topInsetDp = 8.dp,
                cutoutWidthDp = 48.dp, isCalibrationPreview = true,
                offsetXDp = calibration.x.dp, offsetYDp = calibration.y.dp,
                widthOffsetDp = calibration.width.dp, heightOffsetDp = calibration.height.dp,
                alignment = calibration.alignment, expandedPosition = calibration.expandedPosition,
                onExpandChanged = {}, onPauseResumeTimer = {}, onAddTenTimer = {},
                onCancelTimer = {}, onToggleStopwatch = {}, onLapStopwatch = {},
            )
        }
        }
    }
}

private enum class SettingsPicker { ALARM_CARD_FORMAT, THEME, SILENCE, SNOOZE, WEEK_START, PRIVACY, PRESETS, STARTUP_PAGE }

@Composable
private fun DefaultStartupPageDialog(
    current: DefaultStartupPage,
    onDismiss: () -> Unit,
    onSelect: (DefaultStartupPage) -> Unit,
) {
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
        onDismissRequest = onDismiss,
        title = { Text("Default startup page") },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column {
                DefaultStartupPage.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        RadioButton(
                            selected = option == current,
                            onClick = { onSelect(option) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val category = LocalSettingsCategory.current
    if (category != null) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(category.icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Text(category.title, style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            content()
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .padding(top = 18.dp, bottom = 8.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun SettingsNavigateRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .expandedSettingsTile()
            .clip(RoundedCornerShape(20.dp))
            .elasticClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .expandedSettingsTile()
            .clip(RoundedCornerShape(20.dp))
            .elasticClickable(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    val dark = LocalSettingsCategory.current == null && isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (dark) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    if (LocalSettingsCategory.current != null) return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 76.dp, end = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    )
}

@Composable
private fun PalettePickerRow(
    dark: Boolean,
    dynamic: Boolean,
    selected: Palette,
    onDynamic: () -> Unit,
    onSelect: (Palette) -> Unit,
) {
    val context = LocalContext.current
    val wallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else null

    Column(Modifier.fillMaxWidth().expandedSettingsTile().padding(vertical = 12.dp)) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(Icons.Outlined.Palette)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Colour theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Personalise the look of the app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 76.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (wallpaper != null) {
                PaletteSwatch(
                    name = "Wallpaper",
                    color = wallpaper.primary,
                    selected = dynamic,
                    onClick = onDynamic,
                )
            }
            Palette.entries.forEach { palette ->
                PaletteSwatch(
                    name = palette.displayName,
                    color = paletteSwatchColor(palette, dark),
                    selected = !dynamic && palette == selected,
                    onClick = { onSelect(palette) },
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                )
                .elasticClickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Selected $name theme",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun paletteSwatchColor(palette: Palette, dark: Boolean): Color {
    // These are deliberately distinct preview colours. The underlying Palette still controls
    // the complete Material color scheme; the swatch is simply a recognisable visual sample.
    val light = when (palette) {
        Palette.CONCEPT -> Color(0xFF006874)
        Palette.VIOLET -> Color(0xFF7D5260)
        Palette.CORAL -> Color(0xFFBA1A1A)
        Palette.EMBER -> Color(0xFF9B4620)
        Palette.CITRON -> Color(0xFF6B6F00)
        Palette.FOREST -> Color(0xFF356859)
        Palette.LAGOON -> Color(0xFF006A6A)
        Palette.AZURE -> Color(0xFF0061A4)
        Palette.FUCHSIA -> Color(0xFFA80078)
    }
    return if (!dark) light else when (palette) {
        Palette.CONCEPT -> Color(0xFF4FD8EB)
        Palette.VIOLET -> Color(0xFFEFB8C8)
        Palette.CORAL -> Color(0xFFFFB4AB)
        Palette.EMBER -> Color(0xFFFFB594)
        Palette.CITRON -> Color(0xFFE0E68A)
        Palette.FOREST -> Color(0xFF9AD0BC)
        Palette.LAGOON -> Color(0xFF80E0E0)
        Palette.AZURE -> Color(0xFFA8C7FA)
        Palette.FUCHSIA -> Color(0xFFFFA9E8)
    }
}

@Composable
private fun <T> ChoicePickerDialog(
    title: String,
    value: T,
    options: List<T>,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = { onSelect(option) })
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == value, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
    )
}

@Composable
private fun ThemePickerDialog(
    value: DarkMode,
    onDismiss: () -> Unit,
    onSelect: (DarkMode) -> Unit,
) {
    ChoicePickerDialog(
        title = "Theme",
        value = value,
        options = DarkMode.entries,
        label = { it.label },
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
        onDismissRequest = onDismiss,
        title = { Text("Privacy") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            Text(
                "No accounts, analytics, crash reporting or advertising ID. The app has no " +
                    "INTERNET permission, so it cannot make network connections. Your alarms, " +
                    "timers, world clocks and preferences remain on your device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}

private fun minutesLabel(minutes: Int): String = when (minutes) {
    0 -> "Never"
    1 -> "1 minute"
    else -> "$minutes minutes"
}

@Composable
fun TimerPresetsDialog(
    presets: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit,
) {
    var currentPresets by remember(presets) {
        mutableStateOf(presets.take(4).let { if (it.size == 4) it else DEFAULT_TIMER_PRESETS })
    }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
        onDismissRequest = onDismiss,
        title = { Text("Timer presets") },
        confirmButton = {
            TextButton(onClick = { onSave(currentPresets) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentPresets.forEachIndexed { index, seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { editingIndex = index }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Preset ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                formatPresetLabel(seconds),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        },
    )

    editingIndex?.let { index ->
        SinglePresetEditDialog(
            title = "Preset ${index + 1}",
            initialSeconds = currentPresets[index],
            onDismiss = { editingIndex = null },
            onConfirm = { newSeconds ->
                val updated = currentPresets.toMutableList()
                updated[index] = newSeconds
                currentPresets = updated
                editingIndex = null
            },
        )
    }
}

@Composable
internal fun SinglePresetEditDialog(
    title: String,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var minText by remember { mutableStateOf((initialSeconds / 60).toString()) }
    var secText by remember { mutableStateOf((initialSeconds % 60).toString()) }

    val quickOptions = remember {
        listOf(
            15 to "15s",
            30 to "30s",
            60 to "1 min",
            120 to "2 min",
            180 to "3 min",
            300 to "5 min",
            600 to "10 min",
            900 to "15 min",
            1200 to "20 min",
            1800 to "30 min",
            2700 to "45 min",
            3600 to "1 hour",
        )
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = !compactSettingsDialog()),
        modifier = if (compactSettingsDialog()) Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth() else Modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = minText.toIntOrNull() ?: 0
                    val s = secText.toIntOrNull() ?: 0
                    val total = (m * 60 + s).coerceIn(1, 86399)
                    onConfirm(total)
                },
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = secText,
                        onValueChange = { secText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    "Quick options",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalEdgeBlurContainer(fadeLength = 16.dp) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(quickOptions) { (secs, label) ->
                            val currentSecs = (minText.toIntOrNull() ?: 0) * 60 + (secText.toIntOrNull() ?: 0)
                            val isSelected = currentSecs == secs
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    .clickable {
                                        minText = (secs / 60).toString()
                                        secText = (secs % 60).toString()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun HorizontalEdgeBlurContainer(
    modifier: Modifier = Modifier,
    fadeLength: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                // Step 1: Draw base scrolling horizontal content
                drawContent()

                val width = size.width
                val height = size.height
                val fadePx = fadeLength.toPx()

                if (width <= 0f || height <= 0f || fadePx <= 0f) return@drawWithContent

                // Step 2: On Android 12+, render genuine progressive hardware blur overlays at left and right edges
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas

                        val blurPaint = Paint().apply {
                            isAntiAlias = true
                            setPaintBlurEffect(this, 24f, Shader.TileMode.DECAL)
                        }

                        // Left edge hardware blur overlay
                        val leftSave = nativeCanvas.saveLayer(
                            0f, 0f, fadePx, height, blurPaint
                        )
                        drawContent()

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.60f),
                                    Color.Transparent,
                                ),
                                startX = 0f,
                                endX = fadePx,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        nativeCanvas.restoreToCount(leftSave)

                        // Right edge hardware blur overlay
                        val rightStart = width - fadePx
                        if (rightStart > 0f) {
                            val rightSave = nativeCanvas.saveLayer(
                                rightStart, 0f, width, height, blurPaint
                            )
                            drawContent()

                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.60f),
                                        Color.Black,
                                    ),
                                    startX = rightStart,
                                    endX = width,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                            nativeCanvas.restoreToCount(rightSave)
                        }
                    }
                }

                // Step 3: Draw smooth alpha gradient falloff at left & right edges
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black,
                        ),
                        startX = 0f,
                        endX = fadePx,
                    ),
                    blendMode = BlendMode.DstIn,
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Transparent,
                        ),
                        startX = width - fadePx,
                        endX = width,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun setPaintBlurEffect(paint: Paint, radius: Float, tileMode: Shader.TileMode) {
    runCatching {
        val method = paint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java)
        method.invoke(paint, RenderEffect.createBlurEffect(radius, radius, tileMode))
    }
}

/** Material's minimum dialog width must yield to real side gutters in compact windows. */
@Composable
private fun compactSettingsDialog(): Boolean =
    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale > 1.1f

/* ── Shake to dismiss & volume fade-in ─────────────────────────────────────────────────────── */

@Composable
private fun ShakeSettingsRows(
    alarms: app.expressiveclock.data.AlarmSettings,
    update: ((app.expressiveclock.data.AlarmSettings) -> app.expressiveclock.data.AlarmSettings) -> Unit,
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    SettingsSwitchRow(
        icon = Icons.Outlined.Vibration,
        title = "Shake to dismiss",
        subtitle = if (alarms.shakeEnabled) "Shake your phone while an alarm rings" else "Off",
        checked = alarms.shakeEnabled,
        onCheckedChange = { v -> update { it.copy(shakeEnabled = v) } },
    )
    AnimatedVisibility(visible = alarms.shakeEnabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            SettingsDivider()
            SettingsNavigateRow(
                icon = Icons.Outlined.Snooze,
                title = "Action",
                subtitle = alarms.shakeAction.label,
                onClick = { dialog = "action" },
            )
            SettingsDivider()
            if (alarms.shakeCalibrated) {
                SettingsNavigateRow(
                    icon = Icons.Outlined.Tune,
                    title = "Sensitivity",
                    subtitle = "Custom (%.1f g)".format(java.util.Locale.US, alarms.shakeCustomG),
                    trailing = "Reset to preset",
                    onClick = { update { it.copy(shakeCustomG = app.expressiveclock.data.SHAKE_NOT_CALIBRATED) } },
                )
            } else {
                SettingsNavigateRow(
                    icon = Icons.Outlined.Tune,
                    title = "Sensitivity",
                    subtitle = sensitivityLabel(alarms.shakeSensitivity),
                    onClick = { dialog = "sensitivity" },
                )
            }
            SettingsDivider()
            SettingsNavigateRow(
                icon = Icons.Outlined.Vibration,
                title = "Calibrate by shaking",
                subtitle = "Measure how hard you shake",
                onClick = { dialog = "calibrate" },
            )
            SettingsDivider()
            SettingsSliderRow(
                title = "Shakes required",
                valueLabel = "${alarms.shakeCount}× within 3 s",
                value = alarms.shakeCount,
                range = app.expressiveclock.data.SHAKE_COUNT_RANGE,
                step = 1,
                onValueChange = { v -> update { it.copy(shakeCount = v) } },
            )
        }
    }
    when (dialog) {
        "action" -> ChoicePickerDialog(
            title = "Action",
            value = alarms.shakeAction,
            options = app.expressiveclock.data.ShakeAction.entries,
            label = { it.label },
            onDismiss = { dialog = null },
            onSelect = { v -> update { it.copy(shakeAction = v) }; dialog = null },
        )
        "sensitivity" -> ChoicePickerDialog(
            title = "Sensitivity",
            value = alarms.shakeSensitivity,
            options = app.expressiveclock.data.ShakeSensitivity.entries,
            label = ::sensitivityLabel,
            onDismiss = { dialog = null },
            onSelect = { v -> update { it.copy(shakeSensitivity = v) }; dialog = null },
        )
        "calibrate" -> ShakeCalibrationDialog(
            onDismiss = { dialog = null },
            onSave = { g -> update { it.copy(shakeCustomG = g) }; dialog = null },
        )
    }
}

private fun sensitivityLabel(s: app.expressiveclock.data.ShakeSensitivity) =
    "${s.label} (%.1fg)".format(java.util.Locale.US, s.g)

@Composable
private fun VolumeSettingsRows(
    alarms: app.expressiveclock.data.AlarmSettings,
    update: ((app.expressiveclock.data.AlarmSettings) -> app.expressiveclock.data.AlarmSettings) -> Unit,
) {
    SettingsSwitchRow(
        icon = Icons.AutoMirrored.Outlined.VolumeUp,
        title = "Fade in volume",
        subtitle = if (alarms.fadeIn) "Alarms start quietly and get louder" else "Off",
        checked = alarms.fadeIn,
        onCheckedChange = { v -> update { it.copy(fadeIn = v) } },
    )
    AnimatedVisibility(visible = alarms.fadeIn, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            SettingsDivider()
            SettingsSliderRow(
                title = "Fade-in duration",
                valueLabel = "${alarms.fadeInDurationSeconds} s",
                value = alarms.fadeInDurationSeconds,
                range = app.expressiveclock.data.FADE_IN_DURATION_RANGE,
                step = 5,
                onValueChange = { v -> update { it.copy(fadeInDurationSeconds = v) } },
            )
            SettingsDivider()
            SettingsSliderRow(
                title = "Start volume",
                valueLabel = "${alarms.startVolumePercent} %",
                value = alarms.startVolumePercent,
                range = app.expressiveclock.data.START_VOLUME_RANGE,
                step = 5,
                onValueChange = { v -> update { it.copy(startVolumePercent = v) } },
            )
        }
    }
}

/** An integer slider that snaps to [step]; writes on release so DataStore is not hammered. */
@Composable
private fun SettingsSliderRow(
    title: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging?.let { snapTo(it, range, step) } ?: value
    Column(
        Modifier
            .fillMaxWidth()
            .expandedSettingsTile()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f))
            Text(if (dragging == null) valueLabel else valueLabel.replaceFirst(value.toString(), shown.toString()),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = (dragging ?: value.toFloat()),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onValueChange(snapTo(it, range, step)) }
                dragging = null
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
        )
    }
}

private fun snapTo(raw: Float, range: IntRange, step: Int): Int =
    (range.first + Math.round((raw - range.first) / step) * step).coerceIn(range.first, range.last)

/**
 * Three seconds of "shake now", recording the strongest acceleration seen. Anything at or below
 * [app.expressiveclock.data.MIN_CALIBRATED_G] is indistinguishable from carrying the phone, so it
 * is rejected rather than saved.
 */
@Composable
private fun ShakeCalibrationDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    val context = LocalContext.current
    var attempt by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(3) }
    var peak by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<Float?>(null) }
    val sensorManager = remember(context) { context.getSystemService(android.hardware.SensorManager::class.java) }
    val sensor = remember(sensorManager) { sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) }
    val measuring = result == null && sensor != null

    DisposableEffect(attempt, measuring) {
        if (!measuring) return@DisposableEffect onDispose { }
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val g = kotlin.math.sqrt(x * x + y * y + z * z) / android.hardware.SensorManager.GRAVITY_EARTH
                if (g > peak) peak = g
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }
    LaunchedEffect(attempt) {
        if (sensor == null) return@LaunchedEffect
        peak = 0f
        result = null
        for (s in 3 downTo 1) {
            secondsLeft = s
            kotlinx.coroutines.delay(1_000)
        }
        secondsLeft = 0
        result = peak
    }

    val tooWeak = result != null && result!! <= app.expressiveclock.data.MIN_CALIBRATED_G
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibrate by shaking") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    sensor == null -> Text("This device has no accelerometer.")
                    result == null -> {
                        Text("Shake your phone now…", style = MaterialTheme.typography.bodyLarge)
                        Text("$secondsLeft", style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    tooWeak -> Text("Too weak – try again")
                    else -> Text("Measured %.1f g".format(java.util.Locale.US, result))
                }
            }
        },
        confirmButton = {
            when {
                sensor == null || result == null -> Unit
                tooWeak -> TextButton(onClick = { attempt++ }) { Text("Try again") }
                else -> TextButton(onClick = { onSave(result!!) }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
