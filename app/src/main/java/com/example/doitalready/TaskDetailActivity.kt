package com.example.doitalready

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doitalready.ui.screens.TaskDetailScreen
import com.example.doitalready.ui.theme.DoItAlreadyTheme
import kotlinx.coroutines.launch

/**
 * Layar form tambah / edit tugas.
 * Dibuka lewat Explicit Intent dari MainActivity dengan extra "TASK_ID":
 * -1 = tugas baru, selain itu = edit tugas dengan id tersebut.
 */
class TaskDetailActivity : ComponentActivity() {

    private val viewModel: TaskDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        // Pesan sekali tampil dari ViewModel (toast dan tutup layar).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is TaskDetailEvent.ShowMessage -> {
                            Toast.makeText(this@TaskDetailActivity, event.text, Toast.LENGTH_SHORT).show()
                        }

                        is TaskDetailEvent.Finish -> {
                            event.message?.let { message ->
                                Toast.makeText(this@TaskDetailActivity, message, Toast.LENGTH_SHORT).show()
                            }
                            finish()
                        }
                    }
                }
            }
        }

        setContent {
            DoItAlreadyTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // safeDrawing sudah termasuk keyboard, jadi form naik saat keyboard muncul.
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    TaskDetailScreen(
                        state = state,
                        onCourseNameChange = viewModel::onCourseNameChange,
                        onTitleChange = viewModel::onTitleChange,
                        onStartDateChange = viewModel::onStartDateChange,
                        onDueDateChange = viewModel::onDueDateChange,
                        onStatusChange = viewModel::onStatusChange,
                        onPriorityChange = viewModel::onPriorityChange,
                        onTagsChange = viewModel::onTagsChange,
                        onAttachmentAdded = viewModel::addAttachment,
                        onAttachmentRemoved = viewModel::removeAttachment,
                        onSave = viewModel::save,
                        onBack = { finish() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}