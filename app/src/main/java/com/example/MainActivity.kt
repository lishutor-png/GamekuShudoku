package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DifficultyDialog
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatsDialog
import com.example.ui.components.SudokuBoard
import com.example.ui.components.SudokuHeader
import com.example.ui.components.SudokuKeypad
import com.example.ui.components.VictoryDialog
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: SudokuViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
          SudokuGameScreen(
            viewModel = viewModel,
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    viewModel.saveGameProgress()
  }
}

@Composable
fun SudokuGameScreen(
  viewModel: SudokuViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isDark = uiState.isDarkTheme

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 500.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header Section
      SudokuHeader(
        uiState = uiState,
        onDifficultyClick = { viewModel.showDifficultyDialog(true) },
        onToggleTheme = { viewModel.toggleTheme() },
        onTogglePause = { viewModel.togglePause() },
        onSettingsClick = { viewModel.showSettingsDialog(true) },
        onStatsClick = { viewModel.showStatsDialog(true) },
        onHowToPlayClick = { viewModel.showHowToPlayDialog(true) }
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 9x9 Sudoku Board
      SudokuBoard(
        uiState = uiState,
        onCellClick = { r, c -> viewModel.selectCell(r, c) },
        onResumeClick = { viewModel.setPaused(false) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Keypad & Controls
      SudokuKeypad(
        uiState = uiState,
        onNumberClick = { num -> viewModel.selectKeypadNumber(num) },
        onEraseClick = { viewModel.eraseCell() },
        onUndoClick = { viewModel.undo() },
        onToggleNotesClick = { viewModel.toggleNotesMode() },
        onCheckClick = { viewModel.checkCurrentBoard() },
        onHintClick = { viewModel.giveHint() },
        onDismissHint = { viewModel.dismissHintMessage() }
      )

      Spacer(modifier = Modifier.height(12.dp))
    }

    // Dialogs
    if (uiState.showDifficultyDialog) {
      DifficultyDialog(
        currentDifficulty = uiState.difficulty,
        onSelectDifficulty = { diff -> viewModel.startNewGame(diff) },
        onDismiss = { viewModel.showDifficultyDialog(false) },
        isDark = isDark
      )
    }

    if (uiState.showSettingsDialog) {
      SettingsDialog(
        settings = uiState.settings,
        onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) },
        onAutoFillNotes = { viewModel.autoFillAllNotes() },
        onClearNotes = { viewModel.clearAllNotes() },
        onDismiss = { viewModel.showSettingsDialog(false) },
        isDark = isDark
      )
    }

    if (uiState.showStatsDialog) {
      StatsDialog(
        uiState = uiState,
        onDismiss = { viewModel.showStatsDialog(false) }
      )
    }

    if (uiState.showHowToPlayDialog) {
      HowToPlayDialog(
        onDismiss = { viewModel.showHowToPlayDialog(false) }
      )
    }

    if (uiState.showVictoryDialog) {
      VictoryDialog(
        uiState = uiState,
        onPlayAgain = { viewModel.restartCurrentGame() },
        onChangeDifficulty = {
          viewModel.showVictoryDialog(false)
          viewModel.showDifficultyDialog(true)
        },
        onDismiss = { viewModel.showVictoryDialog(false) }
      )
    }

    // Game Over Dialog (If 3-mistakes mode is triggered)
    if (uiState.isGameOver) {
      AlertDialog(
        onDismissRequest = { /* Force action */ },
        title = { Text("Game Selesai", fontWeight = FontWeight.Bold) },
        text = {
          Text(
            "Kamu telah mencapai batas ${uiState.settings.maxMistakes} kesalahan. Jangan menyerah! Coba lagi atau aktifkan mode santai di Pengaturan."
          )
        },
        confirmButton = {
          Button(
            onClick = { viewModel.restartCurrentGame() },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
            )
          ) {
            Text(
              "Coba Lagi",
              color = if (isDark) Color(0xFF142900) else Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              viewModel.updateSettings(uiState.settings.copy(mistakeLimitEnabled = false))
              viewModel.restartCurrentGame()
            }
          ) {
            Text("Main Tanpa Batas Kesalahan")
          }
        }
      )
    }
  }
}
