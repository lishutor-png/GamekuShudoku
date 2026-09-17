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
import org.json.JSONArray
import org.json.JSONObject

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
  val checksRemaining: Int = 3, // Tombol Cek dibatasi 3 kali
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
    val restored = restoreSavedGame()
    if (!restored) {
      startNewGame(Difficulty.MUDAH)
    }
  }

  private fun loadSavedPreferences() {
    val isDark = prefs.getBoolean("is_dark_theme", true)
    val highlightSame = prefs.getBoolean("highlight_same", true)
    val highlightLines = prefs.getBoolean("highlight_lines", true)
    val highlightDupes = prefs.getBoolean("highlight_dupes", false)
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
        checksRemaining = 3,
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

    saveGameProgress()
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
          val newTime = _uiState.value.timerSeconds + 1
          _uiState.update { it.copy(timerSeconds = newTime) }
          // Auto-save timer periodically
          if (newTime % 5 == 0L) {
            prefs.edit().putLong("saved_timer", newTime).apply()
          }
        }
      }
    }
  }

  fun togglePause() {
    val newPause = !_uiState.value.isPaused
    _uiState.update { it.copy(isPaused = newPause) }
    saveGameProgress()
  }

  fun setPaused(paused: Boolean) {
    _uiState.update { it.copy(isPaused = paused) }
    saveGameProgress()
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
    saveGameProgress()
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
        if (r == selected.first && c == selected.second) {
          cellItem.copy(isError = false)
        } else {
          cellItem.copy(isError = cellItem.isError && conflicts.contains(Pair(r, c)))
        }
      }
    }

    _uiState.update {
      it.copy(
        board = refreshedBoard,
        numberCounts = calculateRemainingCounts(refreshedBoard),
        hintMessage = null
      )
    }
    saveGameProgress()
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
      saveGameProgress()
    } else {
      // Penempatan Angka Normal: Tidak langsung salah / dipotong penalti kesalahan
      val newBoard = _uiState.value.board.mapIndexed { rowIdx, rowList ->
        rowList.mapIndexed { colIdx, cell ->
          if (rowIdx == r && colIdx == c) {
            cell.copy(value = number, notes = emptySet(), isError = false)
          } else if (_uiState.value.settings.autoRemoveNotes) {
            // Hapus angka konfirmasi dari catatan pada baris, kolom, dan blok 3x3 yang sama
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

      // Periksa duplikat jika diaktifkan di Pengaturan
      val conflicts = if (_uiState.value.settings.highlightDuplicates) {
        SudokuEngine.findConflictingCells(newBoard)
      } else emptySet()

      val refreshedBoard = newBoard.mapIndexed { rowIdx, rowList ->
        rowList.mapIndexed { colIdx, cell ->
          if (rowIdx == r && colIdx == c) {
            cell.copy(isError = false)
          } else if (conflicts.contains(Pair(rowIdx, colIdx))) {
            cell.copy(isError = true)
          } else {
            cell
          }
        }
      }

      val remaining = calculateRemainingCounts(refreshedBoard)
      val won = SudokuEngine.isBoardSolved(refreshedBoard, _uiState.value.solution)

      if (won) {
        handleGameWon()
      }

      _uiState.update {
        it.copy(
          board = refreshedBoard,
          numberCounts = remaining,
          isGameWon = won,
          showVictoryDialog = won,
          hintMessage = null
        )
      }
      saveGameProgress()
    }
  }

  private fun handleGameWon() {
    timerJob?.cancel()
    clearSavedProgress()
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
    saveGameProgress()
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
    saveGameProgress()
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
    saveGameProgress()
  }

  /**
   * Fitur Tombol Cek:
   * Memeriksa angka yang diisi oleh pemain terhadap solusi.
   * Dibatasi maksimal 3 kali per permainan.
   * Tidak langsung memberikan penalti kesalahan saat mengisi, pemain bebas memeriksa saat diinginkan.
   */
  fun checkCurrentBoard() {
    if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return

    if (_uiState.value.checksRemaining <= 0) {
      _uiState.update { it.copy(hintMessage = "Batas cek (3 kali) pada permainan ini sudah habis.") }
      return
    }

    val currentBoard = _uiState.value.board
    val solution = _uiState.value.solution

    var filledCount = 0
    var wrongCount = 0

    for (r in 0 until 9) {
      for (c in 0 until 9) {
        val cell = currentBoard[r][c]
        if (!cell.isGiven && cell.value != null) {
          filledCount++
          if (cell.value != solution[r][c]) {
            wrongCount++
          }
        }
      }
    }

    if (filledCount == 0) {
      _uiState.update { it.copy(hintMessage = "Belum ada angka yang kamu isi untuk diperiksa.") }
      return
    }

    saveSnapshot()

    val newChecksRemaining = _uiState.value.checksRemaining - 1
    val newMistakes = _uiState.value.mistakesCount + wrongCount
    val gameOver = _uiState.value.settings.mistakeLimitEnabled && newMistakes >= _uiState.value.settings.maxMistakes

    val updatedBoard = currentBoard.mapIndexed { r, row ->
      row.mapIndexed { c, cell ->
        if (!cell.isGiven && cell.value != null) {
          val isWrong = (cell.value != solution[r][c])
          cell.copy(isError = isWrong)
        } else {
          cell
        }
      }
    }

    val msg = if (wrongCount == 0) {
      "Luar biasa! Semua angka yang kamu isi sejauh ini benar. (Sisa Cek: $newChecksRemaining)"
    } else {
      "Ditemukan $wrongCount angka keliru yang ditandai merah. (Sisa Cek: $newChecksRemaining)"
    }

    _uiState.update {
      it.copy(
        board = updatedBoard,
        checksRemaining = newChecksRemaining,
        mistakesCount = newMistakes,
        isGameOver = gameOver,
        hintMessage = msg
      )
    }

    saveGameProgress()
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

  /**
   * Simpan Otomatis Progres Permainan
   */
  fun saveGameProgress() {
    val state = _uiState.value
    if (state.isGameWon || state.isGameOver || state.board.isEmpty()) {
      clearSavedProgress()
      return
    }

    try {
      val boardJson = JSONArray()
      for (row in state.board) {
        val rowJson = JSONArray()
        for (cell in row) {
          val cellObj = JSONObject()
          cellObj.put("v", cell.value ?: 0)
          cellObj.put("g", cell.isGiven)
          cellObj.put("e", cell.isError)
          val notesJson = JSONArray()
          cell.notes.forEach { notesJson.put(it) }
          cellObj.put("n", notesJson)
          rowJson.put(cellObj)
        }
        boardJson.put(rowJson)
      }

      val solJson = JSONArray()
      for (row in state.solution) {
        val rowJson = JSONArray()
        for (num in row) {
          rowJson.put(num)
        }
        solJson.put(rowJson)
      }

      prefs.edit()
        .putBoolean("saved_has_game", true)
        .putString("saved_board", boardJson.toString())
        .putString("saved_solution", solJson.toString())
        .putString("saved_difficulty", state.difficulty.name)
        .putLong("saved_timer", state.timerSeconds)
        .putInt("saved_mistakes", state.mistakesCount)
        .putInt("saved_checks_remaining", state.checksRemaining)
        .apply()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Muat Kembali Progres Permainan yang Tersimpan
   */
  private fun restoreSavedGame(): Boolean {
    val hasSaved = prefs.getBoolean("saved_has_game", false)
    if (!hasSaved) return false

    return try {
      val boardStr = prefs.getString("saved_board", null) ?: return false
      val solStr = prefs.getString("saved_solution", null) ?: return false
      val diffName = prefs.getString("saved_difficulty", Difficulty.MUDAH.name)
      val difficulty = try {
        Difficulty.valueOf(diffName ?: Difficulty.MUDAH.name)
      } catch (_: Exception) {
        Difficulty.MUDAH
      }
      val timer = prefs.getLong("saved_timer", 0L)
      val mistakes = prefs.getInt("saved_mistakes", 0)
      val checks = prefs.getInt("saved_checks_remaining", 3)

      val solJson = JSONArray(solStr)
      val solution = mutableListOf<List<Int>>()
      for (r in 0 until 9) {
        val rowJson = solJson.getJSONArray(r)
        val rowList = mutableListOf<Int>()
        for (c in 0 until 9) {
          rowList.add(rowJson.getInt(c))
        }
        solution.add(rowList)
      }

      val boardJson = JSONArray(boardStr)
      val board = mutableListOf<List<CellData>>()
      for (r in 0 until 9) {
        val rowJson = boardJson.getJSONArray(r)
        val rowList = mutableListOf<CellData>()
        for (c in 0 until 9) {
          val cellObj = rowJson.getJSONObject(c)
          val v = cellObj.getInt("v")
          val g = cellObj.getBoolean("g")
          val e = cellObj.getBoolean("e")
          val nJson = cellObj.getJSONArray("n")
          val notes = mutableSetOf<Int>()
          for (i in 0 until nJson.length()) {
            notes.add(nJson.getInt(i))
          }
          rowList.add(
            CellData(
              row = r,
              col = c,
              value = if (v in 1..9) v else null,
              notes = notes,
              isGiven = g,
              isError = e
            )
          )
        }
        board.add(rowList)
      }

      val remainingCounts = calculateRemainingCounts(board)

      _uiState.update {
        it.copy(
          board = board,
          solution = solution,
          difficulty = difficulty,
          timerSeconds = timer,
          mistakesCount = mistakes,
          checksRemaining = checks,
          numberCounts = remainingCounts,
          selectedCell = null,
          selectedKeypadNumber = null,
          isNotesMode = false,
          isPaused = false,
          isGameWon = false,
          isGameOver = false,
          canUndo = false,
          hintMessage = "Melanjutkan progres game terakhir yang tersimpan otomatis."
        )
      }
      startTimer()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun clearSavedProgress() {
    prefs.edit().putBoolean("saved_has_game", false).apply()
  }

  override fun onCleared() {
    super.onCleared()
    saveGameProgress()
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
