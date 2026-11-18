package com.example.doitalready.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.doitalready.R
import com.example.doitalready.data.TaskEntity
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.ui.components.NeoBrutalistButton
import com.example.doitalready.ui.components.NeoBrutalistCard
import com.example.doitalready.ui.components.NeoBrutalistChip
import com.example.doitalready.ui.components.NeoDefaults
import com.example.doitalready.ui.components.StatusPill
import com.example.doitalready.ui.theme.NeoBlack
import com.example.doitalready.ui.theme.NeoError
import com.example.doitalready.ui.theme.NeoGrayText
import com.example.doitalready.ui.theme.NeoMagenta
import com.example.doitalready.ui.theme.NeoWhite
import com.example.doitalready.ui.theme.NeoYellow
import com.example.doitalready.util.DateUtils

@Composable
fun TaskListScreen(
    tasks: List<TaskEntity>,
    searchQuery: String,
    statusFilter: TaskStatus?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onOpenTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Disimpan dengan rememberSaveable supaya dialog tetap ada setelah layar diputar.
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingTask = pendingDeleteId?.let { id -> tasks.firstOrNull { it.id == id } }
    val isFiltering = searchQuery.isNotBlank() || statusFilter != null

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Tugas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeoBlack
                )
                Text(
                    text = "${tasks.size} tugas",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeoGrayText
                )
            }
            SearchField(query = searchQuery, onQueryChange = onSearchQueryChange)
            StatusFilterRow(selected = statusFilter, onSelected = onStatusFilterChange)
        }

        if (tasks.isEmpty()) {
            EmptyState(isFiltering = isFiltering)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items = tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onOpen = { onOpenTask(task.id) },
                        onDelete = { pendingDeleteId = task.id }
                    )
                }
            }
        }
    }

    if (pendingTask != null) {
        DeleteDialog(
            taskTitle = pendingTask.title,
            onConfirm = {
                onDeleteTask(pendingTask.id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = NeoDefaults.Shape,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = "Cari judul, matkul, atau tag",
                style = MaterialTheme.typography.bodyMedium,
                color = NeoGrayText
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = NeoBlack
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus pencarian",
                        tint = NeoBlack
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NeoBlack,
            unfocusedTextColor = NeoBlack,
            focusedBorderColor = NeoBlack,
            unfocusedBorderColor = NeoBlack,
            focusedContainerColor = NeoWhite,
            unfocusedContainerColor = NeoWhite,
            cursorColor = NeoBlack
        )
    )
}

@Composable
private fun StatusFilterRow(
    selected: TaskStatus?,
    onSelected: (TaskStatus?) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NeoBrutalistChip(
            text = "SEMUA",
            selected = selected == null,
            onClick = { onSelected(null) }
        )
        TaskStatus.entries.forEach { status ->
            NeoBrutalistChip(
                text = status.label.uppercase(),
                selected = selected == status,
                onClick = { onSelected(status) }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val (dueText, dueColor) = dueLabel(task)

    NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        // Baris atas: matkul + judul, menu titik tiga di kanan.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.courseName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeoGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NeoBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu tugas",
                        tint = NeoBlack
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = NeoWhite,
                    shape = NeoDefaults.Shape,
                    border = BorderStroke(NeoDefaults.BorderWidth, NeoBlack)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = NeoBlack)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = NeoBlack)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "MULAI     ${DateUtils.formatDate(task.startDate)}",
            style = MaterialTheme.typography.labelMedium,
            color = NeoGrayText
        )
        Text(
            text = "DEADLINE  ${DateUtils.formatDate(task.dueDate)}",
            style = MaterialTheme.typography.labelMedium,
            color = NeoBlack
        )

        if (task.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.tags.take(MAX_TAGS_SHOWN).joinToString("  ") { "#$it" },
                style = MaterialTheme.typography.labelMedium,
                color = NeoGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status = task.status)
                Text(
                    text = task.priority.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeoBlack
                )
            }
            Text(
                text = dueText,
                style = MaterialTheme.typography.labelMedium,
                color = dueColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            NeoBrutalistButton(
                text = "Share",
                onClick = { shareTask(context, task) },
                containerColor = NeoYellow,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = NeoBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyState(isFiltering: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_task),
            contentDescription = "Belum ada tugas",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFiltering) "Tidak ada yang cocok" else "Belum ada tugas",
            style = MaterialTheme.typography.titleLarge,
            color = NeoBlack,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isFiltering) {
                "Coba ganti kata kunci atau filter statusnya."
            } else {
                "Tekan tombol + untuk menambah tugas pertamamu."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = NeoGrayText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeleteDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Hapus tugas?",
                style = MaterialTheme.typography.titleLarge,
                color = NeoBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$taskTitle\" akan dihapus permanen.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeoBlack
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                NeoBrutalistButton(
                    text = "Batal",
                    onClick = onDismiss,
                    containerColor = NeoWhite
                )
                NeoBrutalistButton(
                    text = "Hapus",
                    onClick = onConfirm,
                    containerColor = NeoMagenta
                )
            }
        }
    }
}

// Teks sisa waktu + warnanya. Tugas selesai tidak dihitung terlambat
private fun dueLabel(task: TaskEntity): Pair<String, Color> {
    if (task.status == TaskStatus.DONE) return "SELESAI" to NeoBlack
    val days = DateUtils.daysBetween(System.currentTimeMillis(), task.dueDate)
    return when {
        days < 0L -> "TERLAMBAT ${-days} HARI" to NeoError
        days == 0L -> "HARI INI" to NeoError
        else -> "H-$days" to NeoBlack
    }
}

// Bagikan ringkasan tugas lewat share sheet bawaan Android
private fun shareTask(context: Context, task: TaskEntity) {
    val message = buildString {
        appendLine(task.title)
        appendLine("Mata kuliah: ${task.courseName}")
        appendLine("Mulai: ${DateUtils.formatDate(task.startDate)}")
        appendLine("Deadline: ${DateUtils.formatDate(task.dueDate)}")
        append("Status: ${task.status.label}")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, task.title)
        putExtra(Intent.EXTRA_TEXT, message)
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan tugas"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi untuk berbagi", Toast.LENGTH_SHORT).show()
    }
}

private const val MAX_TAGS_SHOWN = 3