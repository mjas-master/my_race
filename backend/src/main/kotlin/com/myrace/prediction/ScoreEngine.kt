package com.myrace.prediction

import java.time.LocalDate

/** 팩터 계산에 필요한, 경주 전 시점(asOf) 기준 입력. 시간 누수 방지를 위해 서비스가 채워서 넘긴다. */
data class RunnerContext(
    val entryId: Long,
    val gateNo: Int,
    val weightCarried: Double,
    /** 최근 출전 기록(최신순). finishPos, fieldSize, distance, weightCarried, date */
    val recentRuns: List<PastRun>,
    val jockeyLast30: Stat,
    val jockeySeason: Stat,
    val trainerSeason: Stat,
    /** 이전 출전 등급 vs 이번 등급: -1 강등(쉬움), 0 유지, +1 승급 */
    val classMove: Int,
)

data class PastRun(val date: LocalDate, val finishPos: Int, val fieldSize: Int, val distance: Int, val weightCarried: Double)
data class Stat(val starts: Int, val wins: Int) { val winRate get() = if (starts == 0) 0.0 else wins.toDouble() / starts }

data class RaceContext(val raceId: Long, val raceDate: LocalDate, val distance: Int, val runners: List<RunnerContext>)

data class Factor(val v: Double, val text: String)
data class ScoredEntry(val entryId: Long, val gateNo: Int, val score: Double, val factors: Map<String, Factor>)

interface ScoreEngine {
    fun version(): String
    fun score(ctx: RaceContext): List<ScoredEntry>
}
