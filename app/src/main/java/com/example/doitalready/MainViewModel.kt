package com.example.doitalready

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.doitalready.data.AppDatabase
import com.example.doitalready.data.TaskEntity
import com.example.doitalready.data.TaskRepository
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Satu batang di grafik 7 hari
data class DayStat(
    val dayStartMillis: Long,
    val label: String,
    val count: Int,
    val isToday: Boolean
)

// Semua data yang dibutuhkan layar Dashboard
data class DashboardUiState(
    val totalTasks: Int = 0,
    val statusCounts: Map<TaskStatus, Int> = TaskStatus.entries.associateWith { 0 },
    val weeklyStats: List<DayStat> = emptyList(),
    val dueSoonCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(
        AppDatabase.getInstance(application).taskDao()
    )

    // Filter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    val statusFilter: StateFlow<TaskStatus?> = _statusFilter.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query.take(MAX_QUERY_LENGTH)
    }

    // null = tampilkan semua status
    fun onStatusFilterChange(status: TaskStatus?) {
        _statusFilter.value = status
    }

    // Daftar tugas (ikut filter)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> =
        combine(_searchQuery, _statusFilter) { query, status -> query to status }
            .flatMapLatest { (query, status) ->
                repository.observeFilteredTasks(
                    status = status,
                    priority = null,
                    query = query
                )
            }
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    // Dashboard (tidak terpengaruh filter)

    val dashboard: StateFlow<DashboardUiState> =
        combine(
            repository.observeAllTasks(),
            repository.observeStatusSummary()
        ) { allTasks, summary ->
            buildDashboard(allTasks, summary)
        }
            .catch { emit(DashboardUiState()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DashboardUiState()
            )

    // Pesan sekali tampil (untuk Toast)

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    //  Hapus

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val deleted = repository.deleteTaskById(taskId)
                _events.trySend(if (deleted) "Tugas dihapus" else "Tugas tidak ditemukan")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.trySend("Gagal menghapus tugas")
            }
        }


    }

    // Hitung statistik

    private fun buildDashboard(
        allTasks: List<TaskEntity>,
        summary: Map<TaskStatus, Int>
    ): DashboardUiState {
        val now = System.currentTimeMillis()
        val today = DateUtils.startOfDay(now)

        // Jumlah tugas yang jatuh tempo per hari, hari ini sampai 6 hari ke depan.
        val weekly = (0 until WEEK_DAYS).map { offset ->
            val day = DateUtils.addDays(today, offset)
            DayStat(
                dayStartMillis = day,
                label = DateUtils.shortDayName(day),
                count = allTasks.count { DateUtils.startOfDay(it.dueDate) == day },
                isToday = offset == 0
            )
        }

        // H-3: belum selesai, deadline hari ini sampai 3 hari lagi.
        val dueSoon = allTasks.count { task ->
            task.status != TaskStatus.DONE &&
                    DateUtils.daysBetween(now, task.dueDate) in 0L..DUE_SOON_DAYS
        }

        return DashboardUiState(
            totalTasks = allTasks.size,
            statusCounts = summary,
            weeklyStats = weekly,
            dueSoonCount = dueSoon
        )
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 100
        const val STOP_TIMEOUT_MS = 5_000L
        const val WEEK_DAYS = 7
        const val DUE_SOON_DAYS = 3L
    }
}