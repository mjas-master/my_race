package com.myrace.prediction

import com.myrace.config.MyRaceProperties
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleScoreEngineV1Test {
    private val engine = RuleScoreEngineV1(MyRaceProperties())
    private val day = LocalDate.of(2026, 9, 4)

    private fun runner(id: Long, gate: Int, positions: List<Int>, jkWins: Int = 2) = RunnerContext(
        entryId = id, gateNo = gate, weightCarried = 55.0,
        recentRuns = positions.mapIndexed { i, p -> PastRun(day.minusDays(21L * (i + 1)), p, 10, 1200, 55.0) },
        jockeyLast30 = Stat(10, jkWins), jockeySeason = Stat(100, 15), trainerSeason = Stat(80, 10), classMove = 0,
    )

    @Test
    fun `better recent form ranks higher`() {
        val ctx = RaceContext(1, day, 1200, listOf(runner(1, 1, listOf(6, 7, 5)), runner(2, 2, listOf(1, 2, 1))))
        val out = engine.score(ctx)
        assertEquals(2L, out.first().entryId)
        assertTrue(out.first().score > out.last().score)
    }

    @Test
    fun `scores are bounded and factors explained`() {
        val out = engine.score(RaceContext(1, day, 1200, listOf(runner(1, 1, listOf(1, 1, 1), jkWins = 9))))
        assertTrue(out.first().score in 0.0..100.0)
        assertEquals(7, out.first().factors.size)
        assertTrue(out.first().factors.values.all { it.text.isNotBlank() })
    }
}
