package com.blanksstudio.gridpix.util

import java.time.LocalDate

/** Local calendar dates as `yyyy-MM-dd`, the format the spec stores (SPEC section 5). */
object LocalDates {
    fun today(): String = LocalDate.now().toString()
    fun yesterday(): String = LocalDate.now().minusDays(1).toString()
    fun yesterdayOf(isoDate: String): String = LocalDate.parse(isoDate).minusDays(1).toString()
}

/** mm:ss, or h:mm:ss over an hour. Used by the puzzle timer, solved screen and stats. */
fun formatElapsed(ms: Long): Triple<Int, Int, Int> {
    val totalSeconds = (ms / 1000).toInt()
    return Triple(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
}
