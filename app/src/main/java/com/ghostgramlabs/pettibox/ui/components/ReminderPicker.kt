package com.ghostgramlabs.pettibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
 * Modal dialog for picking a date and time. Used when the user taps
 * "Pick a time…" in [ReminderPickerSheet].
 *
 * Implementation note: we use Material3 TimePicker for the time and
 * advance the calendar by N days via a small numeric row — full
 * DatePickerDialog would be more correct but adds complexity for
 * marginal gain. Most "custom" reminders are within the next 2 weeks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCustomDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
    }
    val timeState = rememberTimePickerState(
        initialHour = initial.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial.get(Calendar.MINUTE),
        is24Hour = false
    )
    var daysFromNow by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a time") },
        text = {
            Column {
                TimePicker(state = timeState)
                Spacer(Modifier.height(12.dp))
                Text("Day", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf(0 to "Today", 1 to "Tomorrow", 2 to "+2 days", 7 to "+1 week")
                    options.forEach { (offset, label) ->
                        val selected = daysFromNow == offset
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { daysFromNow = offset }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, daysFromNow)
                    set(Calendar.HOUR_OF_DAY, timeState.hour)
                    set(Calendar.MINUTE, timeState.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onConfirm(cal.timeInMillis)
            }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
