package com.example.ui.components

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStats
import com.example.data.repository.TaskRepository
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@StringRes
fun TaskPriority.labelRes(): Int = when (this) {
    TaskPriority.LOW -> R.string.priority_low
    TaskPriority.MEDIUM -> R.string.priority_medium
    TaskPriority.HIGH -> R.string.priority_high
}

@StringRes
fun TaskCategory.labelRes(): Int = when (this) {
    TaskCategory.PERSONAL -> R.string.category_personal
    TaskCategory.WORK -> R.string.category_work
    TaskCategory.ERRANDS -> R.string.category_errands
    TaskCategory.HEALTH -> R.string.category_health
    TaskCategory.GENERAL -> R.string.category_general
}

fun TaskPriority.accentColor() = when (this) {
    TaskPriority.HIGH -> PriorityHighColor
    TaskPriority.MEDIUM -> PriorityMediumColor
    TaskPriority.LOW -> PriorityLowColor
}

@Composable
fun DailyProgressCard(
    stats: TaskStats,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.completionFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "daily_progress"
    )
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (stats.totalCount > 0 && stats.completedCount == stats.totalCount) {
                            stringResource(R.string.progress_all_done)
                        } else {
                            stringResource(R.string.progress_summary, stats.completedCount, stats.totalCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                if (stats.completedCount > 0) {
                    TextButton(
                        onClick = onClearCompleted,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_completed_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_clear_completed),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priority = task.priorityEnum()
    val category = task.categoryEnum()
    val priorityColor = priority.accentColor()

    val checkBgColor by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "check_bg"
    )
    val checkBorderColor by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.primary else priorityColor,
        label = "check_border"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.completed) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale"
    )

    val nowMillis = System.currentTimeMillis()
    val startOfToday = remember(nowMillis / DateUtils.DAY_IN_MILLIS) { startOfLocalDayMillis(nowMillis) }
    val isOverdue = !task.completed && task.dueAtMillis in 1L until startOfToday
    val dueLabel = remember(task.dueAtMillis) { formatDueLabel(task.dueAtMillis) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEditClick)
            .testTag("task_card_${task.id}"),
        shape = MaterialTheme.shapes.medium,
        color = if (task.completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverdue) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = if (task.completed) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 48dp touch target circular checkbox
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleComplete)
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(checkBgColor)
                        .border(2.dp, checkBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.cd_toggle_complete),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, bottom = 2.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(category.labelRes()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Priority indicator pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(priority.labelRes()),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = priorityColor
                            )
                        }
                    }

                    // Due date badge
                    if (dueLabel != null) {
                        val badgeBg = if (isOverdue) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        }
                        val badgeFg = if (isOverdue) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isOverdue) {
                                        stringResource(R.string.due_overdue_prefix, dueLabel)
                                    } else {
                                        dueLabel
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeFg
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_task),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    editingTask: TaskItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
    var notes by remember(editingTask) { mutableStateOf(editingTask?.notes ?: "") }
    var priority by remember(editingTask) {
        mutableStateOf(editingTask?.priorityEnum() ?: TaskPriority.MEDIUM)
    }
    var category by remember(editingTask) {
        mutableStateOf(editingTask?.categoryEnum() ?: TaskCategory.PERSONAL)
    }
    var dueAtMillis by remember(editingTask) { mutableLongStateOf(editingTask?.dueAtMillis ?: 0L) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isTitleValid = title.trim().isNotEmpty() && title.trim().length <= TaskRepository.MAX_TITLE_LENGTH
    val isNotesValid = notes.trim().length <= TaskRepository.MAX_NOTES_LENGTH

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (editingTask == null) R.string.editor_new_title else R.string.editor_edit_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (editingTask != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("sheet_delete_task_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= TaskRepository.MAX_TITLE_LENGTH) title = it },
                label = { Text(stringResource(R.string.editor_title_label)) },
                placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
                singleLine = true,
                supportingText = {
                    Text("${title.trim().length}/${TaskRepository.MAX_TITLE_LENGTH}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = MaterialTheme.shapes.small
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= TaskRepository.MAX_NOTES_LENGTH) notes = it },
                label = { Text(stringResource(R.string.editor_notes_label)) },
                placeholder = { Text(stringResource(R.string.editor_notes_placeholder)) },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text("${notes.trim().length}/${TaskRepository.MAX_NOTES_LENGTH}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input"),
                shape = MaterialTheme.shapes.small
            )

            // Priority Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.editor_priority_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { option ->
                        val selected = priority == option
                        FilterChip(
                            selected = selected,
                            onClick = { priority = option },
                            label = { Text(stringResource(option.labelRes())) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = option.accentColor().copy(alpha = 0.16f),
                                selectedLabelColor = option.accentColor(),
                                selectedLeadingIconColor = option.accentColor()
                            ),
                            modifier = Modifier.testTag("priority_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.editor_category_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { option ->
                        val selected = category == option
                        FilterChip(
                            selected = selected,
                            onClick = { category = option },
                            label = { Text(stringResource(option.labelRes())) },
                            modifier = Modifier.testTag("category_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            // Due Date Quick Picks
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.editor_due_date_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val todayEnd = remember { endOfDayAfterDays(0) }
                val tomorrowEnd = remember { endOfDayAfterDays(1) }
                val nextWeekEnd = remember { endOfDayAfterDays(7) }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dueAtMillis == 0L,
                        onClick = { dueAtMillis = 0L },
                        label = { Text(stringResource(R.string.due_none)) },
                        modifier = Modifier.testTag("due_none_chip")
                    )
                    FilterChip(
                        selected = dueAtMillis == todayEnd,
                        onClick = { dueAtMillis = todayEnd },
                        label = { Text(stringResource(R.string.due_today)) },
                        modifier = Modifier.testTag("due_today_chip")
                    )
                    FilterChip(
                        selected = dueAtMillis == tomorrowEnd,
                        onClick = { dueAtMillis = tomorrowEnd },
                        label = { Text(stringResource(R.string.due_tomorrow)) },
                        modifier = Modifier.testTag("due_tomorrow_chip")
                    )
                    FilterChip(
                        selected = dueAtMillis == nextWeekEnd,
                        onClick = { dueAtMillis = nextWeekEnd },
                        label = { Text(stringResource(R.string.due_next_week)) },
                        modifier = Modifier.testTag("due_week_chip")
                    )
                    val customSelected = dueAtMillis > 0L &&
                        dueAtMillis != todayEnd &&
                        dueAtMillis != tomorrowEnd &&
                        dueAtMillis != nextWeekEnd
                    FilterChip(
                        selected = customSelected,
                        onClick = { showDatePicker = true },
                        label = {
                            Text(
                                if (customSelected) {
                                    formatDueLabel(dueAtMillis) ?: stringResource(R.string.due_pick_date)
                                } else {
                                    stringResource(R.string.due_pick_date)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("due_pick_date_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_task_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        onSave(title.trim(), notes.trim(), priority, category, dueAtMillis)
                    },
                    enabled = !isSaving && isTitleValid && isNotesValid,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_task_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        stringResource(
                            if (editingTask == null) R.string.action_save else R.string.action_update
                        )
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (dueAtMillis > 0L) dueAtMillis else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            dueAtMillis = convertUtcPickerMillisToLocalEndOfDay(selectedUtcMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun startOfLocalDayMillis(timeMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun endOfDayAfterDays(daysOffset: Int): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysOffset)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

private fun convertUtcPickerMillisToLocalEndOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return localCal.timeInMillis
}

fun formatDueLabel(dueAtMillis: Long): String? {
    if (dueAtMillis <= 0L) return null
    val todayStart = startOfLocalDayMillis(System.currentTimeMillis())
    val tomorrowStart = todayStart + DateUtils.DAY_IN_MILLIS
    val dayAfterTomorrowStart = tomorrowStart + DateUtils.DAY_IN_MILLIS
    return when {
        dueAtMillis in todayStart until tomorrowStart -> "Today"
        dueAtMillis in tomorrowStart until dayAfterTomorrowStart -> "Tomorrow"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueAtMillis))
    }
}
