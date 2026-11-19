package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.TaskCategory
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskItem
import com.example.data.model.TaskStats
import com.example.ui.components.DailyProgressCard
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditorSheet
import com.example.ui.components.labelRes
import com.example.ui.viewmodel.TodoViewModel
import com.example.ui.viewmodel.UiState

/**
 * Standalone Jetpack Compose screen displaying the task list and a Floating Action Button
 * to create and edit tasks, integrated directly with [TodoViewModel].
 */
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val taskStats by viewModel.taskStats.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    val deletedMsg = stringResource(R.string.snackbar_task_deleted)
    val clearedMsg = stringResource(R.string.snackbar_completed_cleared)

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage.orEmpty())
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTask = null
                    isEditorOpen = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_task)) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        TodoListContent(
            tasksState = tasksState,
            filteredTasks = filteredTasks,
            taskStats = taskStats,
            selectedFilter = selectedFilter,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            showSearch = false,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSelectFilter = viewModel::selectFilter,
            onSelectCategory = viewModel::selectCategory,
            onToggleTask = viewModel::toggleTaskCompletion,
            onEditTask = { task ->
                editingTask = task
                isEditorOpen = true
            },
            onDeleteTask = { taskId -> viewModel.deleteTask(taskId, deletedMsg) },
            onClearCompleted = { viewModel.clearCompletedTasks(clearedMsg) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (isEditorOpen) {
        val target = editingTask
        TaskEditorSheet(
            editingTask = target,
            isSaving = isSaving,
            onDismiss = { isEditorOpen = false },
            onSave = { title, notes, priority, category, dueAtMillis ->
                if (target == null) {
                    viewModel.addTask(title, notes, priority, category, dueAtMillis) {
                        isEditorOpen = false
                    }
                } else {
                    viewModel.updateTask(
                        taskId = target.id,
                        title = title,
                        notes = notes,
                        completed = target.completed,
                        priority = priority,
                        category = category,
                        dueAtMillis = dueAtMillis,
                        onSuccess = { isEditorOpen = false }
                    )
                }
            },
            onDelete = if (target != null) {
                {
                    viewModel.deleteTask(target.id, deletedMsg)
                    isEditorOpen = false
                }
            } else null
        )
    }
}

@Composable
fun TodoListContent(
    tasksState: UiState<List<TaskItem>>,
    filteredTasks: List<TaskItem>,
    taskStats: TaskStats,
    selectedFilter: TaskFilterTab,
    selectedCategory: TaskCategory?,
    searchQuery: String,
    showSearch: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSelectCategory: (TaskCategory?) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_clear_search)
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("search_tasks_input")
            )
        }

        FilterBars(
            selectedFilter = selectedFilter,
            selectedCategory = selectedCategory,
            onSelectFilter = onSelectFilter,
            onSelectCategory = onSelectCategory
        )

        when (tasksState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tasksState.message.ifBlank {
                                stringResource(R.string.error_load_tasks)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val totalTasks = tasksState.data.size
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("todo_task_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "progress_banner") {
                        DailyProgressCard(
                            stats = taskStats,
                            onClearCompleted = onClearCompleted
                        )
                    }

                    if (filteredTasks.isEmpty()) {
                        item(key = "empty_state") {
                            EmptyTasksCard(
                                hasAnyTasks = totalTasks > 0,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(
                            items = filteredTasks,
                            key = { it.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { onToggleTask(task) },
                                onEditClick = { onEditTask(task) },
                                onDeleteClick = { onDeleteTask(task.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBars(
    selectedFilter: TaskFilterTab,
    selectedCategory: TaskCategory?,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSelectCategory: (TaskCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilterTab.entries.forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(stringResource(filterLabelRes(filter))) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_tab_${filter.name.lowercase()}")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text(stringResource(R.string.category_all)) },
                modifier = Modifier.testTag("category_filter_all")
            )
            TaskCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onSelectCategory(if (selectedCategory == category) null else category)
                    },
                    label = { Text(stringResource(category.labelRes())) },
                    modifier = Modifier.testTag("category_filter_${category.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksCard(
    hasAnyTasks: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasAnyTasks) {
                Image(
                    painter = painterResource(id = R.drawable.img_tasks_empty),
                    contentDescription = stringResource(R.string.cd_empty_illustration),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = stringResource(
                    if (hasAnyTasks) R.string.empty_filtered_title else R.string.empty_tasks_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    if (hasAnyTasks) R.string.empty_filtered_subtitle else R.string.empty_tasks_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun filterLabelRes(filter: TaskFilterTab): Int = when (filter) {
    TaskFilterTab.ALL -> R.string.filter_all
    TaskFilterTab.TODAY -> R.string.filter_today
    TaskFilterTab.UPCOMING -> R.string.filter_upcoming
    TaskFilterTab.COMPLETED -> R.string.filter_completed
}
