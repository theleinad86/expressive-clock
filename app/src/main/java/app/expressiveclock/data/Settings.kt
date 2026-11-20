package app.expressiveclock.data

import app.expressiveclock.alarm.Notifications
import app.expressiveclock.ui.theme.Palette
import java.time.DayOfWeek

/**
 * Every preference the app has, in one tree.
 *
 * Grouped by feature, then surfaced from one persistent app-wide settings sheet. This keeps the
 * navigation affordance consistent while leaving room to add more preferences later.
 */
data class ClockSettings(
    val alarms: AlarmSettings = AlarmSettings(),
    val world: WorldClockSettings = WorldClockSettings(),
    val timers: TimerSettings = TimerSettings(),
    val theme: ThemeSettings = ThemeSettings(),
    val defaultStartupPage: DefaultStartupPage = DefaultStartupPage.ALARMS,
    val hasCompletedInitialSetup: Boolean = false,
)

data class AlarmSettings(
    /** How long a ringing alarm keeps ringing before it gives up. */
    /** Minutes a ringing alarm keeps going before it gives up. **0 means never.** */
    val silenceAfterMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val cards24Hour: Boolean = false,
    val weekStart: WeekStart = WeekStart.SYSTEM,
    val shakeEnabled: Boolean = false,
    val shakeAction: ShakeAction = ShakeAction.DISMISS,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    /** Shakes required inside the detection window, [SHAKE_COUNT_RANGE]. */
    val shakeCount: Int = 8,
    /** Calibrated threshold in g; [SHAKE_NOT_CALIBRATED] means the preset applies. */
    val shakeCustomG: Float = SHAKE_NOT_CALIBRATED,
    val fadeIn: Boolean = false,
    /** Seconds, [FADE_IN_DURATION_RANGE] in steps of 5. */
    val fadeInDurationSeconds: Int = 20,
    /** Percent, [START_VOLUME_RANGE]. */
    val startVolumePercent: Int = 0,
) {
    /** The calibrated value wins only when it is strong enough to tell a shake from a walk. */
    val shakeCalibrated: Boolean get() = shakeCustomG > MIN_CALIBRATED_G
    val shakeThresholdG: Float get() = if (shakeCalibrated) shakeCustomG else shakeSensitivity.g
}

enum class ShakeAction(val label: String) { DISMISS("Dismiss"), SNOOZE("Snooze") }

enum class ShakeSensitivity(val label: String, val g: Float) {
    LOW("Light", 1.5f), MEDIUM("Medium", 2.7f), HIGH("Strong", 4.0f)
}

const val SHAKE_NOT_CALIBRATED = -1f
const val MIN_CALIBRATED_G = 1.3f
val SHAKE_COUNT_RANGE = 3..15
val FADE_IN_DURATION_RANGE = 5..60
val START_VOLUME_RANGE = 0..50

data class WorldClockSettings(
    val deleteTutorialShown: Boolean = false,
    val showSeconds: Boolean = false,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
)

data class TimerSettings(
    /** A `content://` ringtone URI, or null for the system default alarm sound. */
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    /** Presets are stored as total seconds, in the order shown on the timer page. */
    val presets: List<Int> = DEFAULT_TIMER_PRESETS,
)

val DEFAULT_TIMER_PRESETS: List<Int> = listOf(60, 120, 300, 600)
const val SILENT = "silent"

fun formatPresetLabel(seconds: Int): String = when {
    seconds >= 3600 && seconds % 3600 == 0 -> "${seconds / 3600}h"
    seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60} min"
    seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds}s"
}

const val FACTORY_OVERLAY_OFFSET_X = 0
const val FACTORY_OVERLAY_OFFSET_Y = 9
const val FACTORY_OVERLAY_WIDTH_OFFSET = -43
const val FACTORY_OVERLAY_HEIGHT_OFFSET = 1

data class ThemeSettings(
    /**
     * Wallpaper colour is the default, per the platform's own guidance: a clock is a background
     * app and should look like it belongs to the phone rather than to itself. [palette] is what it
     * falls back to when dynamic colour is off, or unavailable below API 31.
     */
    val dynamicColor: Boolean = true,
    val palette: Palette = Palette.CONCEPT,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val dynamicIsland: Boolean = false,
    val overlayProfiles: Map<IslandProfile, IslandCalibration> = emptyMap(),
    val overlayOffsetX: Int = FACTORY_OVERLAY_OFFSET_X,
    val overlayOffsetY: Int = FACTORY_OVERLAY_OFFSET_Y,
    val overlayWidthOffset: Int = FACTORY_OVERLAY_WIDTH_OFFSET,
    val overlayHeightOffset: Int = FACTORY_OVERLAY_HEIGHT_OFFSET,
)

enum class DarkMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class HourFormat(val label: String) { SYSTEM("System default"), H12("12-hour"), H24("24-hour") }

enum class WeekStart(val label: String, val day: DayOfWeek?) {
    SYSTEM("System default", null),
    SATURDAY("Saturday", DayOfWeek.SATURDAY),
    SUNDAY("Sunday", DayOfWeek.SUNDAY),
    MONDAY("Monday", DayOfWeek.MONDAY),
}

enum class DefaultStartupPage(val label: String, val tabKey: String) {
    ALARMS("Alarms", Notifications.TAB_ALARMS),
    WORLD("World Clock", "world"),
    TIMER("Timer", Notifications.TAB_TIMERS),
    STOPWATCH("Stopwatch", Notifications.TAB_STOPWATCH);

    companion object {
        fun fromName(name: String?): DefaultStartupPage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALARMS
        }
    }
}

/**
 * The seven weekday letters, rotated to start on [start].
 *
 * The alarm tile draws these, so the setting has to reach all the way into the grid rather than
 * only into a picker. An app that starts the week on Monday in the editor and on Sunday in the
 * tile is worse than one that never offered the choice.
 */
fun WeekStart.order(systemFirstDay: DayOfWeek): List<DayOfWeek> {
    val first = day ?: systemFirstDay
    return (0..6).map { DayOfWeek.of((first.value - 1 + it) % 7 + 1) }
}
