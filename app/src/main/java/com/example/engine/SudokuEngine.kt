package com.example.engine

import com.example.model.CellData
import com.example.model.Difficulty
import kotlin.random.Random

object SudokuEngine {

  private const val GRID_SIZE = 9
  private const val BOX_SIZE = 3

  data class GeneratedPuzzle(
    val initialBoard: List<List<CellData>>,
    val solution: List<List<Int>>
  )

  /**
   * Generates a complete valid Sudoku grid and derives a puzzle based on difficulty.
   */
  fun generatePuzzle(difficulty: Difficulty): GeneratedPuzzle {
    val fullGrid = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
    fillFullGrid(fullGrid)

    val solution = List(GRID_SIZE) { r ->
      List(GRID_SIZE) { c -> fullGrid[r][c] }
    }

    // Carve puzzle
    val puzzleGrid = Array(GRID_SIZE) { r ->
      IntArray(GRID_SIZE) { c -> fullGrid[r][c] }
    }

    val totalCells = GRID_SIZE * GRID_SIZE
    val cellsToRemove = totalCells - difficulty.clueCount
    val positions = (0 until totalCells).shuffled()

    var removed = 0
    for (pos in positions) {
      if (removed >= cellsToRemove) break
      val r = pos / GRID_SIZE
      val c = pos % GRID_SIZE
      if (puzzleGrid[r][c] != 0) {
        puzzleGrid[r][c] = 0
        removed++
      }
    }

    val board = List(GRID_SIZE) { r ->
      List(GRID_SIZE) { c ->
        val v = puzzleGrid[r][c]
        if (v != 0) {
          CellData(row = r, col = c, value = v, isGiven = true)
        } else {
          CellData(row = r, col = c, value = null, isGiven = false)
        }
      }
    }

    return GeneratedPuzzle(initialBoard = board, solution = solution)
  }

  private fun fillFullGrid(grid: Array<IntArray>): Boolean {
    // Fill the 3 independent diagonal boxes first to randomize fast
    for (i in 0 until GRID_SIZE step BOX_SIZE) {
      fillBox(grid, i, i)
    }
    return solveGrid(grid, 0, 0)
  }

  private fun fillBox(grid: Array<IntArray>, rowStart: Int, colStart: Int) {
    val numbers = (1..9).shuffled()
    var idx = 0
    for (r in 0 until BOX_SIZE) {
      for (c in 0 until BOX_SIZE) {
        grid[rowStart + r][colStart + c] = numbers[idx++]
      }
    }
  }

  private fun solveGrid(grid: Array<IntArray>, row: Int, col: Int): Boolean {
    var r = row
    var c = col
    if (c == GRID_SIZE) {
      c = 0
      r++
      if (r == GRID_SIZE) return true
    }

    if (grid[r][c] != 0) {
      return solveGrid(grid, r, c + 1)
    }

    val numbers = (1..9).shuffled()
    for (num in numbers) {
      if (isValidPlacement(grid, r, c, num)) {
        grid[r][c] = num
        if (solveGrid(grid, r, c + 1)) return true
        grid[r][c] = 0
      }
    }
    return false
  }

  private fun isValidPlacement(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
    for (i in 0 until GRID_SIZE) {
      if (grid[row][i] == num) return false
      if (grid[i][col] == num) return false
    }

    val boxRowStart = (row / BOX_SIZE) * BOX_SIZE
    val boxColStart = (col / BOX_SIZE) * BOX_SIZE
    for (r in 0 until BOX_SIZE) {
      for (c in 0 until BOX_SIZE) {
        if (grid[boxRowStart + r][boxColStart + c] == num) return false
      }
    }
    return true
  }

  /**
   * Calculates all valid candidate numbers for a specific cell based on current board state.
   */
  fun getCandidates(board: List<List<CellData>>, row: Int, col: Int): Set<Int> {
    if (board[row][col].value != null) return emptySet()

    val used = mutableSetOf<Int>()

    // Check row and column
    for (i in 0 until GRID_SIZE) {
      board[row][i].value?.let { used.add(it) }
      board[i][col].value?.let { used.add(it) }
    }

    // Check 3x3 box
    val boxRow = (row / BOX_SIZE) * BOX_SIZE
    val boxCol = (col / BOX_SIZE) * BOX_SIZE
    for (r in 0 until BOX_SIZE) {
      for (c in 0 until BOX_SIZE) {
        board[boxRow + r][boxCol + c].value?.let { used.add(it) }
      }
    }

    return (1..9).toSet() - used
  }

  /**
   * Find conflicts (duplicate numbers in same row, column, or box).
   */
  fun findConflictingCells(board: List<List<CellData>>): Set<Pair<Int, Int>> {
    val conflicts = mutableSetOf<Pair<Int, Int>>()

    // Check rows
    for (r in 0 until GRID_SIZE) {
      val seen = mutableMapOf<Int, MutableList<Int>>()
      for (c in 0 until GRID_SIZE) {
        board[r][c].value?.let { seen.getOrPut(it) { mutableListOf() }.add(c) }
      }
      for ((_, cols) in seen) {
        if (cols.size > 1) {
          for (c in cols) conflicts.add(Pair(r, c))
        }
      }
    }

    // Check cols
    for (c in 0 until GRID_SIZE) {
      val seen = mutableMapOf<Int, MutableList<Int>>()
      for (r in 0 until GRID_SIZE) {
        board[r][c].value?.let { seen.getOrPut(it) { mutableListOf() }.add(r) }
      }
      for ((_, rows) in seen) {
        if (rows.size > 1) {
          for (r in rows) conflicts.add(Pair(r, c))
        }
      }
    }

    // Check 3x3 boxes
    for (br in 0 until GRID_SIZE step BOX_SIZE) {
      for (bc in 0 until GRID_SIZE step BOX_SIZE) {
        val seen = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
        for (r in 0 until BOX_SIZE) {
          for (c in 0 until BOX_SIZE) {
            val cellR = br + r
            val cellC = bc + c
            board[cellR][cellC].value?.let {
              seen.getOrPut(it) { mutableListOf() }.add(Pair(cellR, cellC))
            }
          }
        }
        for ((_, cells) in seen) {
          if (cells.size > 1) {
            conflicts.addAll(cells)
          }
        }
      }
    }

    return conflicts
  }

  /**
   * Check if all cells are filled and match the solution.
   */
  fun isBoardSolved(board: List<List<CellData>>, solution: List<List<Int>>): Boolean {
    for (r in 0 until GRID_SIZE) {
      for (c in 0 until GRID_SIZE) {
        val v = board[r][c].value ?: return false
        if (v != solution[r][c]) return false
      }
    }
    return true
  }
}
