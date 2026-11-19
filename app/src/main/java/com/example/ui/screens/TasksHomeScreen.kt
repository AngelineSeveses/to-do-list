package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.DaylistDatabase
import com.example.data.model.AppTab
import com.example.data.model.TaskCategory
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStats
import com.example.data.repository.TaskRepository
import com.example.ui.components.TaskEditorSheet
import com.example.ui.viewmodel.TodoViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksHomeRoute(
    modifier: Modifier = Modifier
) {
    val viewModel: TodoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dao = DaylistDatabase.getInstance(app).taskDao()
                TodoViewModel(TaskRepository(dao))
            }
        }
    )

    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val taskStats by viewModel.taskStats.collectAsStateWithLifecycle()
    val selectedAppTab by viewModel.selectedAppTab.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    TasksHomeScreen(
        tasksState = tasksState,
        filteredTasks = filteredTasks,
        taskStats = taskStats,
        selectedAppTab = selectedAppTab,
        selectedFilter = selectedFilter,
        selectedCategory = selectedCategory,
        searchQuery = searchQuery,
        isSaving = isSaving,
        actionMessage = actionMessage,
        onSelectAppTab = viewModel::selectAppTab,
        onSelectFilter = viewModel::selectFilter,
        onSelectCategory = viewModel::selectCategory,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onClearActionMessage = viewModel::clearActionMessage,
        onToggleTask = viewModel::toggleTaskCompletion,
        onCreateTask = { title, notes, priority, category, dueAtMillis, onDone ->
            viewModel.addTask(title, notes, priority, category, dueAtMillis) { onDone() }
        },
        onUpdateTask = { task, title, notes, priority, category, dueAtMillis, onDone ->
            viewModel.updateTask(
                taskId = task.id,
                title = title,
                notes = notes,
                completed = task.completed,
                priority = priority,
                category = category,
                dueAtMillis = dueAtMillis,
                onSuccess = onDone
            )
        },
        onDeleteTask = viewModel::deleteTask,
        onClearCompleted = viewModel::clearCompletedTasks,
        modifier = modifier
    )
}

@Composable
fun TasksHomeScreen(
    tasksState: UiState<List<TaskItem>>,
    filteredTasks: List<TaskItem>,
    taskStats: TaskStats,
    selectedAppTab: AppTab,
    selectedFilter: TaskFilterTab,
    selectedCategory: TaskCategory?,
    searchQuery: String,
    isSaving: Boolean,
    actionMessage: String?,
    onSelectAppTab: (AppTab) -> Unit,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSelectCategory: (TaskCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearActionMessage: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onCreateTask: (
        title: String,
        notes: String,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long,
        onDone: () -> Unit
    ) -> Unit,
    onUpdateTask: (
        task: TaskItem,
        title: String,
        notes: String,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteTask: (String, String?) -> Unit,
    onClearCompleted: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearch by remember { mutableStateOf(false) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    val deletedMsg = stringResource(R.string.snackbar_task_deleted)
    val clearedMsg = stringResource(R.string.snackbar_completed_cleared)

    BackHandler(enabled = selectedAppTab != AppTab.TASKS) {
        onSelectAppTab(AppTab.TASKS)
    }

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearActionMessage()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (selectedAppTab == AppTab.TASKS) {
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
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedAppTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectAppTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(tabLabelRes(tab))) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppTab.entries.forEach { tab ->
                            val selected = selectedAppTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectAppTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(tabLabelRes(tab))) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize()
                    ) {
                        HomeHeader(
                            showSearch = showSearch,
                            showSearchToggle = selectedAppTab == AppTab.TASKS,
                            onToggleSearch = {
                                showSearch = !showSearch
                                if (!showSearch) onSearchQueryChange("")
                            },
                            useNavRail = useNavRail
                        )

                        if (selectedAppTab == AppTab.TASKS) {
                            TodoListContent(
                                tasksState = tasksState,
                                filteredTasks = filteredTasks,
                                taskStats = taskStats,
                                selectedFilter = selectedFilter,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                showSearch = showSearch,
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectFilter = onSelectFilter,
                                onSelectCategory = onSelectCategory,
                                onToggleTask = onToggleTask,
                                onEditTask = { task ->
                                    editingTask = task
                                    isEditorOpen = true
                                },
                                onDeleteTask = { taskId -> onDeleteTask(taskId, deletedMsg) },
                                onClearCompleted = { onClearCompleted(clearedMsg) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            InsightsScreen(
                                stats = taskStats,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditorOpen) {
        val target = editingTask
        TaskEditorSheet(
            editingTask = target,
            isSaving = isSaving,
            onDismiss = { isEditorOpen = false },
            onSave = { title, notes, priority, category, dueAtMillis ->
                if (target == null) {
                    onCreateTask(title, notes, priority, category, dueAtMillis) {
                        isEditorOpen = false
                    }
                } else {
                    onUpdateTask(target, title, notes, priority, category, dueAtMillis) {
                        isEditorOpen = false
                    }
                }
            },
            onDelete = if (target != null) {
                {
                    onDeleteTask(target.id, deletedMsg)
                    isEditorOpen = false
                }
            } else null
        )
    }
}

@Composable
private fun HomeHeader(
    showSearch: Boolean,
    showSearchToggle: Boolean,
    onToggleSearch: () -> Unit,
    useNavRail: Boolean
) {
    val todayLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (useNavRail) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todayLabel.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showSearchToggle) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_search_button")
            ) {
                Icon(
                    imageVector = if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun tabLabelRes(tab: AppTab): Int = when (tab) {
    AppTab.TASKS -> R.string.nav_tasks
    AppTab.INSIGHTS -> R.string.nav_insights
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.TASKS -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
    AppTab.INSIGHTS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
}
