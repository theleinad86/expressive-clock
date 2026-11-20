package app.expressiveclock.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import android.os.Build
import androidx.compose.foundation.border
import app.expressiveclock.ui.WorldClockCardTime
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.expressiveclock.ui.ClockViewModel
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.BackHandler
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import app.expressiveclock.core.WorldCity
import app.expressiveclock.ui.rememberWallTicker
import app.expressiveclock.ui.theme.WorldClockType
import androidx.compose.ui.text.rememberTextMeasurer
import app.expressiveclock.ui.sheets.prettify
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The expanded Alarms pane's world-clock surface. It deliberately owns only presentation and the
 * transient selection; city persistence remains in [ClockStore] through [ClockViewModel].
 */
private val LocalWorldHourFormat = androidx.compose.runtime.staticCompositionLocalOf { app.expressiveclock.data.HourFormat.SYSTEM }

@Composable
fun WorldClockPanel(
    flatPortrait: Boolean = false,
    hourFormat: app.expressiveclock.data.HourFormat = app.expressiveclock.data.HourFormat.SYSTEM,
    cities: List<WorldCity>,
    selectedZoneId: String?,
    onSelectCity: (WorldCity) -> Unit,
    onShowLocalTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by rememberWallTicker()
    val localCity = prettify(ZoneId.systemDefault().id).copy(region = "Local Time")
    val selected = cities.firstOrNull { it.zone.id == selectedZoneId } ?: localCity
    val presentation = LocalWorldClockPresentation.current
    val state = presentation.grid
    var collapse by presentation.collapse
    val density = LocalDensity.current
    var snapJob by remember { mutableStateOf<Job?>(null) }
    suspend fun settle(velocity: Float = 0f) = coroutineScope {
        snapJob?.cancel()
        if (collapse > 0f && collapse < 1f) {
            val start = collapse
            val target = worldClockSnapTarget(start, velocity)
            val job = launch {
                animate(start, target, animationSpec = tween(260, easing = FastOutSlowInEasing)) { value, _ -> collapse = value }
            }
            snapJob = job
            job.join()
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collectLatest { scrolling ->
            if (!scrolling) settle()
        }
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface, LocalWorldHourFormat provides hourFormat) {
        BoxWithConstraints(modifier.fillMaxSize().worldClockAmbient(MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surface.luminance() < .5f)) {
            val detailsHeight = worldClockDetailsHeight(selected, now, maxWidth)
            val normalDial = minOf(maxWidth - 24.dp, maxHeight * .48f, 320.dp).coerceAtLeast(120.dp)
            val dialSize = if (density.fontScale > 1f || maxWidth < 360.dp)
                minOf(normalDial, (maxHeight - detailsHeight - 124.dp * density.fontScale).coerceAtLeast(100.dp))
                else normalDial
            val range = with(density) { dialSize.toPx() }
            val connection = remember(range, state) {
                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                    fun consume(y: Float): Offset {
                        val old = collapse
                        collapse = (collapse - y / range).coerceIn(0f, 1f)
                        return Offset(0f, (old - collapse) * range)
                    }
                    override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                        if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) snapJob?.cancel()
                        return if (available.y < 0f || (available.y > 0f && !state.canScrollBackward))
                            consume(available.y) else Offset.Zero
                    }
                    override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset =
                        if (available.y > 0f) consume(available.y) else Offset.Zero
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (collapse <= 0f || collapse >= 1f) return Velocity.Zero
                        settle(-available.y / density.density)
                        return Velocity(0f, available.y)
                    }
                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        settle(-available.y / density.density)
                        return Velocity.Zero
                    }

                }
            }
            Column(Modifier.fillMaxSize()
                .then(if (flatPortrait) Modifier else Modifier.clip(RoundedCornerShape(28.dp)))
                .nestedScroll(connection)
                .padding(horizontal = if (flatPortrait) 0.dp else 12.dp)) {
                WorldClockHeader(selected, now, dialSize, collapse, detailsHeight)
                SavedLocationGrid(
                    cities = cities, localCity = localCity, onShowLocalTime = onShowLocalTime,
                    selectedZoneId = selectedZoneId, nowUtcMillis = now, onSelectCity = onSelectCity,
                    state = state,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun SavedLocationGrid(
    cities: List<WorldCity>,
    localCity: WorldCity,
    onShowLocalTime: () -> Unit,
    selectedZoneId: String?,
    nowUtcMillis: Long,
    onSelectCity: (WorldCity) -> Unit,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier = Modifier,
    entranceVisible: Boolean? = null,
    centreShortContent: Boolean = false,
) {
    BoxWithConstraints(modifier.heightIn(min = 120.dp)) {
    val columns = if (maxWidth >= 132.dp * LocalDensity.current.fontScale * 2 + 10.dp) 2 else 1
    val cardWidth = (maxWidth - 16.dp - 10.dp * (columns - 1)) / columns
    // Lazy grids clip at their viewport. Give the exterior glow room to become fully
    // transparent, compensating in content padding so card geometry never changes.
    val glowGutter = maxOf(32.dp, cardWidth * .22f)
    val drawingWidth = maxWidth + glowGutter * 2
    val cardHeight = 124.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val scope = rememberCoroutineScope()
    val vm: ClockViewModel = viewModel()
    var displayedCities by remember { mutableStateOf(cities) }
    var exitingIds by remember { mutableStateOf(emptySet<String>()) }
    val presentation = LocalWorldClockPresentation.current
    var selectedIds by presentation.deletionSelection
    var confirmDelete by presentation.deletionConfirmation
    val view = LocalView.current
    val selectionMode = selectedIds.isNotEmpty()
    BackHandler(enabled = selectionMode && !confirmDelete && entranceVisible != false) { selectedIds = emptySet() }
    LaunchedEffect(entranceVisible) {
        if (entranceVisible == false) { selectedIds = emptySet(); confirmDelete = false }
    }
    LaunchedEffect(cities) {
        val sourceIds = cities.map { it.zone.id }.toSet()
        selectedIds = selectedIds.intersect(sourceIds)
        val removed = displayedCities.map { it.zone.id }.toSet() - sourceIds
        if (removed.isEmpty()) displayedCities = cities
        else {
            exitingIds = exitingIds + removed
            val source = cities.associateBy { it.zone.id }
            val oldIds = displayedCities.map { it.zone.id }.toSet()
            displayedCities = displayedCities.map { source[it.zone.id] ?: it } + cities.filterNot { it.zone.id in oldIds }
            delay(360) // Same 260ms exit + removal margin as Alarm cards.
            displayedCities = cities
            exitingIds = exitingIds - removed
        }
    }
    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }
    if (confirmDelete && selectedIds.isNotEmpty()) DeleteItemsConfirmationSheet(
        count = selectedIds.size, itemName = "location", onDismiss = { confirmDelete = false },
        onConfirmDelete = {
            val ids = selectedIds
            val remaining = cities.filterNot { it.zone.id in ids }
            if (selectedZoneId in ids) {
                remaining.firstOrNull()?.let(onSelectCity) ?: onShowLocalTime()
            }
            // Deleting is a new user action: release any addition coordinator so its
            // placement suppression cannot survive into the removal/reflow animation.
            vm.finishCityEntrance()
            vm.removeCities(cities.filter { it.zone.id in ids }.map { it.zone }.toSet())
            selectedIds = emptySet(); confirmDelete = false
        })
    // The wide pane owns a full-height viewport. Short lists are centred through content
    // padding, never by shrinking the viewport (which would move its falloff bands).
    val rowCount = (displayedCities.size + 1 + columns - 1) / columns
    val rowsHeight = (cardHeight + 10.dp) * rowCount - 10.dp
    val emptyMessageHeight = if (displayedCities.isEmpty()) 48.dp else 0.dp
    val gridViewportHeight = (maxHeight - emptyMessageHeight).coerceAtLeast(1.dp)
    val wideOverflow = rowsHeight > gridViewportHeight - 80.dp
    val targetGutter = if (centreShortContent && !wideOverflow)
        maxOf(40.dp, (gridViewportHeight - rowsHeight) / 2) else 40.dp
    val gutterMotion = remember { Animatable(targetGutter, androidx.compose.ui.unit.Dp.VectorConverter) }
    LaunchedEffect(targetGutter) {
        gutterMotion.animateTo(targetGutter, tween(420, easing = FastOutSlowInEasing))
    }
    val verticalGutter = gutterMotion.value
    val edgeStrength = if (!centreShortContent || wideOverflow) 1f else 0f
    var revealedCity by remember { mutableStateOf<String?>(null) }
    var completedScrollCity by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    LaunchedEffect(vm.newlyAddedCity, cities, columns) {
        val id = vm.newlyAddedCity ?: return@LaunchedEffect
        if (cities.none { it.zone.id == id }) return@LaunchedEffect
        completedScrollCity = null
        snapshotFlow {
            val index = displayedCities.indexOfFirst { it.zone.id == id } + 1
            Triple(index, state.layoutInfo.totalItemsCount, state.layoutInfo.viewportSize.height)
        }.first { (index, count, height) -> index > 0 && count == displayedCities.size + 1 && height > 0 }
        // Reveal alongside layout movement, not as a second pop after scrolling stops.
        revealedCity = id
        try {
            snapshotFlow { !gutterMotion.isRunning && gutterMotion.value == targetGutter }
                .first { it }
            androidx.compose.runtime.withFrameNanos { } // consume the final padding measurement
            val index = displayedCities.indexOfFirst { it.zone.id == id } + 1
            state.scroll {
                val layout = state.layoutInfo
                val anchor = layout.visibleItemsInfo.firstOrNull() ?: return@scroll
                val stride = with(density) { (cardHeight + 10.dp).toPx() }
                val targetTop = anchor.offset.y + (index / columns - anchor.index / columns) * stride
                // Exact bottom reveal for uniform cards, including measured content padding.
                // Clamp before animation, so it cannot hit the end early and coast against it.
                val distance = (targetTop + with(density) { cardHeight.toPx() } +
                    layout.afterContentPadding - layout.viewportEndOffset).coerceAtLeast(0f)
                if (distance > .5f) {
                    var previous = 0f
                    val duration = (420 + distance / density.density * .35f).toInt().coerceIn(420, 750)
                    animate(0f, distance, animationSpec = tween(duration, easing = FastOutSlowInEasing)) { value, _ ->
                        scrollBy(value - previous)
                        previous = value
                    }
                }
            }
        } finally {
            if (vm.newlyAddedCity == id) {
                revealedCity = id
                completedScrollCity = id
            }
        }
    }
    // Retain one entrance state per identity outside lazy composition. Cards and their
    // viewport-owned halos must use the exact same phase, including interrupted switches.
    // Freeze the item snapshot used by this lazy content and its animation maps.
    // Effects can update displayedCities before LazyGrid rebuilds its item provider.
    val frameCities = displayedCities
    val pageEntries = (listOf("local") + frameCities.map { it.zone.id }).mapIndexed { index, id ->
        id to key(id) { rememberCityPageEntry(entranceVisible, index) }
    }.toMap()
    val arrivalAlphas = (listOf("local") + frameCities.map { it.zone.id }).associateWith { id ->
        key(id) {
            // Opacity must not use the overshooting scale spring. Share this monotonic
            // fade with the viewport halo, and retain it after the pending flag clears.
            animateFloatAsState(if (vm.newlyAddedCity == id && revealedCity != id) 0f else 1f,
                tween(220), label = "cityArrivalOpacity")
        }
    }
    val glowEntries = (listOf("local") + frameCities.map { it.zone.id }).mapIndexed { index, id ->
        key(id) {
            val isSelected = if (id == "local") !selectionMode && selectedZoneId == null
                else if (selectionMode) id in selectedIds else id == selectedZoneId
            val strength = animateFloatAsState(if (isSelected && id !in exitingIds) 1f else 0f,
                tween(280), label = "viewportCityGlow")
            CityViewportGlow(index, strength, arrivalAlphas.getValue(id), pageEntries[id])
        }
    }
    val glowAccent = MaterialTheme.colorScheme.primary
    val glowDark = MaterialTheme.colorScheme.surface.luminance() < .5f
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f)
                .wrapContentWidth(unbounded = true).requiredWidth(drawingWidth)
                .progressiveListEdgeMask(topStrength = edgeStrength, bottomStrength = edgeStrength)
                .drawBehind {
                    val anchor = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    if (anchor != null) {
                        val w = cardWidth.toPx()
                        val stride = (cardHeight + 10.dp).toPx()
                        glowEntries.forEach { glow ->
                            val entry = glow.entry?.value ?: 1f
                            val alpha = glow.strength.value * glow.reveal.value * entry
                            if (alpha > 0f) {
                                val x = (8.dp + glowGutter).toPx() + (glow.index % columns) * (w + 10.dp.toPx()) +
                                    w * .52f + (1f - entry) * 22.dp.toPx()
                                // LazyGrid item offsets exclude beforeContentPadding. Drawing
                                // coordinates include it, especially for vertically centred lists.
                                val y = state.layoutInfo.beforeContentPadding + anchor.offset.y +
                                    (glow.index / columns - anchor.index / columns) * stride + cardHeight.toPx() * .88f
                                val centre = Offset(x, y)
                                val radius = w * .68f
                                val brush = Brush.radialGradient(listOf(glowAccent.copy(alpha = if (glowDark) .045f else .15f),
                                    Color.Transparent), centre, radius)
                                withTransform({ scale(1f, .55f, centre) }) {
                                    drawCircle(brush, radius, centre, alpha = alpha)
                                }
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(start = 8.dp + glowGutter, end = 8.dp + glowGutter,
                top = verticalGutter, bottom = verticalGutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "local") {
                val entry = pageEntries["local"]
                Box(Modifier.fillMaxWidth().cityPageEntrance(entry, cardWidth), Alignment.Center) {
                    CityGridCard(city = localCity, selected = !selectionMode && selectedZoneId == null,
                        nowUtcMillis = nowUtcMillis, onClick = { selectedIds = emptySet(); onShowLocalTime() },
                        cardHeight = cardHeight, selectionMode = selectionMode, modifier = Modifier.width(cardWidth))
                }
            }
            items(frameCities, key = { it.zone.id }) { city ->
                val entry = pageEntries[city.zone.id]
                val exiting = city.zone.id in exitingIds
                val exitAlpha by animateFloatAsState(if (exiting) 0f else 1f, tween(260), label = "cityDeleteFade")
                val exitScale by animateFloatAsState(if (exiting) .90f else 1f,
                    spring(dampingRatio = .78f, stiffness = 500f), label = "cityDeleteScale")
                val lightBounds = remember(cardWidth) { cityLightLayerOutsets(cardWidth) }
                Box(Modifier.animateItem(
                    fadeInSpec = null, fadeOutSpec = null,
                    // Match Alarm cards. Only the centred-padding phase of an addition
                    // owns placement; ordinary scrolling must not disable the spring.
                    placementSpec = if (vm.newlyAddedCity != null && gutterMotion.isRunning) null
                        else spring(dampingRatio = .68f, stiffness = 380f))
                    .fillMaxWidth().height(cardHeight).cityPageEntrance(entry, cardWidth), Alignment.Center) {
                    LocationCard(
                        cardHeight = cardHeight, city = city,
                        showSelectionBadge = selectionMode,
                        selected = if (selectionMode) city.zone.id in selectedIds else city.zone.id == selectedZoneId,
                        nowUtcMillis = nowUtcMillis,
                        onSelect = { if (selectionMode) toggleSelection(city.zone.id) else onSelectCity(city) },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            toggleSelection(city.zone.id)
                        },
                        onRequestDelete = { selectedIds = selectedIds + city.zone.id; confirmDelete = true },
                        enabled = !exiting,
                        entering = revealedCity == city.zone.id && vm.newlyAddedCity == city.zone.id,
                        pendingEntrance = vm.newlyAddedCity == city.zone.id,
                        arrivalAlpha = arrivalAlphas.getValue(city.zone.id),
                        modifier = Modifier.width(cardWidth).graphicsLayer {
                            outsets = lightBounds
                            alpha = exitAlpha; scaleX = exitScale; scaleY = exitScale
                        },
                        onEntranceFinished = {
                            scope.launch {
                                // Entrance can finish while centred padding is still moving.
                                // Do not clear the pending key and cancel the scroll coordinator.
                                snapshotFlow { vm.newlyAddedCity != city.zone.id ||
                                    (completedScrollCity == city.zone.id && !state.isScrollInProgress) }.first { it }
                                if (vm.newlyAddedCity == city.zone.id) vm.finishCityEntrance()
                            }
                        },
                    )
                }
            }

        }
        if (displayedCities.isEmpty()) {
            Text(
                "Add a city to begin your world clock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
}

@Composable
private fun rememberCityPageEntry(visible: Boolean?, index: Int): androidx.compose.runtime.State<Float>? {
    if (visible == null) return null
    return animateFloatAsState(if (visible) 1f else 0f,
        tween(350, delayMillis = if (visible) index.coerceAtMost(8) * 40 else 0,
            easing = FastOutSlowInEasing), label = "cityPageEntrance")
}

private fun Modifier.cityPageEntrance(entry: androidx.compose.runtime.State<Float>?, cardWidth: Dp): Modifier {
    if (entry == null) return this
    val lightBounds = cityLightLayerOutsets(cardWidth)
    return graphicsLayer {
        // Fractional alpha promotes this layer to an offscreen buffer even with clip=false.
        // Preserve the badge, diffuse shadow and glow outside the measured card rectangle.
        outsets = lightBounds
        alpha = entry.value
        translationX = (1f - entry.value) * 22.dp.toPx()
    }
}

internal fun cityLightLayerOutsets(cardWidth: Dp): LayerOutsets {
    val side = maxOf(32.dp, cardWidth * .22f)
    return LayerOutsets(left = side, top = 20.dp, right = side,
        bottom = maxOf(32.dp, cardWidth * .40f))
}

@Composable
private fun LocationCard(
    cardHeight: Dp, city: WorldCity, selected: Boolean, nowUtcMillis: Long,
    showSelectionBadge: Boolean,
    onSelect: () -> Unit, onLongPress: () -> Unit, onRequestDelete: () -> Unit,
    enabled: Boolean, entering: Boolean, pendingEntrance: Boolean,
    arrivalAlpha: androidx.compose.runtime.State<Float>,
    onEntranceFinished: () -> Unit, modifier: Modifier = Modifier,
) {
    val entrance = remember(city.zone.id) { Animatable(if (pendingEntrance) 0f else 1f) }
    LaunchedEffect(entering, pendingEntrance) {
        if (!pendingEntrance) { entrance.snapTo(1f); return@LaunchedEffect }
        if (!entering) return@LaunchedEffect
        entrance.animateTo(1f, spring(dampingRatio = .65f, stiffness = 380f))
        onEntranceFinished()
    }
    BoxWithConstraints(modifier.height(cardHeight)) {
        val lightBounds = remember(maxWidth) { cityLightLayerOutsets(maxWidth) }
        CityGridCard(city = city, selected = selected, nowUtcMillis = nowUtcMillis, cardHeight = cardHeight,
            showSelectionBadge = showSelectionBadge, selectionMode = showSelectionBadge,
            onClick = { if (enabled && !pendingEntrance) onSelect() },
            onLongClick = { if (enabled && !pendingEntrance) onLongPress() },
            deleteAction = CustomAccessibilityAction("Delete ${city.city}") {
                if (enabled && !pendingEntrance) { onRequestDelete(); true } else false
            },
            modifier = Modifier.graphicsLayer {
                outsets = lightBounds
                scaleX = .9f + .1f * entrance.value
                scaleY = .9f + .1f * entrance.value
                alpha = arrivalAlpha.value
            })
    }
}

@Composable
private fun CityGridCard(
    city: WorldCity, selected: Boolean, nowUtcMillis: Long, onClick: () -> Unit,
    modifier: Modifier = Modifier, cardHeight: Dp = 124.dp,
    onLongClick: (() -> Unit)? = null,
    deleteAction: CustomAccessibilityAction? = null,
    showSelectionBadge: Boolean = false,
    selectionMode: Boolean = false,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val local = city.timeAt(nowUtcMillis)
    // The app always shows 24-hour time.
    val use24h = true
    val time = local.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm", locale))
    val date = local.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val dark = scheme.surface.luminance() < .5f
    val coordinates = remember(city.zone.id, city.city) { cityCoordinates(city) }
    val night = remember(coordinates, city.zone, nowUtcMillis / 60_000) {
        coordinates?.let { !solarDaylight(nowUtcMillis, it.latitude, it.longitude) } ?: city.isNight(nowUtcMillis)
    }
    val cue = if (night) {
        if (dark) Color(0xFFA8BEDB) else Color(0xFF617DA4)
    } else {
        if (dark) Color(0xFFFFD166) else Color(0xFFF2AD12)
    }
    val shape = RoundedCornerShape(26.dp)
    val selection = animateFloatAsState(if (selected) 1f else 0f, tween(280), label = "citySurfaceSelection")
    val badge = animateFloatAsState(if (showSelectionBadge && selected) 1f else 0f,
        tween(180), label = "cityDeletionBadge")
    val selectionDim by animateFloatAsState(if (selectionMode && !selected) .68f else 1f,
        tween(180), label = "citySelectionDim")
    val selectionLift by animateFloatAsState(if (selectionMode && selected) 1f else 0f,
        spring(dampingRatio = .68f, stiffness = 380f), label = "citySelectionLift")
    // Lighting travels inside the card's existing entrance layer, sharing its opacity.
    // Layer outsets retain the full halo throughout the slide without a second fade.
    val edge = androidx.compose.animation.animateColorAsState(
        if (selected) primary.copy(alpha = if (dark) .48f else .44f)
        else if (dark) scheme.outlineVariant.copy(alpha = .16f) else Color.White.copy(alpha = .85f),
        tween(280), label = "citySurfaceEdge")
    BoxWithConstraints(modifier.height(cardHeight).graphicsLayer {
        outsets = cityLightLayerOutsets(size.width.toDp())
        alpha = selectionDim
        scaleX = 1f + .025f * selectionLift
        scaleY = scaleX
        translationY = -2.dp.toPx() * selectionLift
    }.graphicsLayer {
        this.shape = shape
        clip = false
        shadowElevation = ((if (dark) 3.dp else (10f + selection.value * 4f).dp) + 3.dp * selectionLift).toPx()
        ambientShadowColor = (if (dark) Color.Black else lerp(Color(0xFF65718B), primary, .25f))
            .copy(alpha = (if (dark) .12f else .36f))
        spotShadowColor = (if (dark) Color.Black else primary).copy(alpha = (if (dark) .12f else .30f))
    }) {
        val reserved = if (!use24h) 20f else 0f
        val measurer = rememberTextMeasurer()
        val baseTimeStyle = WorldClockType.cardTime()
        val measuredWidth = measurer.measure(time, baseTimeStyle).size.width.toFloat()
        val availableWidth = with(LocalDensity.current) { (maxWidth - (28f + reserved).dp).toPx() }
        val timeSize = 34.sp * minOf(1f, availableWidth / measuredWidth.coerceAtLeast(1f))

        Box(Modifier.fillMaxSize().clip(shape)
            .worldCitySurface(scheme.surface, primary, dark) { selection.value }
            .border(.8.dp, edge.value, shape).combinedClickable(onClick = onClick,
                onLongClick = onLongClick, onLongClickLabel = "Select location for deletion", hapticFeedbackEnabled = false)
            .semantics {
                this.selected = selected
                if (deleteAction != null) customActions = listOf(deleteAction)
            }) {
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 14.dp, end = 50.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(city.city, modifier = Modifier.fillMaxWidth(), maxLines = 2,
                    overflow = TextOverflow.Ellipsis, style = WorldClockType.cardCity(), color = scheme.onSurface)
                Text(date, style = WorldClockType.cardDate(),
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom) {
                WorldClockCardTime(time, city.zone.id, style = WorldClockType.cardTime(timeSize),
                    color = scheme.onSurface)
                if (!use24h) Text(local.format(DateTimeFormatter.ofPattern(" a", locale)),
                    style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 12.dp).size(32.dp).shadow(if (dark) 2.dp else 5.dp, CircleShape, clip = false,
            ambientColor = cue.copy(alpha = .15f), spotColor = cue.copy(alpha = .22f))
            .background(Brush.radialGradient(listOf(
                lerp(scheme.surface, cue, if (dark) .13f else .06f),
                lerp(scheme.surface, cue, if (dark) .20f else .16f))), CircleShape)
            .border(.6.dp, Color.White.copy(alpha = if (dark) .10f else .85f), CircleShape), Alignment.Center) {
            Icon(if (night) Icons.Outlined.Bedtime else Icons.Outlined.LightMode,
                contentDescription = if (night) "Night" else "Daytime",
                tint = cue, modifier = Modifier.size(21.dp))
        }

        Box(Modifier.align(Alignment.TopStart).offset(x = (-5).dp, y = (-5).dp)
            .size(20.dp).graphicsLayer {
                alpha = badge.value
                scaleX = .75f + .25f * badge.value
                scaleY = scaleX
            }.background(primary, CircleShape).border(1.5.dp, scheme.surface, CircleShape), Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(13.dp))
        }
    }

}

@Composable
internal fun Modifier.progressiveListEdgeMask(topStrength: Float, bottomStrength: Float = 1f, edgeBandsOnly: Boolean = false): Modifier {
    val sharp = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val bottomBlurred = rememberGraphicsLayer()
    return graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    if (topStrength <= 0f && bottomStrength <= 0f) {
        drawContent()
        return@drawWithContent
    }
    // Record once, then crossfade sharp and blurred samples in stationary viewport bands.
    sharp.record { this@drawWithContent.drawContent() }
    val edgeFraction = (32.dp.toPx() / size.height.coerceAtLeast(1f)).coerceAtMost(.25f)
    val clearMask = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 1f - topStrength), edgeFraction to Color.Black,
        (1f - edgeFraction) to Color.Black, 1f to Color.Black.copy(alpha = 1f - bottomStrength),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (edgeBandsOnly) {
            val band = (32.dp.toPx() + 10.dp.toPx()).coerceAtMost(size.height / 2f)
            val bandSize = IntSize(size.width.toInt().coerceAtLeast(1), band.toInt().coerceAtLeast(1))
            val effect = RenderEffect.createBlurEffect(5.dp.toPx(), 5.dp.toPx(), Shader.TileMode.CLAMP).asComposeRenderEffect()
            blurred.record(size = bandSize) { drawLayer(sharp) }
            bottomBlurred.record(size = bandSize) {
                translate(top = -(this@drawWithContent.size.height - band)) { drawLayer(sharp) }
            }
            blurred.renderEffect = effect
            bottomBlurred.renderEffect = effect
            val canvas = drawContext.canvas
            val bounds = Rect(Offset.Zero, size)
            canvas.saveLayer(bounds, Paint())
            drawLayer(sharp)
            drawRect(clearMask, blendMode = BlendMode.DstIn)
            canvas.restore()
            canvas.saveLayer(bounds, Paint())
            drawLayer(blurred)
            translate(top = size.height - band) { drawLayer(bottomBlurred) }
            drawRect(clearMask, blendMode = BlendMode.DstOut)
            canvas.restore()
        } else {
        blurred.record { drawLayer(sharp) }
        blurred.renderEffect = RenderEffect.createBlurEffect(
            5.dp.toPx(), 5.dp.toPx(), Shader.TileMode.CLAMP,
        ).asComposeRenderEffect()
        val canvas = drawContext.canvas
        val bounds = Rect(Offset.Zero, size)
        canvas.saveLayer(bounds, Paint())
        drawLayer(sharp)
        drawRect(clearMask, blendMode = BlendMode.DstIn)
        canvas.restore()
        canvas.saveLayer(bounds, Paint())
        drawLayer(blurred)
        drawRect(clearMask, blendMode = BlendMode.DstOut)
        canvas.restore()
        }
    } else {
        drawLayer(sharp)
    }
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Black.copy(alpha = 1f - topStrength),
                edgeFraction to Color.Black,
                (1f - edgeFraction) to Color.Black,
                1f to Color.Black.copy(alpha = 1f - bottomStrength),
            ),
        ),
        blendMode = BlendMode.DstIn,
    )
}
}


/** Viewport-owned illumination survives lazy-item culling at the edge of the screen. */
private data class CityViewportGlow(
    val index: Int,
    val strength: androidx.compose.runtime.State<Float>,
    val reveal: androidx.compose.runtime.State<Float>,
    val entry: androidx.compose.runtime.State<Float>?,
)
