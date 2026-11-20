package app.expressiveclock.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged


import android.Manifest
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import app.expressiveclock.ui.screens.SelectionToolbar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expressiveclock.alarm.Notifications
import app.expressiveclock.core.Alarm
import app.expressiveclock.core.WorldCity
import app.expressiveclock.data.DefaultStartupPage
import app.expressiveclock.data.order
import app.expressiveclock.ui.screens.AlarmsScreen
import app.expressiveclock.ui.screens.OnboardingScreen
import app.expressiveclock.ui.screens.StopwatchScreen
import app.expressiveclock.ui.screens.SettingsScreen
import app.expressiveclock.ui.screens.TimersScreen
import app.expressiveclock.ui.sheets.AlarmEditSheet
import app.expressiveclock.ui.sheets.NewAlarmPickerSheet
import app.expressiveclock.ui.sheets.AddCitySheet
import app.expressiveclock.ui.theme.ClockTheme
import androidx.compose.runtime.CompositionLocalProvider
import app.expressiveclock.ui.responsive.LocalAppWindowSizeInfo
import app.expressiveclock.ui.responsive.rememberAppWindowSizeInfo

/** Breathing room between the dock and the navigation bar. */
private val DOCK_CLEARANCE = 20.dp

/**
 * How much of the bottom of every screen the floating dock is standing on.
 *
 * Derived from [DOCK_HEIGHT] rather than restated as a number. It was written out as a literal 96
 * once ("64 of pill, 20 under it, 12 above the last row"), and the moment the pill grew to 72 the
 * comment was still true and the constant was silently 8 dp short, which shows up as the last row
 * of every list tucked a little too far under the dock. There is nothing to notice and nothing to
 * fail; it just quietly looks wrong.
 */
private val DOCK_RESERVE = DOCK_HEIGHT + DOCK_CLEARANCE + 12.dp

internal val LocalAlarmCards24Hour = androidx.compose.runtime.staticCompositionLocalOf { false }

enum class Tab(val label: String, val icon: ImageVector, val key: String) {
    ALARMS("Alarms", Icons.Outlined.Alarm, Notifications.TAB_ALARMS),
    WORLD("World Clock", Icons.Outlined.Public, "world"),
    TIMERS("Timers", Icons.Outlined.HourglassEmpty, Notifications.TAB_TIMERS),
    STOPWATCH("Stopwatch", Icons.Outlined.Timer, Notifications.TAB_STOPWATCH),
}

/**
 * The app shell.
 *
 * Navigation is [ClockDock], a floating pill with a detached add button and no navigation bar, so
 * the alarm grid and the world-clock list run full-bleed to the bottom edge. That component owes
 * the accessibility contract a `ShortNavigationBar` would have supplied for free; see its own
 * documentation for what it pays and why.
 *
 * Settings are always available from the top-right gear, regardless of which page is visible.
 */
@Composable
fun ClockApp(startTab: String? = null, startRequestId: Long = 0L, vm: ClockViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val isSettingsLoaded by vm.isSettingsLoaded.collectAsStateWithLifecycle()
    val cities by vm.cities.collectAsStateWithLifecycle()
    val windowSizeInfo = rememberAppWindowSizeInfo()

    val worldGrid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val worldCollapse = rememberSaveable { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val worldPresentation = remember(worldGrid) { app.expressiveclock.ui.screens.WorldClockPresentation(worldGrid, worldCollapse) }
    val analogueMotion = rememberSaveable(stateSaver = app.expressiveclock.ui.screens.ClockHandMotionSaver) {
        mutableStateOf(app.expressiveclock.ui.screens.ClockHandMotion(java.time.ZoneId.systemDefault()))
    }
    ClockTheme(settings.theme) {
        CompositionLocalProvider(LocalAppWindowSizeInfo provides windowSizeInfo,
            app.expressiveclock.ui.screens.LocalWorldClockPresentation provides worldPresentation,
            app.expressiveclock.ui.screens.LocalClockHandMotion provides analogueMotion) {
            val defaultTab = when (settings.defaultStartupPage) {
                DefaultStartupPage.ALARMS -> Tab.ALARMS
                DefaultStartupPage.WORLD -> Tab.WORLD
                DefaultStartupPage.TIMER -> Tab.TIMERS
                DefaultStartupPage.STOPWATCH -> Tab.STOPWATCH
            }

        var tab by rememberSaveable { mutableStateOf<Tab?>(null) }
        var handledStartRequest by rememberSaveable { mutableStateOf<Long?>(null) }

        // Hydrate startup tab once settings load from DataStore on cold start
        LaunchedEffect(isSettingsLoaded, startTab, startRequestId) {
            val navigation = resolveStartupNavigation(tab, startTab, startRequestId, handledStartRequest,
                isSettingsLoaded, defaultTab)
            tab = navigation.tab
            handledStartRequest = navigation.handledRequestId
        }

        val activeTab = tab ?: startTab?.let { k -> Tab.entries.firstOrNull { it.key == k } } ?: defaultTab
        LaunchedEffect(activeTab) {
            if (activeTab != Tab.WORLD) {
                worldPresentation.deletionSelection.value = emptySet()
                worldPresentation.deletionConfirmation.value = false
            }
        }

        var editing by remember { mutableStateOf<Alarm?>(null) }
        var showNewAlarmPicker by remember { mutableStateOf(false) }
        var showAddCitySheet by rememberSaveable { mutableStateOf(false) }
        var selectedWorldCityZoneId by rememberSaveable { mutableStateOf<String?>(null) }
        var alarmSelectionCount by remember { mutableIntStateOf(0) }
        var clearSelectionSignal by remember { mutableIntStateOf(0) }
        var deleteClickSignal by remember { mutableIntStateOf(0) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val settingsProgress = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val wideClockPages = app.expressiveclock.ui.screens.useWideClockPages(
            windowSizeInfo.screenWidthDp.value, windowSizeInfo.screenHeightDp.value,
            androidx.compose.ui.platform.LocalDensity.current.fontScale)

        fun openSettings() {
            showSettings = true
            scope.launch {
                settingsProgress.snapTo(0f)
                settingsProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                )
            }
        }

        fun closeSettings() {
            scope.launch {
                settingsProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                )
                showSettings = false
            }
        }

        PredictiveBackHandler(enabled = showSettings) { backEvents ->
            try {
                backEvents.collect { event ->
                    settingsProgress.snapTo(1f - event.progress)
                }
                settingsProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
                showSettings = false
            } catch (_: CancellationException) {
                settingsProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
            }
        }
        val snackbar = remember { SnackbarHostState() }
        fun saveWithConfirmation(draft: Alarm) {
            vm.saveAlarm(draft) { saved ->
                val now = java.time.ZonedDateTime.now()
                saved.nextFire(now)?.let { next ->
                    scope.launch { snackbar.showSnackbar("Alarm in ${app.expressiveclock.core.humanUntil(now, next)}") }
                }
            }
        }

        // Asked once, on first composition. Denying it costs the notifications and nothing else.
        // The alarm still rings, because the ringer is a foreground service and audio, not a post.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }

        val showOnboarding = !settings.hasCompletedInitialSetup
        var isPostOnboardingStaggerToken by rememberSaveable { mutableIntStateOf(0) }

        val onboardingTransition = updateTransition(targetState = showOnboarding, label = "onboardingTransition")

        onboardingTransition.AnimatedContent(
            transitionSpec = {
                if (!targetState) {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f),
                        initialOffsetX = { width -> width },
                    ) + fadeIn(tween(220)) togetherWith
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f),
                            targetOffsetX = { width -> -width },
                        ) + fadeOut(tween(200))
                } else {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { isSetup ->
            if (isSetup) {
                OnboardingScreen(
                    onComplete = {
                        isPostOnboardingStaggerToken = 1
                        vm.updateSettings { it.copy(hasCompletedInitialSetup = true) }
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = settingsProgress.value
                                translationX = -size.width * 0.12f * p
                                alpha = 1f - 0.06f * p
                            },
                    ) {
                Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = DOCK_RESERVE)) },
            topBar = {
                if ((activeTab == Tab.ALARMS && alarmSelectionCount > 0) ||
                    (activeTab == Tab.WORLD && worldPresentation.deletionSelection.value.isNotEmpty())) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(52.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SelectionToolbar(
                            selectedCount = if (activeTab == Tab.WORLD) worldPresentation.deletionSelection.value.size else alarmSelectionCount,
                            onClearSelection = {
                                if (activeTab == Tab.WORLD) worldPresentation.deletionSelection.value = emptySet()
                                else clearSelectionSignal++
                            },
                            onDeleteClick = {
                                if (activeTab == Tab.WORLD) worldPresentation.deletionConfirmation.value = true
                                else deleteClickSignal++
                            },
                            deleteDescription = if (activeTab == Tab.WORLD) "Delete selected locations" else "Delete selected alarms",
                        )
                    }
                } else {
                    TopAppBar(
                        title = {},
                        actions = {
                            IconButton(onClick = ::openSettings) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            },
        ) { padding ->
            // Keep the bars' insets but let each screen own its horizontal margin, so a grid
            // and a full-bleed dial can differ without fighting the Scaffold.
            //
            // The dock floats over the content rather than displacing it, so the space it occupies
            // has to be reserved here by hand; otherwise the last alarm tile sits under the pill.
            val ld = LocalLayoutDirection.current
            val underDock = padding.calculateBottomPadding() + DOCK_RESERVE
            val body = PaddingValues(
                start = padding.calculateStartPadding(ld) + 16.dp,
                end = padding.calculateEndPadding(ld) + 16.dp,
                top = padding.calculateTopPadding(),
                bottom = underDock,
            )
            val edgeToEdge = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = underDock,
            )

          GlassBackdrop(Modifier.fillMaxSize()) {
            val pagerState = rememberPagerState(initialPage = activeTab.ordinal, pageCount = { Tab.entries.size })
            var pagerNavigationTarget by remember { mutableStateOf<Int?>(null) }
            var pagerNavigationRequest by remember { mutableIntStateOf(0) }
            LaunchedEffect(activeTab, pagerNavigationRequest) {
                val destination = activeTab.ordinal
                pagerNavigationTarget = destination
                // A return tap can target settledPage while the pager is still between pages.
                // Cancel/retarget immediately instead of leaving that interrupted scroll unfinished.
                val request = pagerNavigationRequest
                try {
                    if (pagerState.currentPage != destination || pagerState.currentPageOffsetFraction != 0f) {
                        pagerState.animateScrollToPage(destination,
                            animationSpec = spring(stiffness = 380f, dampingRatio = .86f))
                    }
                } finally {
                    // A user drag can cancel the animation. Release that request,
                    // without clearing a newer bottom-bar tap that replaced it.
                    if (request == pagerNavigationRequest) pagerNavigationTarget = null
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { Triple(pagerState.settledPage, pagerNavigationTarget, pagerState.isScrollInProgress) }
                    .distinctUntilChanged().drop(1).collect { (page, requested, scrolling) ->
                        // Intermediate programmatic settlements must not overwrite a newer tap.
                        if (requested == null && !scrolling) tab = Tab.entries[page]
                    }
            }
            // The same retained pager serves compact and wide layouts. Each wide clock
            // destination keeps its two-pane composition instead of showing the phone screen.
            val pageContent: @Composable (Tab) -> Unit = { current ->
                when (if (current == Tab.WORLD) Tab.ALARMS else current) {
                    Tab.ALARMS -> {
                        val alarms by vm.alarms.collectAsStateWithLifecycle()
                        val alarmCards: @Composable (Modifier, Boolean, PaddingValues) -> Unit = { layout, single, insets ->
                            CompositionLocalProvider(LocalAlarmCards24Hour provides true) {
                                AlarmsScreen(alarms = alarms, weekStart = settings.alarms.weekStart,
                                    staggerEntranceToken = isPostOnboardingStaggerToken,
                                    onStaggerConsumed = { isPostOnboardingStaggerToken = 0 },
                                    onToggle = vm::toggleAlarm, onEdit = { editing = it },
                                    onAddAlarm = { showNewAlarmPicker = true },
                                    onDeleteAlarms = { vm.deleteAlarms(it) },
                                    onSelectionCountChange = { alarmSelectionCount = it },
                                    clearSelectionSignal = clearSelectionSignal, deleteClickSignal = deleteClickSignal,
                                    editingAlarm = editing, widePane = single,
                                    contentPadding = insets, modifier = layout)
                            }
                        }
                        if (wideClockPages) {
                            app.expressiveclock.ui.screens.WideClockPages(
                                worldSelected = current == Tab.WORLD,
                                cities = cities, selectedZoneId = selectedWorldCityZoneId,
                                onSelectCity = { selectedWorldCityZoneId = it.zone.id },
                                onShowLocalTime = { selectedWorldCityZoneId = null },
                                alarmContent = { alarmCards(it, true, PaddingValues(vertical = 8.dp)) },
                                showAlarmEditor = editing != null,
                                alarmEditor = {
                                    app.expressiveclock.ui.screens.InlineAlarmEditorPanel(
                                        editingAlarm = editing,
                                        weekStart = settings.alarms.weekStart,
                                        order = settings.alarms.weekStart.order(app.expressiveclock.ui.sheets.systemFirstDay()),
                                        onSave = { saveWithConfirmation(it); editing = null; showNewAlarmPicker = false },
                                        onCancel = { editing = null; showNewAlarmPicker = false },
                                        onDelete = { vm.deleteAlarms(setOf(it)); editing = null; showNewAlarmPicker = false })
                                },
                                modifier = Modifier.padding(body))
                        } else {
                            val phoneTab = current
                            androidx.compose.runtime.key(phoneTab) {
                                if (phoneTab == Tab.WORLD) app.expressiveclock.ui.screens.WorldClockPanel(
                                    flatPortrait = true, cities = cities, selectedZoneId = selectedWorldCityZoneId,
                                    onSelectCity = { selectedWorldCityZoneId = it.zone.id },
                                    onShowLocalTime = { selectedWorldCityZoneId = null }, modifier = Modifier.padding(body))
                                else alarmCards(Modifier, false, PaddingValues(start = 16.dp, end = 16.dp,
                                    top = padding.calculateTopPadding(), bottom = underDock))
                            }
                        }
                    }
                    Tab.WORLD -> Unit // Routed to the clock content branch above.

                    Tab.TIMERS -> {
                        val timer by vm.timer.collectAsStateWithLifecycle()
                        // Only tick while a timer exists; the setting screen has nothing moving.
                        val now by rememberElapsedTicker(active = timer != null)
                        TimersScreen(
                            timer = timer,
                            draft = vm.draftDuration,
                            nowElapsedMillis = now,
                            presets = settings.timers.presets,
                            onDigit = vm::pressDigit,
                            onBackspace = vm::backspace,
                            onResetDraft = vm::resetTimerDraft,
                            onPreset = vm::setTimerPreset,
                            onEditPreset = { index, seconds ->
                                vm.updateSettings { current ->
                                    current.copy(timers = current.timers.copy(
                                        presets = current.timers.presets.mapIndexed { presetIndex, value ->
                                            if (presetIndex == index) seconds else value
                                        },
                                    ))
                                }
                            },
                            onStart = { vm.startTimer() },
                            onPauseResume = { vm.pauseOrResumeTimer() },
                            onAddTen = { vm.addTenSeconds() },
                            onCancel = { vm.cancelTimer() },
                            contentPadding = edgeToEdge,
                        )
                    }

                    Tab.STOPWATCH -> {
                        val sw by vm.stopwatch.collectAsStateWithLifecycle()
                        val now by rememberElapsedTicker(active = sw.running)
                        StopwatchScreen(
                            stopwatch = sw,
                            nowElapsedMillis = now,
                            onToggle = { vm.toggleStopwatch() },
                            onLap = { vm.lap() },
                            onReset = { vm.resetStopwatch() },
                            contentPadding = edgeToEdge,
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = Tab.entries.size - 1,
                key = { Tab.entries[it].key },
                userScrollEnabled = !showSettings && !showAddCitySheet && !showNewAlarmPicker && editing == null &&
                    alarmSelectionCount == 0 && worldPresentation.deletionSelection.value.isEmpty(),
                flingBehavior = PagerDefaults.flingBehavior(pagerState,
                    snapAnimationSpec = spring(stiffness = 380f, dampingRatio = .86f)),
                modifier = Modifier.fillMaxSize(),
            ) { pageContent(Tab.entries[it]) }

            val dockTab = if (pagerNavigationTarget == null)
                Tab.entries[pagerState.currentPage] else activeTab
            ClockDock(
                destinations = listOf(Tab.ALARMS, Tab.WORLD, Tab.TIMERS, Tab.STOPWATCH),
                selected = dockTab,
                pagePosition = {
                    pagerState.currentPage + pagerState.currentPageOffsetFraction
                },
                onSelect = {
                    pagerNavigationTarget = it.ordinal
                    pagerNavigationRequest++
                    tab = it
                },
                // Timer and Stopwatch retain their existing in-content actions.
                showAdd = dockTab == Tab.ALARMS || dockTab == Tab.WORLD,
                addLabel = if (dockTab == Tab.WORLD) "Add Location" else "Add alarm",
                onAdd = { if (dockTab == Tab.WORLD) showAddCitySheet = true else showNewAlarmPicker = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = DOCK_CLEARANCE),
                enableGlow = false,
            )
          }
            }
            }

            if (showSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = settingsProgress.value
                            translationX = size.width * (1f - p)
                            alpha = 0.92f + 0.08f * p
                        },
                ) {
                    SettingsScreen(
                        settings = settings,
                        onChange = vm::updateSettings,
                        onBack = ::closeSettings,
                        onReopenOnboarding = {
                            closeSettings()
                            vm.updateSettings { it.copy(hasCompletedInitialSetup = false) }
                        },
                    )
                }
            }
        }
    }
}

        if (!wideClockPages) {
            editing?.let { draft ->
                AlarmEditSheet(
                    initial = draft,
                    weekStart = settings.alarms.weekStart,
                    onDismiss = { editing = null },
                    onSave = { saveWithConfirmation(it); editing = null },
                    onDelete = if (draft.id == 0L) null else ({ id -> vm.deleteAlarm(id) }),
                )
            }
        }

        if (showNewAlarmPicker) {
            NewAlarmPickerSheet(
                initial = vm.blankAlarm(),
                weekStart = settings.alarms.weekStart,
                onDismiss = { showNewAlarmPicker = false },
                onSave = { alarm -> saveWithConfirmation(alarm); showNewAlarmPicker = false },
            )
        }

        if (showAddCitySheet) {
            AddCitySheet(
                existing = cities,
                nowUtcMillis = System.currentTimeMillis(),
                onAdd = { city: WorldCity ->
                    vm.addCity(city)
                    selectedWorldCityZoneId = city.zone.id
                },
                onDismiss = { showAddCitySheet = false },
            )
        }
        }
    }
}
