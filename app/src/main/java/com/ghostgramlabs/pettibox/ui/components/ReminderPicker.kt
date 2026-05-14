package com.ghostgramlabs.pettibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Quick-pick reminder presets that map to wall-clock times relative to
 * "now." Calendar handles DST transitions for us — these all resolve at
 * the moment the user taps, not at app start.
 */
enum class ReminderPreset(val label: String) {
    TONIGHT("Tonight"),
    TOMORROW("Tomorrow"),
    WEEKEND("Weekend"),
    NEXT_WEEK("Next week"),
    CUSTOM("Pick a time…")
}

/**
 * Returns the wall-clock millis for a preset, or null for CUSTOM (caller
 * shows the picker).
 *
 * Heuristics chosen to match user intent rather than literal interpretation:
 *  - Tonight: 9 PM today, or 9 PM tomorrow if it's already past 9 PM
 *  - Tomorrow: 9 AM tomorrow
 *  - Weekend: 10 AM on the next Saturday (or today if it IS Saturday and
 *    before 10 AM)
 *  - Next week: 9 AM 7 days from now
 */
fun ReminderPreset.toEpochMillis(now: Long = System.currentTimeMillis()): Long? {
    if (this == ReminderPreset.CUSTOM) return null
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    when (this) {
        ReminderPreset.TONIGHT -> {
            cal.set(Calendar.HOUR_OF_DAY, 21)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        ReminderPreset.TOMORROW -> {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        ReminderPreset.WEEKEND -> {
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val daysUntilSaturday = ((Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK)) + 7) % 7
            if (daysUntilSaturday == 0 && cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_MONTH, 7)
            } else {
                cal.add(Calendar.DAY_OF_MONTH, daysUntilSaturday)
            }
        }
        ReminderPreset.NEXT_WEEK -> {
            cal.add(Calendar.DAY_OF_MONTH, 7)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        ReminderPreset.CUSTOM -> Unit
    }
    return cal.timeInMillis
}

/**
 * Friendly preview of when a reminder will fire — e.g. "Sat 10:00 AM"
 * or "Tomorrow 9:00 AM" or "Dec 3, 9:00 AM" for things further out.
 */
fun formatReminderAt(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val sameDay = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    val tomorrowCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = cal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
    val withinWeek = (epochMillis - now) in 0..(7L * 24 * 3600 * 1000)
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return when {
        sameDay -> "Today " + timeFmt.format(Date(epochMillis))
        isTomorrow -> "Tomorrow " + timeFmt.format(Date(epochMillis))
        withinWeek -> SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(epochMillis))
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMillis))
    }
}

/**
 * Bottom sheet that offers the five preset reminder times plus a
 * "Custom…" entry. Caller is responsible for showing the custom time
 * picker on CUSTOM if they want fine control — most users land on a
 * preset and never need it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerSheet(
    currentRemindAt: Long?,
    onPick: (Long?) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Remind me",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            if (currentRemindAt != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Currently set for " + formatReminderAt(currentRemindAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            ReminderPreset.entries.forEach { preset ->
                val previewMillis = remember(preset) { preset.toEpochMillis() }
                ReminderRow(
                    label = preset.label,
                    subtitle = previewMillis?.let { formatReminderAt(it) } ?: "Choose a date & time",
                    onClick = {
                        if (preset == ReminderPreset.CUSTOM) onCustom()
                        else onPick(previewMillis)
                    }
                )
            }
            if (currentRemindAt != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onPick(null) }) {
                    Text("Clear reminder", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Icon(
            Icons.Rounded.AccessTime,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bottom sheet for picking an arbitrary date + time as a reminder.
 *
 * Replaces the prior ReminderCustomDialog which crammed a TimePicker
 * on top of a 4-chip "Day" row in an AlertDialog — the day picker was
 * tiny and hidden, and there was no preview of when the reminder would
 * actually fire. The new sheet:
 *
 *  - Shows a live "We'll nudge you Thu, May 14 · 9:00 PM" preview at
 *    the top, in the primary color, that updates as the user changes
 *    either tab.
 *  - Splits Date and Time into proper tabs so each picker gets the
 *    full sheet width instead of fighting for space.
 *  - Uses Material3 [DatePicker] (full calendar grid, any future date)
 *    instead of the four hardcoded day chips.
 *  - Disables the Set button until the combined timestamp is in the
 *    future, so the user can't accidentally schedule a reminder for
 *    five minutes ago.
 *
 * Date/time combination is timezone-correct: DatePicker returns UTC
 * midnight of the picked day, which we read in UTC and then re-construct
 * as a local-time Calendar at the picked hour and minute.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCustomSheet(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    // DatePicker stores its selection in UTC midnight of the user's
    // picked day — convert the LOCAL initial date to a UTC midnight of
    // the same calendar day so the picker opens on "today" wherever the
    // user is, not on yesterday for users east of UTC.
    val initialUtcDay = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtcDay)
    val timeState = rememberTimePickerState(
        initialHour = initial.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial.get(Calendar.MINUTE),
        is24Hour = false
    )
    var tab by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val combined: Long? by remember {
        derivedStateOf {
            val d = dateState.selectedDateMillis ?: return@derivedStateOf null
            // Read the picked day's year/month/day in UTC, then build a
            // local Calendar for the user's timezone at the picked hour.
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = d }
            Calendar.getInstance().apply {
                set(
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH),
                    utc.get(Calendar.DAY_OF_MONTH),
                    timeState.hour,
                    timeState.minute,
                    0
                )
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
    val isFuture by remember {
        derivedStateOf { (combined ?: 0L) > System.currentTimeMillis() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
                .heightIn(max = 720.dp)
        ) {
            Text(
                "Custom reminder",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            // Live preview — updates as either tab changes. Primary
            // color when valid, muted hint when in the past so the user
            // notices before tapping Set.
            Text(
                when {
                    combined == null -> "Pick a date and time"
                    !isFuture -> "That time is already in the past — pick a future moment"
                    else -> "We'll nudge you " + formatReminderAt(combined!!)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isFuture) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            SecondaryTabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Date", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Time", fontWeight = FontWeight.SemiBold) }
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                when (tab) {
                    0 -> DatePicker(
                        state = dateState,
                        modifier = Modifier.fillMaxWidth(),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                    else -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timeState)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isFuture,
                    onClick = { combined?.let(onConfirm) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Set reminder", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
