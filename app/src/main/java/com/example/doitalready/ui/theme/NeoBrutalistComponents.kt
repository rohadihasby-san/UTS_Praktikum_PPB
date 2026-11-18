package com.example.doitalready.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.doitalready.data.TaskStatus
import com.example.doitalready.ui.theme.DoItAlreadyTheme
import com.example.doitalready.ui.theme.NeoBlack
import com.example.doitalready.ui.theme.NeoDisabled
import com.example.doitalready.ui.theme.NeoGrayText
import com.example.doitalready.ui.theme.NeoMagenta
import com.example.doitalready.ui.theme.NeoMint
import com.example.doitalready.ui.theme.NeoTeal
import com.example.doitalready.ui.theme.NeoWhite
import com.example.doitalready.ui.theme.NeoYellow

/** Ukuran baku gaya Soft Neo-Brutalist. */
object NeoDefaults {
    val CornerRadius = 10.dp
    val BorderWidth = 2.dp
    val ShadowOffset = 3.dp
    val Shape = RoundedCornerShape(CornerRadius)
}

/**
 * Dasar semua komponen: lapisan bayangan hitam keras (tanpa blur) di belakang,
 * lapisan depan berwarna dengan border hitam. Kalau bisa diklik, lapisan depan
 * bergeser menutupi bayangan saat ditekan, jadi terasa seperti tombol fisik.
 */
@Composable
private fun NeoShadowBox(
    modifier: Modifier,
    containerColor: Color,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    role: Role?,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isClickable = onClick != null && enabled

    val pressOffset by animateDpAsState(
        targetValue = if (isClickable && isPressed) NeoDefaults.ShadowOffset else 0.dp,
        label = "neoPressOffset"
    )

    // Padding kanan & bawah menyediakan ruang untuk bayangan supaya tidak terpotong.
    Box(
        modifier = modifier.padding(
            end = NeoDefaults.ShadowOffset,
            bottom = NeoDefaults.ShadowOffset
        ),
        propagateMinConstraints = true
    ) {
        // Bayangan
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(NeoDefaults.ShadowOffset, NeoDefaults.ShadowOffset)
                .background(NeoBlack, NeoDefaults.Shape)
        )

        // Lapisan depan
        val frontModifier = Modifier
            .offset(pressOffset, pressOffset)
            .clip(NeoDefaults.Shape)
            .background(containerColor)
            .border(NeoDefaults.BorderWidth, NeoBlack, NeoDefaults.Shape)

        Box(
            modifier = if (onClick != null) {
                frontModifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onClick = onClick
                )
            } else {
                frontModifier
            },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Kartu putih dengan border dan bayangan keras.
 * Isi [onClick] kalau kartu bisa diklik (mis. untuk buka detail tugas).
 */
@Composable
fun NeoBrutalistCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NeoWhite,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    NeoShadowBox(
        modifier = modifier,
        containerColor = containerColor,
        onClick = onClick,
        enabled = true,
        role = if (onClick != null) Role.Button else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Tombol dengan teks. [leadingIcon] opsional (mis. Image dari res/drawable).
 */
@Composable
fun NeoBrutalistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoTeal,
    contentColor: Color = NeoBlack,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    NeoShadowBox(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        containerColor = if (enabled) containerColor else NeoDisabled,
        onClick = onClick,
        enabled = enabled,
        role = Role.Button
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp - NeoDefaults.ShadowOffset)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) contentColor else NeoGrayText,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Warna pill untuk tiap status. Bisa dipakai ulang di layar lain. */
fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.TODO -> NeoYellow
    TaskStatus.IN_PROGRESS -> NeoTeal
    TaskStatus.DONE -> NeoMint
}

/**
 * Label kecil penanda status tugas (To Do / In Progress / Done).
 */
@Composable
fun StatusPill(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    NeoShadowBox(
        modifier = modifier,
        containerColor = statusColor(status),
        onClick = null,
        enabled = true,
        role = null
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NeoBlack,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Tombol aksi melayang (FAB) berbentuk kotak, default Magenta.
 * Isi [content] dengan Icon, mis. tanda plus.
 */
@Composable
fun NeoBrutalistFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoMagenta,
    content: @Composable () -> Unit
) {
    NeoShadowBox(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        containerColor = containerColor,
        onClick = onClick,
        enabled = true,
        role = Role.Button
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Chip kecil untuk filter. Saat [selected] warnanya berubah ke [selectedColor].
 */
@Composable
fun NeoBrutalistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = NeoTeal
) {
    NeoShadowBox(
        modifier = modifier.semantics { this.selected = selected },
        containerColor = if (selected) selectedColor else NeoWhite,
        onClick = onClick,
        enabled = true,
        role = Role.Button
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = NeoBlack,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5DC)
@Composable
private fun NeoBrutalistComponentsPreview() {
    DoItAlreadyTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
                Text("Laporan Praktikum", style = MaterialTheme.typography.titleMedium)
                Text("Pemrograman Perangkat Bergerak", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(TaskStatus.TODO)
                StatusPill(TaskStatus.IN_PROGRESS)
                StatusPill(TaskStatus.DONE)
            }
            NeoBrutalistButton(text = "Simpan Tugas", onClick = {})
            NeoBrutalistButton(text = "Nonaktif", onClick = {}, enabled = false)
        }
    }
}