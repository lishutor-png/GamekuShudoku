package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CellData
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeHighlightDark
import com.example.ui.theme.LimeHighlightLight
import com.example.ui.theme.RelatedCellDark
import com.example.ui.theme.RelatedCellLight
import com.example.ui.theme.SelectedCellDark
import com.example.ui.theme.SelectedCellLight
import com.example.viewmodel.SudokuUiState

@Composable
fun SudokuBoard(
  uiState: SudokuUiState,
  onCellClick: (Int, Int) -> Unit,
  onResumeClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkTheme

  // Colors for board lines - prominent 3x3 (9 kotak) divider
  val outerBorderColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
  val blockBorderColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
  val cellBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

  val selectedCell = uiState.selectedCell
  val activeValue = selectedCell?.let { uiState.board[it.first][it.second].value }
    ?: uiState.selectedKeypadNumber

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .padding(horizontal = 8.dp)
      .clip(RoundedCornerShape(16.dp))
      .border(2.5.dp, outerBorderColor, RoundedCornerShape(16.dp))
      .testTag("sudoku_board"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 4.dp
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellSize = maxWidth / 9

        Column(modifier = Modifier.fillMaxSize()) {
          for (r in 0 until 9) {
            Row(modifier = Modifier.fillMaxWidth()) {
              for (c in 0 until 9) {
                val cell = uiState.board.getOrNull(r)?.getOrNull(c) ?: CellData(r, c)
                val isSelected = selectedCell?.first == r && selectedCell?.second == c

                val isSameNumber = activeValue != null && cell.value == activeValue
                val isRelated = selectedCell != null && uiState.settings.highlightRowColBox &&
                    (selectedCell.first == r || selectedCell.second == c ||
                        (selectedCell.first / 3 == r / 3 && selectedCell.second / 3 == c / 3))

                SudokuCell(
                  cell = cell,
                  isSelected = isSelected,
                  isRelated = isRelated,
                  isSameNumber = isSameNumber && uiState.settings.highlightSameNumber,
                  isDark = isDark,
                  size = cellSize,
                  activeValue = activeValue,
                  onCellClick = { onCellClick(r, c) },
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                )
              }
            }
          }
        }

        // Draw overlay 3x3 block borders
        BlockDividers(
          cellSize = cellSize,
          dividerColor = blockBorderColor,
          subDividerColor = cellBorderColor
        )
      }

      // Pause Overlay
      AnimatedVisibility(
        visible = uiState.isPaused,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Lanjutkan",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
              )
            }
            Text(
              text = "Game Dijeda",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Papan disembunyikan agar timer tetap adil",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Button(
              onClick = onResumeClick,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                "Lanjutkan Bermain",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SudokuCell(
  cell: CellData,
  isSelected: Boolean,
  isRelated: Boolean,
  isSameNumber: Boolean,
  isDark: Boolean,
  size: Dp,
  activeValue: Int?,
  onCellClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val backgroundColor = when {
    cell.isError -> if (isDark) ErrorContainerDark else ErrorContainerLight
    isSelected -> if (isDark) SelectedCellDark else SelectedCellLight
    isSameNumber -> if (isDark) LimeHighlightDark else LimeHighlightLight
    isRelated -> if (isDark) RelatedCellDark else RelatedCellLight
    else -> Color.Transparent
  }

  val textColor = when {
    cell.isError -> ErrorColor
    cell.isGiven -> MaterialTheme.colorScheme.onSurface
    else -> if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
  }

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .background(backgroundColor)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onCellClick
      )
      .testTag("cell_${cell.row}_${cell.col}"),
    contentAlignment = Alignment.Center
  ) {
    // If selected, highlight with a rounded inner ring
    if (isSelected) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(2.dp)
          .border(
            2.dp,
            if (isDark) LimeAccent else MaterialTheme.colorScheme.primary,
            RoundedCornerShape(6.dp)
          )
      )
    }

    if (cell.value != null) {
      Text(
        text = cell.value.toString(),
        fontSize = 22.sp,
        fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.ExtraBold,
        color = textColor,
        textAlign = TextAlign.Center
      )
    } else if (cell.notes.isNotEmpty()) {
      // High-visibility Notes Grid for "Ragu-ragu"
      NotesGrid(
        notes = cell.notes,
        activeValue = activeValue,
        isDark = isDark
      )
    }
  }
}

@Composable
private fun NotesGrid(
  notes: Set<Int>,
  activeValue: Int?,
  isDark: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(1.dp),
    verticalArrangement = Arrangement.SpaceEvenly
  ) {
    for (row in 0 until 3) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        for (col in 0 until 3) {
          val num = row * 3 + col + 1
          val isPresent = notes.contains(num)
          val isMatchingActive = isPresent && activeValue != null && activeValue == num

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .padding(0.5.dp)
              .clip(RoundedCornerShape(3.dp))
              .then(
                when {
                  isMatchingActive -> Modifier.background(
                    if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
                  )
                  isPresent -> Modifier.background(
                    if (isDark) Color(0x38CCFF00) else Color(0x22006C4C)
                  )
                  else -> Modifier
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isPresent) {
              Text(
                text = num.toString(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 11.5.sp,
                color = when {
                  isMatchingActive -> if (isDark) Color.Black else Color.White
                  isDark -> Color(0xFFF2FF77)
                  else -> Color(0xFF034D35)
                },
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BlockDividers(
  cellSize: Dp,
  dividerColor: Color,
  subDividerColor: Color
) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val totalWidth = size.width
    val totalHeight = size.height
    val cellPx = totalWidth / 9f

    // 1. Draw thin cell dividers first
    for (i in 1 until 9) {
      if (i % 3 != 0) {
        val pos = cellPx * i
        // Horizontal thin line
        drawLine(
          color = subDividerColor,
          start = Offset(0f, pos),
          end = Offset(totalWidth, pos),
          strokeWidth = 1.dp.toPx()
        )
        // Vertical thin line
        drawLine(
          color = subDividerColor,
          start = Offset(pos, 0f),
          end = Offset(pos, totalHeight),
          strokeWidth = 1.dp.toPx()
        )
      }
    }

    // 2. Draw thick, prominent lines per 3x3 block (garis per 9 kotak)
    // at index 3 and 6
    for (b in listOf(3, 6)) {
      val pos = cellPx * b
      // Horizontal bold divider
      drawLine(
        color = dividerColor,
        start = Offset(0f, pos),
        end = Offset(totalWidth, pos),
        strokeWidth = 3.5.dp.toPx()
      )
      // Vertical bold divider
      drawLine(
        color = dividerColor,
        start = Offset(pos, 0f),
        end = Offset(pos, totalHeight),
        strokeWidth = 3.5.dp.toPx()
      )
    }
  }
}
