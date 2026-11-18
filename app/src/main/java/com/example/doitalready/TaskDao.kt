package com.example.doitalready.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Hasil hitung jumlah tugas per status (untuk ringkasan di dashboard).
data class StatusCount(
    val status: TaskStatus,
    val total: Int
)

@Dao
interface TaskDao {

    // Create / Update / Delete

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long): Int

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: TaskStatus): Int

    // Read

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTaskById(taskId: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    // Filter

    // Filter gabungan. Parameter yang null / kosong dianggap "tidak difilter".
    // - [status] / [priority]: nama enum (mis. "TODO", "HIGH") atau null.
    // - [query]: cari di judul, nama mata kuliah, dan tags. Kosong = semua.
    @Query(
        """
        SELECT * FROM tasks
        WHERE (:status IS NULL OR status = :status)
          AND (:priority IS NULL OR priority = :priority)
          AND (
                :query = ''
                OR title LIKE '%' || :query || '%'
                OR courseName LIKE '%' || :query || '%'
                OR tags LIKE '%' || :query || '%'
              )
        ORDER BY dueDate ASC, createdAt DESC
        """
    )
    fun observeFilteredTasks(
        status: String?,
        priority: String?,
        query: String
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    fun observeTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    // Hitung per status

    @Query("SELECT status AS status, COUNT(*) AS total FROM tasks GROUP BY status")
    fun observeStatusCounts(): Flow<List<StatusCount>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    fun observeCountByStatus(status: TaskStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    fun observeTotalCount(): Flow<Int>
}