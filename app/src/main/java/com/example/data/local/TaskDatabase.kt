package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val completed: Boolean,
    val priority: String,
    val category: String,
    val dueAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): TaskItem = TaskItem(
        id = id,
        title = title,
        notes = notes,
        completed = completed,
        priority = priority,
        category = category,
        dueAtMillis = dueAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed, updatedAtMillis = :updatedAtMillis WHERE id = :taskId")
    suspend fun updateCompletion(taskId: String, completed: Boolean, updatedAtMillis: Long)

    @Query(
        """
        UPDATE tasks
        SET title = :title,
            notes = :notes,
            completed = :completed,
            priority = :priority,
            category = :category,
            dueAtMillis = :dueAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateTask(
        taskId: String,
        title: String,
        notes: String,
        completed: Boolean,
        priority: String,
        category: String,
        dueAtMillis: Long,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteTasksByIds(taskIds: List<String>)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class DaylistDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: DaylistDatabase? = null

        fun getInstance(context: Context): DaylistDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DaylistDatabase::class.java,
                    "daylist.db"
                ).build().also { instance = it }
            }
        }
    }
}
