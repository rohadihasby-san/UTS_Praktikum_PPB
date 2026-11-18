package com.example.doitalready.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Satu-satunya pintu akses data tugas untuk ViewModel. ViewModel tidak boleh menyentuh DAO langsung
class TaskRepository(private val taskDao: TaskDao) {

    // Write

    // Simpan tugas baru. Mengembalikan id baris yang dibuat
    suspend fun addTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity): Boolean = taskDao.updateTask(task) > 0

    suspend fun deleteTask(task: TaskEntity): Boolean = taskDao.deleteTask(task) > 0

    suspend fun deleteTaskById(taskId: Long): Boolean = taskDao.deleteTaskById(taskId) > 0

    suspend fun updateStatus(taskId: Long, status: TaskStatus): Boolean =
        taskDao.updateStatus(taskId, status) > 0

    // Read

    // Null kalau id tidak ditemukan (misalnya sudah dihapus)
    suspend fun getTaskById(taskId: Long): TaskEntity? = taskDao.getTaskById(taskId)

    fun observeTaskById(taskId: Long): Flow<TaskEntity?> = taskDao.observeTaskById(taskId)

    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    fun observeTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>> =
        taskDao.observeTasksByStatus(status)

    // Filter gabungan. [status] / [priority] null = semua. [query] boleh kosong
    fun observeFilteredTasks(
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        query: String = ""
    ): Flow<List<TaskEntity>> = taskDao.observeFilteredTasks(
        status = status?.name,
        priority = priority?.name,
        query = query.trim()
    )

    // Hitung

    // Jumlah tugas per status. Status yang belum punya tugas tetap muncul dengan nilai 0, jadi UI tidak perlu cek null
    fun observeStatusSummary(): Flow<Map<TaskStatus, Int>> =
        taskDao.observeStatusCounts().map { counts ->
            TaskStatus.entries.associateWith { status ->
                counts.firstOrNull { it.status == status }?.total ?: 0
            }
        }

    fun observeCountByStatus(status: TaskStatus): Flow<Int> =
        taskDao.observeCountByStatus(status)

    fun observeTotalCount(): Flow<Int> = taskDao.observeTotalCount()
}