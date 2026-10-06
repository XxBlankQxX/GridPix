package com.blanksstudio.gridpix.ui.puzzle

import androidx.annotation.StringRes
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.game.Puzzle
import com.blanksstudio.gridpix.game.Solution

/** SPEC section 3: three scripted 5x5 puzzles explaining clues, X marks and drag. */
data class TutorialStep(
    val step: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val puzzle: Puzzle,
)

object TutorialSteps {
    const val COUNT = 3

    private fun puzzle(step: Int, vararg rows: String): Puzzle =
        Puzzle.from("tutorial-$step", Solution.fromText(rows.toList()))

    val steps: List<TutorialStep> = listOf(
        // Full lines and "1 1": a ladder.
        TutorialStep(
            1, R.string.tutorial_step1_title, R.string.tutorial_step1_body,
            puzzle(1, "#####", "#...#", "#####", "#...#", "#####"),
        ),
        // Empty columns (clue 0) teach X marks: an arrow / signpost.
        TutorialStep(
            2, R.string.tutorial_step2_title, R.string.tutorial_step2_body,
            puzzle(2, ".###.", ".#.#.", ".###.", "..#..", "..#.."),
        ),
        // Full rows to drag across: a mushroom.
        TutorialStep(
            3, R.string.tutorial_step3_title, R.string.tutorial_step3_body,
            puzzle(3, "#####", "#####", "..#..", "..#..", "..#.."),
        ),
    )

    fun step(n: Int): TutorialStep? = steps.getOrNull(n - 1)
}
