package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DaylistDatabase
import com.example.data.repository.TaskRepository
import com.example.ui.screens.TodoListScreen
import com.example.ui.theme.DaylistTheme
import com.example.ui.viewmodel.TodoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodoListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: DaylistDatabase
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
        viewModel = TodoViewModel(
            repository = TaskRepository(database.taskDao()),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `clicking fab opens editor and adds task to list`() {
        composeTestRule.setContent {
            DaylistTheme {
                TodoListScreen(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_task_fab").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Buy espresso beans")
        composeTestRule.onNodeWithTag("save_task_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Buy espresso beans").assertIsDisplayed()
    }
}
