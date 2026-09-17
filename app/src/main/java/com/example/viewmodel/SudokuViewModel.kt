package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.SudokuEngine
import com.example.model.AssistanceSettings
import com.example.model.BoardSnapshot
import com.example.model.CellData
import com.example.model.Difficulty
import com.example.model.GameStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SudokuUiState(
  val board: List<List<CellData>> = emptyList(),
  val solution: List<List<Int>> = emptyList(),
  val selectedCell: Pair<Int, Int>? = null,
  val selectedKeypadNumber: Int? = null, // For quick input / Number-first mode
  val isNotesMode: Boolean = false, // "Ragu-ragu" mode
  val difficulty: Difficulty = Difficulty.MUDAH,
  val timerSeconds: Long = 0L,
  val isPaused: Boolean = false,
  val mistakesCount: Int = 0,
  val isGameWon: Boolean = false,
  val isGameOver: Boolean = false,
  val isDarkTheme: Boolean = true, // Default modern dark theme with lime green
  val settings: AssistanceSettings = AssistanceSettings(),
  val stats: GameStats = GameStats(),
  val hintMessage: String? = null,
  val canUndo: Boolean = false,
  val numberCounts: Map<Int, Int> = emptyMap(), // Count remaining for 1..9
  val showDifficultyDialog: Boolean = false,
  val showSettingsDialog: Boolean = false,
  val showStatsDialog: Boolean = false,
  val showVictoryDialog: Boolean = false,
  val showHowToPlayDialog: Boolean = false
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("sudoku_prefs", Context.MODE_PRIVATE)

  private val _uiState = MutableStateFlow(SudokuUiState())
  val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

  private val history = mutableListOf<BoardSnapshot>()
  private var timerJob: Job? = null

  init {
    loadSavedPreferences()
    startNewGame(Difficulty.MUDAH)
  }

  private fun loadSavedPreferences() {
    val isDark = prefs.getBoolean("is_dark_theme", true)
    val highlightSame = prefs.getBoolean("highlight_same", true)
    val highlightLines = prefs.getBoolean("highlight_lines", true)
    val highlightDupes = prefs.getBoolean("highlight_dupes", true)
    val autoRemoveNotes = prefs.getBoolean("auto_remove_notes", true)
    val mistakeLimit = prefs.getBoolean("mistake_limit", false)

    val gamesPlayed = prefs.getInt("games_played", 0)
    val gamesWon = prefs.getInt("games_won", 0)

    val bestTimes = mutableMapOf<Difficulty, Long>()
    Difficulty.values().forEach { diff ->
      val t = prefs.getLong("best_time_${diff.name}", -1L)
      if (t > 0) bestTimes[diff] = t
    }

    _uiState.update {
      it.copy(
        isDarkTheme = isDark,
        settings = AssistanceSettings(
          highlightSameNumber = highlightSame,
          highlightRowColBox = highlightLines,
          highlightDuplicates = highlightDupes,
          autoRemoveNotes = autoRemoveNotes,
          mistakeLimitEnabled = mistakeLimit
        ),
        stats = GameStats(
          gamesPlayed = gamesPlayed,
          gamesWon = gamesWon,
          bestTimes = bestTimes
        )
      )
    }
  }

  fun startNewGame(difficulty: Difficulty = _uiState.value.difficulty) {
    timerJob?.cancel()
    history.clear()

    val generated = SudokuEngine.generatePuzzle(difficulty)
    val remainingCounts = calculateRemainingCounts(generated.initialBoard)

    _uiState.update {
      it.copy(
        board = generated.initialBoard,
        solution = generated.solution,
        selectedCell = null,
        selectedKeypadNumber = null,
        difficulty = difficulty,
        timerSeconds = 0L,
        isPaused = false,
        mistakesCount = 0,
        isGameWon = false,
        isGameOver = false,
        canUndo = false,
        numberCounts = remainingCounts,
        hintMessage = null,
        showDifficultyDialog = false,
        showVictoryDialog = false
      )
    }

    // Update stats: games played
    val currentPlayed = _uiState.value.stats.gamesPlayed + 1
    prefs.edit().putInt("games_played", currentPlayed).apply()
    _uiState.update { it.copy(stats = it.stats.copy(gamesPlayed = currentPlayed)) }

    startTimer()
  }

  fun restartCurrentGame() {
    val currentDifficulty = _uiState.value.difficulty
    startNewGame(currentDifficulty)
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (true) {
        delay(1000)
        if (!_uiState.value.isPaused && !_uiState.value.isGameWon && !_uiState.value.isGameOver) {
          _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
        }
      }
    }
  }

  fun togglePause() {
    _uiState.update { it.copy(isPaused = !it.isPaused) }
  }

  fun setPaused(paused: Boolean) {
    _uiState.update { it.copy(isPaused = paused) }
  }

  fun toggleTheme() {
    val newTheme = !_uiState.value.isDarkTheme
    prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
    _uiState.update { it.copy(isDarkTheme = newTheme) }
  }

  fun selectCell(row: Int, col: Int) {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    val currentSelected = _uiState.value.selectedCell
    val newSelected = if (currentSelected?.first == row && currentSelected?.second == col) {
      null
    } else {
      Pair(row, col)
    }

    _uiState.update { it.copy(selectedCell = newSelected, hintMessage = null) }

    // If a keypad number was locked/selected, try to apply it immediately!
    val lockedNumber = _uiState.value.selectedKeypadNumber
    if (newSelected != null && lockedNumber != null) {
      applyNumberToCell(newSelected.first, newSelected.second, lockedNumber)
    }
  }

  fun toggleNotesMode() {
    _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
  }

  fun selectKeypadNumber(number: Int) {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    val selectedCell = _uiState.value.selectedCell
    if (selectedCell != null) {
      // Cell-First: Cell is selected, apply this number to it!
      applyNumberToCell(selectedCell.first, selectedCell.second, number)
    } else {
      // Number-First: Toggle or select the number for quick filling
      val currentKey = _uiState.value.selectedKeypadNumber
      _uiState.update {
        it.copy(selectedKeypadNumber = if (currentKey == number) null else number)
      }
    }
  }

  private fun saveSnapshot() {
    val currentBoard = _uiState.value.board.map { row ->
      row.map { cell -> cell.copy(notes = cell.notes.toSet()) }
    }
    history.add(BoardSnapshot(currentBoard, _uiState.value.mistakesCount))
    if (history.size > 25) {
      history.removeAt(0)
    }
    _uiState.update { it.copy(canUndo = history.isNotEmpty()) }
  }

  fun undo() {
    if (history.isEmpty() || _uiState.value.isPaused || _uiState.value.isGameWon) return
    val lastSnapshot = history.removeAt(history.lastIndex)

    val remaining = calculateRemainingCounts(lastSnapshot.board)
    val conflicts = if (_uiState.value.settings.highlightDuplicates) {
      SudokuEngine.findConflictingCells(lastSnapshot.board)
    } else emptySet()

    val updatedBoard = lastSnapshot.board.mapIndexed { r, row ->
      row.mapIndexed { c, cell ->
        cell.copy(isError = conflicts.contains(Pair(r, c)))
      }
    }

    _uiState.update {
      it.copy(
        board = updatedBoard,
        mistakesCount = lastSnapshot.mistakesCount,
        canUndo = history.isNotEmpty(),
        numberCounts = remaining,
        hintMessage = null
      )
    }
  }

  fun eraseCell() {
    val selected = _uiState.value.selectedCell ?: return
    val cell = _uiState.value.board[selected.first][selected.second]
    if (cell.isGiven) return
    if (cell.value == null && cell.notes.isEmpty()) return

    saveSnapshot()

    val newBoard = _uiState.value.board.mapIndexed { r, row ->
      row.mapIndexed { c, currentCell ->
        if (r == selected.first && c == selected.second) {
          currentCell.copy(value = null, notes = emptySet(), isError = false)
        } else {
          currentCell
        }
      }
    }

    val conflicts = if (_uiState.value.settings.highlightDuplicates) {
      SudokuEngine.findConflictingCells(newBoard)
    } else emptySet()

    val refreshedBoard = newBoard.mapIndexed { r, row ->
      row.mapIndexed { c, cellItem ->
        cellItem.copy(isError = conflicts.contains(Pair(r, c)))
      }
    }

    _uiState.update {
      it.copy(
        board = refreshedBoard,
        numberCounts = calculateRemainingCounts(refreshedBoard),
        hintMessage = null
      )
    }
  }

  private fun applyNumberToCell(r: Int, c: Int, number: Int) {
    val currentCell = _uiState.value.board[r][c]
    if (currentCell.isGiven) return

    saveSnapshot()

    if (_uiState.value.isNotesMode) {
      // Ragu-ragu (Notes mode)
      val currentNotes = currentCell.notes.toMutableSet()
      if (currentNotes.contains(number)) {
        currentNotes.remove(number)
      } else {
        currentNotes.add(number)
      }

      val newBoard = _uiState.value.board.mapIndexed { rowIdx, rowList ->
        rowList.mapIndexed { colIdx, cell ->
          if (rowIdx == r && colIdx == c) {
            cell.copy(value = null, notes = currentNotes, isError = false)
          } else {
            cell
          }
        }
      }
      _uiState.update { it.copy(board = newBoard) }
    } else {
      // Normal confirmation
      val correctValue = _uiState.value.solution[r][c]
      val isCorrect = (number == correctValue)

      var newMistakes = _uiState.value.mistakesCount
      if (!isCorrect) {
        newMistakes++
      }

      val newBoard = _uiState.value.board.mapIndexed { rowIdx, rowList ->
        rowList.mapIndexed { colIdx, cell ->
          if (rowIdx == r && colIdx == c) {
            cell.copy(value = number, notes = emptySet(), isError = !isCorrect)
          } else if (_uiState.value.settings.autoRemoveNotes && isCorrect) {
            // Remove confirmed number from notes in same row, col, and 3x3 box
            val sameRow = (rowIdx == r)
            val sameCol = (colIdx == c)
            val sameBox = (rowIdx / 3 == r / 3 && colIdx / 3 == c / 3)
            if (sameRow || sameCol || sameBox) {
              cell.copy(notes = cell.notes - number)
            } else {
              cell
            }
          } else {
            cell
          }
        }
      }

      // Check conflicts
      val conflicts = if (_uiState.value.settings.highlightDuplicates) {
        SudokuEngine.findConflictingCells(newBoard)
      } else emptySet()

      val refreshedBoard = newBoard.mapIndexed { rowIdx, rowList ->
        rowList.mapIndexed { colIdx, cell ->
          cell.copy(isError = conflicts.contains(Pair(rowIdx, colIdx)) || (!isCorrect && rowIdx == r && colIdx == c))
        }
      }

      val remaining = calculateRemainingCounts(refreshedBoard)
      val won = SudokuEngine.isBoardSolved(refreshedBoard, _uiState.value.solution)
      val gameOver = _uiState.value.settings.mistakeLimitEnabled && newMistakes >= _uiState.value.settings.maxMistakes

      if (won) {
        handleGameWon()
      }

      _uiState.update {
        it.copy(
          board = refreshedBoard,
          mistakesCount = newMistakes,
          numberCounts = remaining,
          isGameWon = won,
          isGameOver = gameOver,
          showVictoryDialog = won,
          hintMessage = if (!isCorrect) "Angka $number kurang tepat di sini." else null
        )
      }
    }
  }

  private fun handleGameWon() {
    timerJob?.cancel()
    val wonCount = _uiState.value.stats.gamesWon + 1
    val currentDiff = _uiState.value.difficulty
    val currentTime = _uiState.value.timerSeconds

    val currentBest = _uiState.value.stats.bestTimes[currentDiff]
    val newBestTimes = _uiState.value.stats.bestTimes.toMutableMap()
    if (currentBest == null || currentTime < currentBest) {
      newBestTimes[currentDiff] = currentTime
      prefs.edit().putLong("best_time_${currentDiff.name}", currentTime).apply()
    }
    prefs.edit().putInt("games_won", wonCount).apply()

    _uiState.update {
      it.copy(
        stats = it.stats.copy(
          gamesWon = wonCount,
          bestTimes = newBestTimes
        )
      )
    }
  }

  /**
   * Fitur Bantuan (Hint):
   * Memberikan petunjuk untuk sel yang dipilih, atau secara otomatis menemukan sel dengan kemungkinan tunggal.
   */
  fun giveHint() {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    val selected = _uiState.value.selectedCell
    val targetCell: Pair<Int, Int> = if (selected != null && _uiState.value.board[selected.first][selected.second].value != _uiState.value.solution[selected.first][selected.second]) {
      selected
    } else {
      // Find the best empty cell
      var bestPair: Pair<Int, Int>? = null
      var minCandidates = 10

      for (r in 0 until 9) {
        for (c in 0 until 9) {
          if (_uiState.value.board[r][c].value == null) {
            val candidates = SudokuEngine.getCandidates(_uiState.value.board, r, c)
            if (candidates.size in 1 until minCandidates) {
              minCandidates = candidates.size
              bestPair = Pair(r, c)
            }
          }
        }
      }
      bestPair ?: return
    }

    val r = targetCell.first
    val c = targetCell.second
    val correctVal = _uiState.value.solution[r][c]

    saveSnapshot()

    val newBoard = _uiState.value.board.mapIndexed { rowIdx, rowList ->
      rowList.mapIndexed { colIdx, cell ->
        if (rowIdx == r && colIdx == c) {
          cell.copy(value = correctVal, notes = emptySet(), isError = false)
        } else if (_uiState.value.settings.autoRemoveNotes) {
          val sameRow = (rowIdx == r)
          val sameCol = (colIdx == c)
          val sameBox = (rowIdx / 3 == r / 3 && colIdx / 3 == c / 3)
          if (sameRow || sameCol || sameBox) cell.copy(notes = cell.notes - correctVal) else cell
        } else {
          cell
        }
      }
    }

    val remaining = calculateRemainingCounts(newBoard)
    val won = SudokuEngine.isBoardSolved(newBoard, _uiState.value.solution)

    if (won) {
      handleGameWon()
    }

    _uiState.update {
      it.copy(
        board = newBoard,
        selectedCell = targetCell,
        numberCounts = remaining,
        isGameWon = won,
        showVictoryDialog = won,
        hintMessage = "Bantuan: Mengisi angka $correctVal pada Baris ${r + 1}, Kolom ${c + 1}!"
      )
    }
  }

  /**
   * Fitur Bantuan Pemula:
   * Mengisi semua catatan ragu-ragu kandidat untuk sel kosong yang tersisa.
   */
  fun autoFillAllNotes() {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    saveSnapshot()

    val newBoard = _uiState.value.board.mapIndexed { r, row ->
      row.mapIndexed { c, cell ->
        if (cell.value == null) {
          val candidates = SudokuEngine.getCandidates(_uiState.value.board, r, c)
          cell.copy(notes = candidates)
        } else {
          cell
        }
      }
    }

    _uiState.update {
      it.copy(
        board = newBoard,
        hintMessage = "Semua catatan ragu-ragu otomatis terisi untuk membantu analisa!"
      )
    }
  }

  fun clearAllNotes() {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    saveSnapshot()

    val newBoard = _uiState.value.board.map { row ->
      row.map { cell -> cell.copy(notes = emptySet()) }
    }

    _uiState.update {
      it.copy(
        board = newBoard,
        hintMessage = "Catatan ragu-ragu telah dibersihkan."
      )
    }
  }

  fun updateSettings(newSettings: AssistanceSettings) {
    prefs.edit()
      .putBoolean("highlight_same", newSettings.highlightSameNumber)
      .putBoolean("highlight_lines", newSettings.highlightRowColBox)
      .putBoolean("highlight_dupes", newSettings.highlightDuplicates)
      .putBoolean("auto_remove_notes", newSettings.autoRemoveNotes)
      .putBoolean("mistake_limit", newSettings.mistakeLimitEnabled)
      .apply()

    _uiState.update { it.copy(settings = newSettings) }
  }

  fun showDifficultyDialog(show: Boolean) {
    _uiState.update { it.copy(showDifficultyDialog = show) }
  }

  fun showSettingsDialog(show: Boolean) {
    _uiState.update { it.copy(showSettingsDialog = show) }
  }

  fun showStatsDialog(show: Boolean) {
    _uiState.update { it.copy(showStatsDialog = show) }
  }

  fun showVictoryDialog(show: Boolean) {
    _uiState.update { it.copy(showVictoryDialog = show) }
  }

  fun showHowToPlayDialog(show: Boolean) {
    _uiState.update { it.copy(showHowToPlayDialog = show) }
  }

  fun dismissHintMessage() {
    _uiState.update { it.copy(hintMessage = null) }
  }

  private fun calculateRemainingCounts(board: List<List<CellData>>): Map<Int, Int> {
    val placedCounts = mutableMapOf<Int, Int>()
    (1..9).forEach { placedCounts[it] = 0 }

    for (row in board) {
      for (cell in row) {
        cell.value?.let { v ->
          if (v in 1..9) {
            placedCounts[v] = (placedCounts[v] ?: 0) + 1
          }
        }
      }
    }

    return (1..9).associateWith { num ->
      val count = placedCounts[num] ?: 0
      (9 - count).coerceAtLeast(0)
    }
  }
}
