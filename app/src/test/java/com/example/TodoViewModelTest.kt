package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DaylistDatabase
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import com.example.ui.viewmodel.TodoViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodoViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: DaylistDatabase
    private lateinit var repository: TaskRepository
    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directExecutor = Executor { runnable -> runnable.run() }
        database = Room.inMemoryDatabaseBuilder(context, DaylistDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        repository = TaskRepository(database.taskDao())
        viewModel = TodoViewModel(repository = repository, ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `crud operations persist and emit through TodoViewModel`() = runTest(testDispatcher) {
        val collectorJob = backgroundScope.launch(testDispatcher) {
            viewModel.tasksState.collect {}
        }

        // Create
        var createdId = ""
        viewModel.addTask(
            title = "Write unit tests",
            notes = "Cover create, read, update, delete",
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
            dueAtMillis = 0L,
            onSuccess = { id -> createdId = id }
        )
        advanceUntilIdle()
        assertTrue("Expected createdId to be non-blank", createdId.isNotBlank())

        // Read
        val loadedTask = viewModel.getTaskById(createdId)
        assertNotNull(loadedTask)
        assertEquals("Write unit tests", loadedTask?.title)
        assertFalse(loadedTask?.completed ?: true)

        val stateAfterCreate = viewModel.tasksState.first { it is UiState.Success && it.data.isNotEmpty() }
        assertEquals(1, (stateAfterCreate as UiState.Success).data.size)

        // Update (toggle completion + full edit)
        viewModel.toggleTaskCompletion(loadedTask!!)
        advanceUntilIdle()
        val toggled = viewModel.getTaskById(createdId)
        assertTrue(toggled?.completed ?: false)

        viewModel.updateTask(
            taskId = createdId,
            title = "Write comprehensive Room tests",
            notes = "Verified CRUD",
            completed = false,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.PERSONAL,
            dueAtMillis = 123456L
        )
        advanceUntilIdle()
        val updated = viewModel.getTaskById(createdId)
        assertEquals("Write comprehensive Room tests", updated?.title)
        assertEquals(TaskPriority.MEDIUM, updated?.priorityEnum())
        assertEquals(TaskCategory.PERSONAL, updated?.categoryEnum())
        assertFalse(updated?.completed ?: true)

        // Delete
        viewModel.deleteTask(createdId)
        advanceUntilIdle()
        val deleted = viewModel.getTaskById(createdId)
        assertNull(deleted)

        collectorJob.cancel()
    }
}
