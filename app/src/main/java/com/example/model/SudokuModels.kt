package com.example.model

enum class Difficulty(val displayName: String, val clueCount: Int, val description: String) {
  MUDAH("Mudah", 42, "Cocok untuk pemula, banyak angka bantuan"),
  SEDANG("Sedang", 34, "Tantangan seimbang untuk latihan"),
  SULIT("Sulit", 28, "Memerlukan teknik eliminasi lanjutan"),
  AHLI("Ahli", 24, "Tantangan tertinggi bagi master Sudoku")
}

data class CellData(
  val row: Int,
  val col: Int,
  val value: Int? = null,
  val isGiven: Boolean = false,
  val notes: Set<Int> = emptySet(),
  val isError: Boolean = false
)

data class AssistanceSettings(
  val highlightSameNumber: Boolean = true,
  val highlightRowColBox: Boolean = true,
  val highlightDuplicates: Boolean = true,
  val autoRemoveNotes: Boolean = true,
  val mistakeLimitEnabled: Boolean = false, // Default false for friendly beginner experience
  val maxMistakes: Int = 3
)

data class GameStats(
  val gamesPlayed: Int = 0,
  val gamesWon: Int = 0,
  val bestTimes: Map<Difficulty, Long> = emptyMap()
)

data class BoardSnapshot(
  val board: List<List<CellData>>,
  val mistakesCount: Int
)
