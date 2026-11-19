package com.example.data.repository

import android.content.Context
import com.example.data.local.DaylistDatabase
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao
) {
    constructor(context: Context) : this(
        DaylistDatabase.getInstance(context.applicationContext).taskDao()
    )

    fun observeTasks(): Flow<List<TaskItem>> =
        taskDao.observeTasks().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getTaskById(taskId: String): TaskItem? =
        taskDao.getTaskById(taskId)?.toDomain()

    suspend fun createTask(
        title: String,
        notes: String,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long
    ): Result<String> {
        val cleanTitle = title.trim()
        val cleanNotes = notes.trim()
        return try {
            require(cleanTitle.length in 1..MAX_TITLE_LENGTH) {
                "Task title must be 1 to $MAX_TITLE_LENGTH characters."
            }
            require(cleanNotes.length <= MAX_NOTES_LENGTH) {
                "Notes must be $MAX_NOTES_LENGTH characters or fewer."
            }
            require(dueAtMillis >= 0L) { "Due date must be non-negative." }

            val now = System.currentTimeMillis()
            val taskId = UUID.randomUUID().toString()
            taskDao.insertTask(
                TaskEntity(
                    id = taskId,
                    title = cleanTitle,
                    notes = cleanNotes,
                    completed = false,
                    priority = priority.name,
                    category = category.name,
                    dueAtMillis = dueAtMillis,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            Result.success(taskId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(
        taskId: String,
        completed: Boolean
    ): Result<Unit> {
        return try {
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            taskDao.updateCompletion(
                taskId = taskId,
                completed = completed,
                updatedAtMillis = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        taskId: String,
        title: String,
        notes: String,
        completed: Boolean,
        priority: TaskPriority,
        category: TaskCategory,
        dueAtMillis: Long
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanNotes = notes.trim()
        return try {
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            require(cleanTitle.length in 1..MAX_TITLE_LENGTH) {
                "Task title must be 1 to $MAX_TITLE_LENGTH characters."
            }
            require(cleanNotes.length <= MAX_NOTES_LENGTH) {
                "Notes must be $MAX_NOTES_LENGTH characters or fewer."
            }
            require(dueAtMillis >= 0L) { "Due date must be non-negative." }

            taskDao.updateTask(
                taskId = taskId,
                title = cleanTitle,
                notes = cleanNotes,
                completed = completed,
                priority = priority.name,
                category = category.name,
                dueAtMillis = dueAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            taskDao.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCompletedTasks(taskIds: List<String>): Result<Unit> {
        return try {
            val validIds = taskIds.filter { it.isNotBlank() }
            if (validIds.isNotEmpty()) {
                validIds.chunked(MAX_BATCH_DELETES).forEach { chunk ->
                    taskDao.deleteTasksByIds(chunk)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 120
        const val MAX_NOTES_LENGTH = 500
        private const val MAX_BATCH_DELETES = 450
    }
}
