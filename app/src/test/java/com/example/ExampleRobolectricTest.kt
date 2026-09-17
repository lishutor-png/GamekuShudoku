package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Difficulty
import com.example.viewmodel.SudokuViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ShuDoku", appName)
  }

  @Test
  fun `sudoku starts with 3 checks and does not penalize immediately on wrong input`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = SudokuViewModel(app)
    vm.startNewGame(Difficulty.MUDAH)

    assertEquals(3, vm.uiState.value.checksRemaining)
    assertEquals(0, vm.uiState.value.mistakesCount)

    // Find an empty cell
    val board = vm.uiState.value.board
    var emptyR = -1
    var emptyC = -1
    for (r in 0 until 9) {
      for (c in 0 until 9) {
        if (board[r][c].value == null) {
          emptyR = r
          emptyC = c
          break
        }
      }
      if (emptyR != -1) break
    }

    assertTrue(emptyR != -1)

    // Input a number into the empty cell
    val correctVal = vm.uiState.value.solution[emptyR][emptyC]
    val wrongVal = if (correctVal == 1) 2 else 1

    vm.selectCell(emptyR, emptyC)
    vm.selectKeypadNumber(wrongVal)

    // Verify: not marked as error immediately, and mistakes count did NOT increase
    assertFalse(vm.uiState.value.board[emptyR][emptyC].isError)
    assertEquals(0, vm.uiState.value.mistakesCount)

    // Now press Check
    vm.checkCurrentBoard()

    // Verify: checks remaining decremented from 3 to 2
    assertEquals(2, vm.uiState.value.checksRemaining)
    // The cell is now flagged as error
    assertTrue(vm.uiState.value.board[emptyR][emptyC].isError)
    // Mistake count updated
    assertEquals(1, vm.uiState.value.mistakesCount)
  }

  @Test
  fun `auto save restores game progress in a new viewmodel instance`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm1 = SudokuViewModel(app)
    vm1.startNewGame(Difficulty.MUDAH)

    // Select and fill a cell
    val board = vm1.uiState.value.board
    var emptyR = -1
    var emptyC = -1
    for (r in 0 until 9) {
      for (c in 0 until 9) {
        if (board[r][c].value == null) {
          emptyR = r
          emptyC = c
          break
        }
      }
      if (emptyR != -1) break
    }

    vm1.selectCell(emptyR, emptyC)
    vm1.selectKeypadNumber(7)
    vm1.saveGameProgress()

    // Initialize second viewmodel instance simulating app restart
    val vm2 = SudokuViewModel(app)
    val restoredBoard = vm2.uiState.value.board

    assertEquals(7, restoredBoard[emptyR][emptyC].value)
  }
}
