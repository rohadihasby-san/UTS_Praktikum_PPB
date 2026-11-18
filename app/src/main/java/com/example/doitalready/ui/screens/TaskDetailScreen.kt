package com.example.doitalready.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.doitalready.MAX_ATTACHMENTS
import com.example.doitalready.TaskFormUiState
import com.example.doitalready.data.TaskPriority
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.ui.components.NeoBrutalistButton
import com.example.doitalready.ui.components.NeoBrutalistCard
import com.example.doitalready.ui.components.NeoBrutalistChip
import com.example.doitalready.ui.components.NeoDefaults
import com.example.doitalready.ui.components.statusColor
import com.example.doitalready.ui.theme.NeoBlack
import com.example.doitalready.ui.theme.NeoError
import com.example.doitalready.ui.theme.NeoGrayText
import com.example.doitalready.ui.theme.NeoMagenta
import com.example.doitalready.ui.theme.NeoMint
import com.example.doitalready.ui.theme.NeoTeal
import com.example.doitalready.ui.theme.NeoWhite
import com.example.doitalready.ui.theme.NeoYellow
import com.example.doitalready.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PICKER_NONE = 0
private const val PICKER_START = 1
private const val PICKER_DUE = 2

@Composable
fun TaskDetailScreen(
    state: TaskFormUiState,
    onCourseNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onDueDateChange: (Long) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onTagsChange: (String) -> Unit,
    onAttachmentAdded: (String) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Dialog tanggal yang sedang terbuka. rememberSaveable supaya tidak hilang saat rotasi.
    var activePicker by rememberSaveable { mutableIntStateOf(PICKER_NONE) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Penyedia file tidak mendukung izin permanen; URI tetap disimpan.
            }
            onAttachmentAdded(uri.toString())
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        FormHeader(isNewTask = state.isNewTask, onBack = onBack)

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Memuat tugas...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NeoGrayText
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeoTextField(
                    value = state.courseName,
                    onValueChange = onCourseNameChange,
                    label = "Mata Kuliah",
                    error = state.errors.courseName,
                    capitalization = KeyboardCapitalization.Words
                )
                NeoTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = "Judul Tugas",
                    error = state.errors.title,
                    capitalization = KeyboardCapitalization.Sentences
                )

                DateField(
                    label = "Tanggal Mulai",
                    millis = state.startDate,
                    error = state.errors.startDate,
                    onClick = { activePicker = PICKER_START }
                )
                DateField(
                    label = "Deadline",
                    millis = state.dueDate,
                    error = state.errors.dueDate,
                    onClick = { activePicker = PICKER_DUE }
                )

                SectionLabel("Status")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskStatus.entries.forEach { status ->
                        NeoBrutalistChip(
                            text = status.label.uppercase(),
                            selected = state.status == status,
                            onClick = { onStatusChange(status) },
                            selectedColor = statusColor(status)
                        )
                    }
                }

                SectionLabel("Prioritas")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
                        NeoBrutalistChip(
                            text = priority.label.uppercase(),
                            selected = state.priority == priority,
                            onClick = { onPriorityChange(priority) },
                            selectedColor = priorityColor(priority)
                        )
                    }
                }

                NeoTextField(
                    value = state.tagsInput,
                    onValueChange = onTagsChange,
                    label = "Tag (pisahkan dengan koma)",
                    error = null,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                )

                SectionLabel("Lampiran (${state.attachments.size}/$MAX_ATTACHMENTS)")
                state.attachments.forEach { uriString ->
                    AttachmentRow(
                        uriString = uriString,
                        onRemove = { onAttachmentRemoved(uriString) }
                    )
                }
                NeoBrutalistButton(
                    text = "Tambah Lampiran",
                    onClick = { attachmentLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow,
                    enabled = state.attachments.size < MAX_ATTACHMENTS,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            NeoBrutalistButton(
                text = when {
                    state.isSaving -> "MENYIMPAN..."
                    state.isNewTask -> "SIMPAN TUGAS"
                    else -> "SIMPAN PERUBAHAN"
                },
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 13.dp, top = 8.dp, bottom = 8.dp),
                enabled = !state.isSaving
            )
        }
    }

    when (activePicker) {
        PICKER_START -> NeoDatePickerDialog(
            initialLocalMillis = state.startDate ?: todayStart(),
            minLocalMillis = null,
            onConfirm = { picked ->
                onStartDateChange(picked)
                activePicker = PICKER_NONE
            },
            onDismiss = { activePicker = PICKER_NONE }
        )

        PICKER_DUE -> NeoDatePickerDialog(
            initialLocalMillis = state.dueDate ?: state.startDate ?: todayStart(),
            // Deadline tidak bisa dipilih sebelum tanggal mulai.
            minLocalMillis = state.startDate,
            onConfirm = { picked ->
                onDueDateChange(picked)
                activePicker = PICKER_NONE
            },
            onDismiss = { activePicker = PICKER_NONE }
        )
    }
}

@Composable
private fun FormHeader(isNewTask: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = NeoBlack
            )
        }
        Text(
            text = if (isNewTask) "Tambah Tugas" else "Edit Tugas",
            style = MaterialTheme.typography.headlineMedium,
            color = NeoBlack
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = NeoBlack
    )
}

@Composable
private fun neoFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NeoBlack,
    unfocusedTextColor = NeoBlack,
    errorTextColor = NeoBlack,
    focusedBorderColor = NeoBlack,
    unfocusedBorderColor = NeoBlack,
    errorBorderColor = NeoError,
    focusedContainerColor = NeoWhite,
    unfocusedContainerColor = NeoWhite,
    errorContainerColor = NeoWhite,
    focusedLabelColor = NeoBlack,
    unfocusedLabelColor = NeoGrayText,
    errorLabelColor = NeoError,
    cursorColor = NeoBlack,
    errorCursorColor = NeoError,
    errorSupportingTextColor = NeoError
)

@Composable
private fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    capitalization: KeyboardCapitalization,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        shape = NeoDefaults.Shape,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
        supportingText = if (error != null) {
            { Text(text = error, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction
        ),
        colors = neoFieldColors()
    )
}

/** Kolom tanggal read-only. Ditekan untuk membuka dialog kalender. */
@Composable
private fun DateField(
    label: String,
    millis: Long?,
    error: String?,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (millis != null) DateUtils.formatDate(millis) else "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            isError = error != null,
            shape = NeoDefaults.Shape,
            textStyle = MaterialTheme.typography.bodyLarge,
            label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
            placeholder = {
                Text(
                    text = "Pilih tanggal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoGrayText
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = NeoBlack
                )
            },
            supportingText = if (error != null) {
                { Text(text = error, style = MaterialTheme.typography.bodySmall) }
            } else {
                null
            },
            colors = neoFieldColors()
        )
        // Lapisan transparan penangkap sentuhan (TextField read-only tidak meneruskan klik).
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeoDatePickerDialog(
    initialLocalMillis: Long,
    minLocalMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Hari sebelum minLocalMillis tampil abu-abu dan tidak bisa dipilih.
    val selectableDates = remember(minLocalMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minLocalMillis == null ||
                        DateUtils.utcPickerToLocalStartOfDay(utcTimeMillis) >= minLocalMillis
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localToUtcPicker(initialLocalMillis),
        selectableDates = selectableDates
    )
    val pickerColors = DatePickerDefaults.colors(
        containerColor = NeoWhite,
        selectedDayContainerColor = NeoTeal,
        selectedDayContentColor = NeoBlack,
        todayContentColor = NeoBlack,
        todayDateBorderColor = NeoBlack,
        selectedYearContainerColor = NeoTeal,
        selectedYearContentColor = NeoBlack
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NeoBrutalistButton(
                text = "Pilih",
                onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected != null) {
                        onConfirm(DateUtils.utcPickerToLocalStartOfDay(selected))
                    }
                },
                enabled = pickerState.selectedDateMillis != null
            )
        },
        dismissButton = {
            NeoBrutalistButton(
                text = "Batal",
                onClick = onDismiss,
                containerColor = NeoWhite
            )
        },
        shape = NeoDefaults.Shape,
        colors = pickerColors
    ) {
        DatePicker(state = pickerState, colors = pickerColors)
    }
}

@Composable
private fun AttachmentRow(
    uriString: String,
    onRemove: () -> Unit
) {
    val name = rememberAttachmentName(uriString)
    NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = NeoBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus lampiran $name",
                    tint = NeoBlack
                )
            }
        }
    }
}

/** Nama file lampiran. Dibaca di thread IO; sementara itu tampil nama dari URI. */
@Composable
private fun rememberAttachmentName(uriString: String): String {
    val context = LocalContext.current
    val fallback = remember(uriString) {
        Uri.parse(uriString).lastPathSegment ?: uriString
    }
    val name by produceState(initialValue = fallback, uriString) {
        value = withContext(Dispatchers.IO) { queryDisplayName(context, uriString) } ?: fallback
    }
    return name
}

private fun queryDisplayName(context: Context, uriString: String): String? {
    return try {
        context.contentResolver.query(
            Uri.parse(uriString),
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.LOW -> NeoMint
    TaskPriority.MEDIUM -> NeoYellow
    TaskPriority.HIGH -> NeoMagenta
}

private fun todayStart(): Long = DateUtils.startOfDay(System.currentTimeMillis())