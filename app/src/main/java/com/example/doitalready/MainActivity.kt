package com.example.doitalready

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doitalready.ui.components.NeoBrutalistFab
import com.example.doitalready.ui.components.NeoDefaults
import com.example.doitalready.ui.screens.DashboardScreen
import com.example.doitalready.ui.screens.TaskListScreen
import com.example.doitalready.ui.theme.DoItAlreadyTheme
import com.example.doitalready.ui.theme.NeoBlack
import com.example.doitalready.ui.theme.NeoTeal
import com.example.doitalready.ui.theme.NeoWhite
import com.example.doitalready.util.TaskIntent

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aplikasi hanya mode terang, jadi ikon status bar dipaksa gelap.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            DoItAlreadyTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

private const val TAB_DASHBOARD = 0
private const val TAB_TASKS = 1

@Composable
private fun MainApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    // Tab aktif aman dari rotasi layar.
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_DASHBOARD) }

    // Pesan sekali tampil dari ViewModel (mis. "Tugas dihapus").
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Explicit Intent ke TaskDetailActivity. NEW_TASK_ID (-1) = mode tambah tugas.
    fun openDetail(taskId: Long) {
        val intent = Intent(context, TaskDetailActivity::class.java)
        intent.putExtra(TaskIntent.EXTRA_TASK_ID, taskId)
        context.startActivity(intent)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NeoBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            NeoBrutalistFab(
                onClick = { openDetail(TaskIntent.NEW_TASK_ID) },
                contentDescription = "Tambah tugas"
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NeoBlack,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            TAB_DASHBOARD -> DashboardScreen(
                state = dashboard,
                modifier = Modifier.padding(innerPadding)
            )

            else -> TaskListScreen(
                tasks = tasks,
                searchQuery = searchQuery,
                statusFilter = statusFilter,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onStatusFilterChange = viewModel::onStatusFilterChange,
                onOpenTask = { id -> openDetail(id) },
                onDeleteTask = viewModel::deleteTask,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// Navigasi bawah 2 tab, digambar manual supaya gayanya konsisten dengan Neo-Brutalist
@Composable
private fun NeoBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("DASHBOARD", "TUGAS")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeoWhite)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NeoDefaults.BorderWidth)
                .background(NeoBlack)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(NeoDefaults.BorderWidth)
                            .fillMaxHeight()
                            .background(NeoBlack)
                    )
                }
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) NeoTeal else NeoWhite)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = NeoBlack
                    )
                }
            }
        }
    }
}