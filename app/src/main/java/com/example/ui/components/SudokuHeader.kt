package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.LimeAccent
import com.example.viewmodel.SudokuUiState
import java.util.Locale

@Composable
fun SudokuHeader(
  uiState: SudokuUiState,
  onDifficultyClick: () -> Unit,
  onToggleTheme: () -> Unit,
  onTogglePause: () -> Unit,
  onSettingsClick: () -> Unit,
  onStatsClick: () -> Unit,
  onHowToPlayClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkTheme

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Bar: Logo & Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // App Branding with Cool Sudoku Logo
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("app_brand_logo")
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
              width = 1.5.dp,
              color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(10.dp)
            )
        ) {
          Image(
            painter = painterResource(id = R.drawable.sudoku_cool_logo),
            contentDescription = "Logo ShuDoku",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "SHU",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "DOKU",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = "9x9 MASTER",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Quick Action Buttons
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Panduan Pemula
        IconButton(
          onClick = onHowToPlayClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("button_how_to_play")
        ) {
          Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = "Panduan Pemula",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Statistik
        IconButton(
          onClick = onStatsClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("button_stats")
        ) {
          Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = "Statistik",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Pengaturan & Bantuan Pemula
        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("button_settings")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Pengaturan Bantuan",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Toggle Tema Gelap / Terang
        IconButton(
          onClick = onToggleTheme,
          modifier = Modifier
            .size(40.dp)
            .testTag("button_toggle_theme")
        ) {
          Icon(
            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (isDark) "Ubah ke Tema Terang" else "Ubah ke Tema Gelap",
            tint = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    // Status Row: Difficulty chip, Mistakes, and Timer
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Difficulty Selector Chip
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .clickable(onClick = onDifficultyClick)
          .testTag("chip_difficulty"),
        color = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = uiState.difficulty.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Pilih Kesulitan",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Mistakes Counter
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("counter_mistakes")
      ) {
        Text(
          text = "Kesalahan: ",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = if (uiState.settings.mistakeLimitEnabled) {
            "${uiState.mistakesCount}/${uiState.settings.maxMistakes}"
          } else {
            "${uiState.mistakesCount}"
          },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = if (uiState.mistakesCount > 0) ErrorColor else MaterialTheme.colorScheme.onSurface
        )
      }

      // Timer & Pause/Play
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .clickable(onClick = onTogglePause)
          .testTag("timer_chip"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (uiState.isPaused) "Lanjutkan" else "Jeda",
            tint = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = formatTime(uiState.timerSeconds),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

fun formatTime(seconds: Long): String {
  val mins = seconds / 60
  val secs = seconds % 60
  return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
