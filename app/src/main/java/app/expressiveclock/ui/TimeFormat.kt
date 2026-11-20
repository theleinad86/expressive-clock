package app.expressiveclock.ui

import androidx.compose.runtime.Composable

/**
 * The app always shows 24-hour time (HH:mm), regardless of the system 12/24-hour preference.
 * Kept as a function so every existing caller (ring screen, alarm list, pickers, world clock)
 * follows the same rule from one place.
 */
@Composable
internal fun rememberSystem24HourFormat(): Boolean = true
