package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LimeAccent
import com.example.viewmodel.SudokuUiState

@Composable
fun SudokuKeypad(
  uiState: SudokuUiState,
  onNumberClick: (Int) -> Unit,
  onEraseClick: () -> Unit,
  onUndoClick: () -> Unit,
  onToggleNotesClick: () -> Unit,
  onHintClick: () -> Unit,
  onDismissHint: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkTheme

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Hint notification toast/card
    AnimatedVisibility(
      visible = uiState.hintMessage != null,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      uiState.hintMessage?.let { msg ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hint_message_card"),
          colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            IconButton(
              onClick = onDismissHint,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Tutup petunjuk",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }

    // Action Row: Undo, Erase, Notes (Ragu-ragu), Hint
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      ActionButton(
        icon = Icons.AutoMirrored.Filled.Undo,
        label = "Urungkan",
        enabled = uiState.canUndo,
        onClick = onUndoClick,
        testTag = "action_undo"
      )

      ActionButton(
        icon = Icons.Default.Delete,
        label = "Hapus",
        enabled = uiState.selectedCell != null,
        onClick = onEraseClick,
        testTag = "action_erase"
      )

      // Ragu-ragu (Notes mode) with prominent active styling
      NotesToggleButton(
        isActive = uiState.isNotesMode,
        isDark = isDark,
        onClick = onToggleNotesClick,
        testTag = "action_notes"
      )

      ActionButton(
        icon = Icons.Default.Lightbulb,
        label = "Bantuan",
        enabled = true,
        onClick = onHintClick,
        accentColor = LimeAccent,
        testTag = "action_hint"
      )
    }

    // Status Banner for Notes Mode (Ragu-ragu)
    AnimatedVisibility(
      visible = uiState.isNotesMode,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) LimeAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(
          1.dp,
          if (isDark) LimeAccent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              tint = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Mode Ragu-ragu Aktif",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = "Ketuk angka untuk catatan pensil",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    val selectedCell = uiState.selectedCell
    val currentCellNotes = if (selectedCell != null) {
      uiState.board.getOrNull(selectedCell.first)?.getOrNull(selectedCell.second)?.notes ?: emptySet()
    } else {
      emptySet()
    }

    // Number Row (1 - 9)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      for (num in 1..9) {
        val remaining = uiState.numberCounts[num] ?: 0
        val isCompleted = remaining == 0
        val isSelectedKey = uiState.selectedKeypadNumber == num
        val isNotedInCell = currentCellNotes.contains(num)

        NumberKey(
          number = num,
          remaining = remaining,
          isCompleted = isCompleted,
          isSelected = isSelectedKey,
          isNotedInCell = isNotedInCell,
          isNotesMode = uiState.isNotesMode,
          isDark = isDark,
          onClick = { onNumberClick(num) },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun ActionButton(
  icon: ImageVector,
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
  accentColor: Color? = null
) {
  val contentColor = when {
    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    accentColor != null -> accentColor
    else -> MaterialTheme.colorScheme.onSurface
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .background(
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.8f else 0.3f),
          CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = contentColor
    )
  }
}

@Composable
private fun NotesToggleButton(
  isActive: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val containerColor = if (isActive) {
    if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
  }

  val contentColor = if (isActive) {
    if (isDark) Color(0xFF142900) else Color.White
  } else {
    MaterialTheme.colorScheme.onSurface
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .background(containerColor, CircleShape)
        .then(
          if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer, CircleShape)
          else Modifier
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Opsi Ragu-ragu (Catatan)",
        tint = contentColor,
        modifier = Modifier.size(20.dp)
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = if (isActive) "Ragu: ON" else "Ragu-ragu",
      fontSize = 11.sp,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
      color = if (isActive) (if (isDark) LimeAccent else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun NumberKey(
  number: Int,
  remaining: Int,
  isCompleted: Boolean,
  isSelected: Boolean,
  isNotedInCell: Boolean,
  isNotesMode: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val surfaceColor = when {
    isSelected -> if (isDark) LimeAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
    isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    isNotesMode && isNotedInCell -> if (isDark) LimeAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
  }

  val textColor = when {
    isSelected -> if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    else -> MaterialTheme.colorScheme.onSurface
  }

  val borderColor = when {
    isSelected -> if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
    isNotesMode && isNotedInCell -> if (isDark) LimeAccent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    else -> Color.Transparent
  }

  Surface(
    modifier = modifier
      .height(58.dp)
      .clip(RoundedCornerShape(10.dp))
      .border(if (isSelected || (isNotesMode && isNotedInCell)) 2.dp else 0.dp, borderColor, RoundedCornerShape(10.dp))
      .clickable(enabled = !isCompleted, onClick = onClick)
      .testTag("keypad_num_$number"),
    color = surfaceColor,
    shape = RoundedCornerShape(10.dp),
    tonalElevation = if (isSelected) 4.dp else 1.dp
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = number.toString(),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = textColor
        )
        Text(
          text = if (isCompleted) "✓" else remaining.toString(),
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (isCompleted) LimeAccent.copy(alpha = 0.8f) else textColor.copy(alpha = 0.6f)
        )
      }

      // Small indicator badge if this digit is noted in the selected cell
      if (isNotedInCell && !isCompleted) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(7.dp)
            .background(if (isDark) LimeAccent else MaterialTheme.colorScheme.primary, CircleShape)
        )
      }
    }
  }
}
