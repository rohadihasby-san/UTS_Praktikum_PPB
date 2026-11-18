package com.example.doitalready

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.doitalready.data.AppDatabase
import com.example.doitalready.data.TaskEntity
import com.example.doitalready.data.TaskPriority
import com.example.doitalready.data.TaskRepository
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.util.DateUtils
import com.example.doitalready.util.TaskIntent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Batas jumlah lampiran per tugas. */
const val MAX_ATTACHMENTS = 10

/** Pesan error per field. null = field valid (atau belum perlu ditampilkan). */
data class TaskFormErrors(
    val courseName: String? = null,
    val title: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null
) {
    val hasError: Boolean
        get() = courseName != null || title != null || startDate != null || dueDate != null
}

/** Seluruh state yang dibutuhkan layar form. */
data class TaskFormUiState(
    val isNewTask: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val courseName: String = "",
    val title: String = "",
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val tagsInput: String = "",
    val attachments: List<String> = emptyList(),
    val errors: TaskFormErrors = TaskFormErrors()
)

/** Kejadian sekali tampil dari ViewModel ke Activity. */
sealed interface TaskDetailEvent {
    data class ShowMessage(val text: String) : TaskDetailEvent
    data class Finish(val message: String?) : TaskDetailEvent
}

private data class FormFields(
    val courseName: String,
    val title: String,
    val startDate: Long?,
    val dueDate: Long?,
    val tagsInput: String
)

private data class FormChoices(
    val status: TaskStatus,
    val priority: TaskPriority,
    val attachments: List<String>,
    val submitAttempted: Boolean,
    val isLoading: Boolean
)

/**
 * ViewModel form Create / Update tugas.
 *
 * Semua isian form disimpan di [SavedStateHandle], jadi tidak hilang saat layar
 * diputar, dan tetap pulih kalau proses aplikasi dimatikan sistem.
 * Mode ditentukan oleh extra "TASK_ID": -1 berarti tugas baru, selain itu edit.
 */
class TaskDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = TaskRepository(
        AppDatabase.getInstance(application).taskDao()
    )

    private val taskId: Long =
        savedStateHandle.get<Long>(TaskIntent.EXTRA_TASK_ID) ?: TaskIntent.NEW_TASK_ID
    private val isNewTask: Boolean = taskId == TaskIntent.NEW_TASK_ID

    // Nilai awal untuk tugas baru: tanggal mulai = hari ini (hanya sekali).
    init {
        if (isNewTask && savedStateHandle.get<Boolean>(KEY_INITIALIZED) != true) {
            savedStateHandle[KEY_START_DATE] = DateUtils.startOfDay(System.currentTimeMillis())
            savedStateHandle[KEY_INITIALIZED] = true
        }
    }

    // ---------- State form (tersimpan di SavedStateHandle) ----------

    private val courseName = savedStateHandle.getStateFlow(KEY_COURSE_NAME, "")
    private val title = savedStateHandle.getStateFlow(KEY_TITLE, "")
    private val startDate = savedStateHandle.getStateFlow<Long?>(KEY_START_DATE, null)
    private val dueDate = savedStateHandle.getStateFlow<Long?>(KEY_DUE_DATE, null)
    private val tagsInput = savedStateHandle.getStateFlow(KEY_TAGS, "")
    private val statusName = savedStateHandle.getStateFlow(KEY_STATUS, TaskStatus.TODO.name)
    private val priorityName = savedStateHandle.getStateFlow(KEY_PRIORITY, TaskPriority.MEDIUM.name)
    private val attachmentsRaw = savedStateHandle.getStateFlow(KEY_ATTACHMENTS, "")
    private val submitAttempted = savedStateHandle.getStateFlow(KEY_SUBMIT_ATTEMPTED, false)

    // ---------- State sementara (tidak perlu disimpan) ----------

    private val _isLoading = MutableStateFlow(
        !isNewTask && savedStateHandle.get<Boolean>(KEY_INITIALIZED) != true
    )
    private val _isSaving = MutableStateFlow(false)

    private val _events = Channel<TaskDetailEvent>(Channel.BUFFERED)
    val events: Flow<TaskDetailEvent> = _events.receiveAsFlow()

    private val fields = combine(
        courseName, title, startDate, dueDate, tagsInput
    ) { course, taskTitle, start, due, tags ->
        FormFields(course, taskTitle, start, due, tags)
    }

    private val choices = combine(
        statusName, priorityName, attachmentsRaw, submitAttempted, _isLoading
    ) { status, priority, attachments, submitted, loading ->
        FormChoices(
            status = TaskStatus.fromName(status),
            priority = TaskPriority.fromName(priority),
            attachments = decodeAttachments(attachments),
            submitAttempted = submitted,
            isLoading = loading
        )
    }

    val uiState: StateFlow<TaskFormUiState> = combine(
        fields, choices, _isSaving
    ) { form, choice, saving ->
        TaskFormUiState(
            isNewTask = isNewTask,
            isLoading = choice.isLoading,
            isSaving = saving,
            courseName = form.courseName,
            title = form.title,
            startDate = form.startDate,
            dueDate = form.dueDate,
            status = choice.status,
            priority = choice.priority,
            tagsInput = form.tagsInput,
            attachments = choice.attachments,
            errors = validateForm(
                courseName = form.courseName,
                title = form.title,
                startDate = form.startDate,
                dueDate = form.dueDate,
                showRequired = choice.submitAttempted
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = TaskFormUiState(isNewTask = isNewTask, isLoading = _isLoading.value)
    )

    init {
        if (_isLoading.value) loadTask()
    }

    // ---------- Aksi dari UI ----------

    fun onCourseNameChange(value: String) {
        savedStateHandle[KEY_COURSE_NAME] = value.take(MAX_COURSE_LENGTH)
    }

    fun onTitleChange(value: String) {
        savedStateHandle[KEY_TITLE] = value.take(MAX_TITLE_LENGTH)
    }

    fun onTagsChange(value: String) {
        savedStateHandle[KEY_TAGS] = value.take(MAX_TAGS_LENGTH)
    }

    fun onStatusChange(status: TaskStatus) {
        savedStateHandle[KEY_STATUS] = status.name
    }

    fun onPriorityChange(priority: TaskPriority) {
        savedStateHandle[KEY_PRIORITY] = priority.name
    }

    /** [localDayMillis] = jam 00:00 hari yang dipilih, zona waktu perangkat. */
    fun onStartDateChange(localDayMillis: Long) {
        savedStateHandle[KEY_START_DATE] = localDayMillis
        val due = dueDate.value
        // Deadline yang jadi lebih awal dari tanggal mulai dikosongkan.
        if (due != null && due < localDayMillis) {
            savedStateHandle[KEY_DUE_DATE] = null as Long?
            _events.trySend(
                TaskDetailEvent.ShowMessage("Deadline direset karena lebih awal dari tanggal mulai")
            )
        }
    }

    fun onDueDateChange(localDayMillis: Long) {
        val start = startDate.value
        if (start != null && localDayMillis < start) {
            _events.trySend(
                TaskDetailEvent.ShowMessage("Deadline tidak boleh sebelum tanggal mulai")
            )
            return
        }
        savedStateHandle[KEY_DUE_DATE] = localDayMillis
    }

    fun addAttachment(uriString: String) {
        val current = decodeAttachments(attachmentsRaw.value)
        when {
            uriString.isBlank() || uriString in current -> return
            current.size >= MAX_ATTACHMENTS -> {
                _events.trySend(
                    TaskDetailEvent.ShowMessage("Maksimal $MAX_ATTACHMENTS lampiran")
                )
            }
            else -> savedStateHandle[KEY_ATTACHMENTS] = encodeAttachments(current + uriString)
        }
    }

    fun removeAttachment(uriString: String) {
        val current = decodeAttachments(attachmentsRaw.value)
        savedStateHandle[KEY_ATTACHMENTS] = encodeAttachments(current.filterNot { it == uriString })
    }

    fun save() {
        if (_isSaving.value || _isLoading.value) return

        savedStateHandle[KEY_SUBMIT_ATTEMPTED] = true

        val start = startDate.value
        val due = dueDate.value
        val errors = validateForm(
            courseName = courseName.value,
            title = title.value,
            startDate = start,
            dueDate = due,
            showRequired = true
        )
        if (errors.hasError || start == null || due == null) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val entity = TaskEntity(
                    id = if (isNewTask) 0L else taskId,
                    courseName = courseName.value.trim(),
                    title = title.value.trim(),
                    startDate = start,
                    dueDate = due,
                    status = TaskStatus.fromName(statusName.value),
                    priority = TaskPriority.fromName(priorityName.value),
                    tags = parseTags(tagsInput.value),
                    attachmentList = decodeAttachments(attachmentsRaw.value),
                    createdAt = savedStateHandle.get<Long>(KEY_CREATED_AT)
                        ?: System.currentTimeMillis()
                )

                if (isNewTask) {
                    repository.addTask(entity)
                    _events.trySend(TaskDetailEvent.Finish("Tugas disimpan"))
                } else {
                    val updated = repository.updateTask(entity)
                    _events.trySend(
                        TaskDetailEvent.Finish(
                            if (updated) "Perubahan disimpan" else "Tugas tidak ditemukan"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.trySend(TaskDetailEvent.ShowMessage("Gagal menyimpan tugas"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ---------- Internal ----------

    /** Muat data tugas untuk mode edit. Hanya sekali; setelah itu isian user yang dipakai. */
    private fun loadTask() {
        viewModelScope.launch {
            try {
                val task = repository.getTaskById(taskId)
                if (task == null) {
                    _events.trySend(TaskDetailEvent.Finish("Tugas tidak ditemukan"))
                } else {
                    savedStateHandle[KEY_COURSE_NAME] = task.courseName
                    savedStateHandle[KEY_TITLE] = task.title
                    savedStateHandle[KEY_START_DATE] = task.startDate
                    savedStateHandle[KEY_DUE_DATE] = task.dueDate
                    savedStateHandle[KEY_STATUS] = task.status.name
                    savedStateHandle[KEY_PRIORITY] = task.priority.name
                    savedStateHandle[KEY_TAGS] = task.tags.joinToString(", ")
                    savedStateHandle[KEY_ATTACHMENTS] = encodeAttachments(task.attachmentList)
                    savedStateHandle[KEY_CREATED_AT] = task.createdAt
                    savedStateHandle[KEY_INITIALIZED] = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.trySend(TaskDetailEvent.Finish("Gagal memuat tugas"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private companion object {
        const val KEY_COURSE_NAME = "form_course_name"
        const val KEY_TITLE = "form_title"
        const val KEY_START_DATE = "form_start_date"
        const val KEY_DUE_DATE = "form_due_date"
        const val KEY_STATUS = "form_status"
        const val KEY_PRIORITY = "form_priority"
        const val KEY_TAGS = "form_tags"
        const val KEY_ATTACHMENTS = "form_attachments"
        const val KEY_SUBMIT_ATTEMPTED = "form_submit_attempted"
        const val KEY_CREATED_AT = "form_created_at"
        const val KEY_INITIALIZED = "form_initialized"

        const val MAX_COURSE_LENGTH = 60
        const val MAX_TITLE_LENGTH = 100
        const val MAX_TAGS_LENGTH = 100
        const val MAX_TAGS = 10
        const val ATTACHMENT_SEPARATOR = "\n"
        const val STOP_TIMEOUT_MS = 5_000L

        /** Lampiran disimpan sebagai satu teks (satu URI per baris). */
        fun encodeAttachments(list: List<String>): String =
            list.joinToString(ATTACHMENT_SEPARATOR)

        fun decodeAttachments(raw: String): List<String> =
            raw.split(ATTACHMENT_SEPARATOR).filter { it.isNotBlank() }

        /** "kuis, #UTS , kuis" -> ["kuis", "UTS"] */
        fun parseTags(input: String): List<String> =
            input.split(",")
                .map { it.trim().removePrefix("#").trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_TAGS)

        /**
         * Validasi form. Aturan urutan tanggal selalu aktif; pesan "wajib diisi"
         * baru muncul setelah user menekan Simpan ([showRequired]).
         */
        fun validateForm(
            courseName: String,
            title: String,
            startDate: Long?,
            dueDate: Long?,
            showRequired: Boolean
        ): TaskFormErrors {
            val dateOrderError =
                if (startDate != null && dueDate != null && dueDate < startDate) {
                    "Deadline tidak boleh sebelum tanggal mulai"
                } else {
                    null
                }
            return TaskFormErrors(
                courseName = if (showRequired && courseName.isBlank()) {
                    "Nama mata kuliah wajib diisi"
                } else {
                    null
                },
                title = if (showRequired && title.isBlank()) {
                    "Judul tugas wajib diisi"
                } else {
                    null
                },
                startDate = if (showRequired && startDate == null) {
                    "Tanggal mulai wajib dipilih"
                } else {
                    null
                },
                dueDate = when {
                    dateOrderError != null -> dateOrderError
                    showRequired && dueDate == null -> "Deadline wajib dipilih"
                    else -> null
                }
            )
        }
    }
}