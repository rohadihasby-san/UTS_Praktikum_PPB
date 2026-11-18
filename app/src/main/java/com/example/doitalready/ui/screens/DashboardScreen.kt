package com.example.doitalready.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.doitalready.DashboardUiState
import com.example.doitalready.DayStat
import com.example.doitalready.R
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.ui.components.NeoBrutalistCard
import com.example.doitalready.ui.components.NeoDefaults
import com.example.doitalready.ui.components.statusColor
import com.example.doitalready.ui.theme.NeoBlack
import com.example.doitalready.ui.theme.NeoGrayText
import com.example.doitalready.ui.theme.NeoMagenta
import com.example.doitalready.ui.theme.NeoMint
import com.example.doitalready.ui.theme.NeoTeal
import com.example.doitalready.ui.theme.NeoYellow

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DashboardHeader()
        DueSoonCard(count = state.dueSoonCount)
        StatusSummarySection(state = state)
        WeeklyChartSection(stats = state.weeklyStats)
        Spacer(modifier = Modifier.height(72.dp)) // ruang agar tidak tertutup FAB
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Logo DoItAlready",
            modifier = Modifier.size(56.dp)
        )
        Column {
            Text(
                text = "DoItAlready",
                style = MaterialTheme.typography.headlineMedium,
                color = NeoBlack
            )
            Text(
                text = "Tugas kuliah, beres satu per satu.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeoGrayText
            )
        }
    }
}

@Composable
private fun DueSoonCard(count: Int) {
    val hasUrgent = count > 0
    NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (hasUrgent) NeoMagenta else NeoMint
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = NeoBlack
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasUrgent) "Deadline H-3" else "Aman terkendali",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeoBlack
                )
                Text(
                    text = if (hasUrgent) {
                        "tugas belum selesai yang jatuh tempo dalam 3 hari ke depan."
                    } else {
                        "tidak ada tugas yang jatuh tempo dalam 3 hari ke depan."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoBlack
                )
            }
        }
    }
}

@Composable
private fun StatusSummarySection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title = "Ringkasan Status", trailing = "${state.totalTasks} tugas")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TaskStatus.entries.forEach { status ->
                NeoBrutalistCard(
                    modifier = Modifier.weight(1f),
                    containerColor = statusColor(status),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) {
                    Text(
                        text = (state.statusCounts[status] ?: 0).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeoBlack
                    )
                    Text(
                        text = status.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeoBlack,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChartSection(stats: List<DayStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title = "Deadline 7 Hari ke Depan", trailing = null)
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
            if (stats.isEmpty()) {
                Text(
                    text = "Belum ada data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoGrayText
                )
            } else {
                WeeklyBarChart(stats = stats)
            }
        }
    }
}

// Grafik batang manual: tinggi tiap batang sebanding dengan jumlah tugas yang jatuh tempo di hari itu. Batang hari ini berwarna kuning
@Composable
private fun WeeklyBarChart(stats: List<DayStat>) {
    val chartHeight = 120.dp
    val maxCount = (stats.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEach { stat ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeoBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (stat.count == 0) {
                        // Garis dasar tipis untuk hari tanpa tugas.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(NeoBlack)
                        )
                    } else {
                        val fraction = stat.count.toFloat() / maxCount.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((chartHeight * fraction).coerceAtLeast(14.dp))
                                .background(
                                    color = if (stat.isToday) NeoYellow else NeoTeal,
                                    shape = NeoDefaults.Shape
                                )
                                .border(
                                    width = NeoDefaults.BorderWidth,
                                    color = NeoBlack,
                                    shape = NeoDefaults.Shape
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stat.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeoBlack,
                    fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = NeoBlack
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = NeoGrayText
            )
        }
    }
}