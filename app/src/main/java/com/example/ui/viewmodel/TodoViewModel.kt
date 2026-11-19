package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.TaskDao
import com.example.data.model.AppTab
import com.example.data.model.TaskCategory
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStats
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class TodoViewModel(
    private val repository: TaskRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _selectedAppTab = MutableStateFlow(AppTab.TASKS)
    val selectedAppTab: StateFlow<AppTab> = _selectedAppTab.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilterTab.ALL)
    val selectedFilter: StateFlow<TaskFilterTab> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Read (Reactive Stream from Room via Kotlin Flow)
    val tasksState: StateFlow<UiState<List<TaskItem>>> = repository.observeTasks()
        .map<List<TaskItem>, UiState<List<TaskItem>>> { tasks ->
            val sorted = tasks.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenByDescending { it.priorityEnum().ordinal }
                    .thenBy { if (it.dueAtMillis > 0L) it.dueAtMillis else Long.MAX_VALUE }
                    .thenByDescending { it.createdAtMillis }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing tasks", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        tasksState,
        _selectedFilter,
        _selectedCategory,
        _searchQuery
    ) { state, filter, category, query ->
        val allTasks = (state as? UiState.Success)?.data ?: emptyList()
        val nowMillis = System.currentTimeMillis()
        val endOfTodayMillis = endOfDayMillis(nowMillis)
        val trimmedQuery = query.trim().lowercase()

        allTasks.filter { task ->
            val matchesFilter = when (filter) {
                TaskFilterTab.ALL -> true
                TaskFilterTab.TODAY -> !task.completed && task.dueAtMillis in 1L..endOfTodayMillis
                TaskFilterTab.UPCOMING -> !task.completed && task.dueAtMillis > endOfTodayMillis
                TaskFilterTab.COMPLETED -> task.completed
            }
            val matchesCategory = category == null || task.categoryEnum() == category
            val matchesSearch = trimmedQuery.isEmpty() ||
                task.title.lowercase().contains(trimmedQuery) ||
                task.notes.lowercase().contains(trimmedQuery)
            matchesFilter && matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val taskStats: StateFlow<TaskStats> = tasksState.map { state ->
        val tasks = (state as? UiState.Success)?.data ?: emptyList()
        val nowMillis = System.currentTimeMillis()
        val startToday = startOfDayMillis(nowMillis)
        val endToday = endOfDayMillis(nowMillis)

        val total = tasks.size
        val completed = tasks.count { it.completed }
        val active = total - completed
        val overdue = tasks.count { !it.completed && it.dueAtMillis in 1L until startToday }
        val dueToday = tasks.count { !it.completed && it.dueAtMillis in startToday..endToday }
        val fraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f

        val byPriority = TaskPriority.entries.associateWith { p ->
            tasks.count { !it.completed && it.priorityEnum() == p }
        }
        val byCategory = TaskCategory.entries.associateWith { c ->
            tasks.count { it.categoryEnum() == c }
        }

        TaskStats(
            totalCount = total,
            completedCount = completed,
            activeCount = active,
            overdueCount = overdue,
            dueTodayCount = dueToday,
            completionFraction = fraction,
            activeByPriority = byPriority,
            countByCategory = byCategory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TaskStats()
    )

    fun selectAppTab(tab: AppTab) {
        _selectedAppTab.value = tab
    }

    fun selectFilter(filter: TaskFilterTab) {
        _selectedFilter.value = filter
    }

    fun selectCategory(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    // Create
    fun addTask(
        title: String,
        notes: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        category: TaskCategory = TaskCategory.PERSONAL,
        dueAtMillis: Long = 0L,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = withContext(ioDispatcher) {
                repository.createTask(
                    title = title,
                    notes = notes,
                    priority = priority,
                    category = category,
                    dueAtMillis = dueAtMillis
                )
            }
            _isSaving.value = false
            result.onSuccess { taskId ->
                onSuccess(taskId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save task."
            }
        }
    }

    // Read (One-shot lookup)
    suspend fun getTaskById(taskId: String): TaskItem? =
        withContext(ioDispatcher) { repository.getTaskById(taskId) }

    // Update (Full task details)
    fun updateTask(
        taskId: String,
        title: String,
        notes: String,
        completed: Boolean,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = withContext(ioDispatcher) {
                repository.updateTask(
                    taskId = taskId,
                    title = title,
                    notes = notes,
                    completed = completed,
                    priority = priority,
                    category = category,
                    dueAtMillis = dueAtMillis
                )
            }
            _isSaving.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update task."
            }
        }
    }

    // Update (Toggle completion state)
    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                repository.toggleTaskCompletion(
                    taskId = task.id,
                    completed = !task.completed
                )
            }
            result.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not update task."
            }
        }
    }

    // Delete (Single task)
    fun deleteTask(taskId: String, feedbackMessage: String? = null) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                repository.deleteTask(taskId = taskId)
            }
            result.onSuccess {
                if (!feedbackMessage.isNullOrBlank()) {
                    _actionMessage.value = feedbackMessage
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not delete task."
            }
        }
    }

    // Delete (Batch clear completed)
    fun clearCompletedTasks(feedbackMessage: String? = null) {
        val currentTasks = (tasksState.value as? UiState.Success)?.data ?: return
        val completedIds = currentTasks.filter { it.completed }.map { it.id }
        if (completedIds.isEmpty()) return

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                repository.clearCompletedTasks(completedIds)
            }
            result.onSuccess {
                if (!feedbackMessage.isNullOrBlank()) {
                    _actionMessage.value = feedbackMessage
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not clear completed tasks."
            }
        }
    }

    companion object {
        private const val TAG = "TodoViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun provideFactory(taskDao: TaskDao): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TodoViewModel(TaskRepository(taskDao))
            }
        }

        private fun startOfDayMillis(timeMillis: Long): Long {
            val cal = Calendar.getInstance().apply {
                this.timeInMillis = timeMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        private fun endOfDayMillis(timeMillis: Long): Long {
            val cal = Calendar.getInstance().apply {
                this.timeInMillis = timeMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }
    }
}
