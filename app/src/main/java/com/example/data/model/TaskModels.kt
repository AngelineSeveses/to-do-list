package com.example.data.model

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TaskCategory {
    PERSONAL,
    WORK,
    ERRANDS,
    HEALTH,
    GENERAL;

    companion object {
        fun fromRaw(raw: String?): TaskCategory =
            entries.firstOrNull { it.name == raw } ?: PERSONAL
    }
}

enum class TaskFilterTab {
    ALL,
    TODAY,
    UPCOMING,
    COMPLETED
}

enum class AppTab {
    TASKS,
    INSIGHTS
}

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = TaskCategory.PERSONAL.name,
    val dueAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun priorityEnum(): TaskPriority = TaskPriority.fromRaw(priority)

    fun categoryEnum(): TaskCategory = TaskCategory.fromRaw(category)
}

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val completionFraction: Float = 0f,
    val activeByPriority: Map<TaskPriority, Int> = emptyMap(),
    val countByCategory: Map<TaskCategory, Int> = emptyMap()
)
