package com.example.doitalready.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Status pengerjaan tugas. Disimpan di Room sebagai teks (nama enum)
enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    companion object {
        // Konversi aman dari teks ke enum,
        // fallback ke TODO kalau datanya tidak dikenal
        fun fromName(name: String?): TaskStatus =
            entries.firstOrNull { it.name == name } ?: TODO
    }
}

// Tingkat prioritas tugas. Disimpan di Room sebagai teks (nama enum)
enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        // Konversi aman dari teks ke enum; fallback ke MEDIUM kalau datanya tidak dikenal
        fun fromName(name: String?): TaskPriority =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

// Satu baris tugas kuliah. Dengan catatan format sebagai berikut:
// - [startDate], [dueDate], [createdAt] = epoch milidetik.
// - [tags] dan [attachmentList] disimpan sebagai JSON array lewat [Converters]
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val courseName: String,
    val title: String,
    val startDate: Long,
    val dueDate: Long,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val tags: List<String> = emptyList(),
    val attachmentList: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)