package com.myrace.prediction

import com.myrace.config.MyRaceProperties
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min

/** 05_logic.md §2 의 규칙 기반 점수. 설명 가능한 7팩터 가중합. */
@Component
class RuleScoreEngineV1(props: MyRaceProperties) : ScoreEngine {

    private val w: Map<String, Double> = props.prediction.weights.ifEmpty {
        mapOf("recentForm" to .30, "distanceFit" to .20, "jockeyForm" to .15, "trainerForm" to .10,
            "weightChange" to .10, "restDays" to .10, "classMove" to .05)
    }

    override fun version() = "v1-rule"

    override fun score(ctx: RaceContext): List<ScoredEntry> {
        val scored = ctx.runners.map { r ->
            val f = linkedMapOf(
                "recentForm" to recentForm(r),
                "distanceFit" to distanceFit(r, ctx.distance),
                "jockeyForm" to jockeyForm(r),
                "trainerForm" to trainerForm(r),
                "weightChange" to weightChange(r),
                "restDays" to restDays(r, ctx),
                "classMove" to classMove(r),
            )
            val total = f.entries.sumOf { (k, v) -> (w[k] ?: 0.0) * v.v }
            ScoredEntry(r.entryId, r.gateNo, round1(total), f)
        }
        return scored.sortedWith(compareByDescending<ScoredEntry> { it.score }
            .thenByDescending { it.factors["recentForm"]!!.v }.thenBy { it.gateNo })
    }

    private fun recentForm(r: RunnerContext): Factor {
        val runs = r.recentRuns.take(3)
        if (runs.size < 2) return Factor(50.0, "출전 기록 부족(${runs.size}전)")
        val avgPos = runs.map { it.finishPos.toDouble() }.average()
        val avgRel = runs.map { it.finishPos.toDouble() / it.fieldSize }.average()   // 0~1, 작을수록 좋음
        val v = clamp((1.0 - avgRel) * 100)
        return Factor(round1(v), "최근 ${runs.size}전 평균 ${"%.1f".format(avgPos)}위")
    }

    private fun distanceFit(r: RunnerContext, dist: Int): Factor {
        val runs = r.recentRuns.filter { abs(it.distance - dist) <= 100 }
        if (runs.isEmpty()) return Factor(40.0, "${dist}m 부근 출전 없음")
        val pts = runs.sumOf { placePoints(it.finishPos) }
        val v = pts.toDouble() / (runs.size * 3) * 100
        val wins = runs.count { it.finishPos == 1 }; val p = runs.count { it.finishPos == 2 }
        return Factor(round1(v), "${dist}m ${runs.size}전 ${wins}승 ${p}준")
    }

    private fun jockeyForm(r: RunnerContext): Factor {
        val s = if (r.jockeyLast30.starts >= 5) r.jockeyLast30 else r.jockeySeason
        val v = min(100.0, s.winRate * 400)
        return Factor(round1(v), "기수 ${if (s === r.jockeyLast30) "최근 30일" else "시즌"} 승률 ${pct(s.winRate)}")
    }

    private fun trainerForm(r: RunnerContext): Factor {
        if (r.trainerSeason.starts == 0) return Factor(50.0, "조교사 기록 없음")
        return Factor(round1(min(100.0, r.trainerSeason.winRate * 500)), "조교사 시즌 승률 ${pct(r.trainerSeason.winRate)}")
    }

    private fun weightChange(r: RunnerContext): Factor {
        val prev = r.recentRuns.firstOrNull() ?: return Factor(50.0, "부담중량 비교 불가")
        val d = r.weightCarried - prev.weightCarried
        return Factor(round1(clamp(50 - d * 10)), "부담중량 ${if (d >= 0) "+" else ""}${"%.1f".format(d)}kg")
    }

    private fun restDays(r: RunnerContext, ctx: RaceContext): Factor {
        val last = r.recentRuns.firstOrNull() ?: return Factor(60.0, "휴양일 불명")
        val d = ChronoUnit.DAYS.between(last.date, ctx.raceDate).toInt()
        val v = when {
            d < 7 -> 40.0; d <= 13 -> 70.0; d <= 35 -> 100.0; d <= 90 -> 60.0; else -> 35.0
        }
        return Factor(v, "휴양 ${d}일")
    }

    private fun classMove(r: RunnerContext) = when (r.classMove) {
        -1 -> Factor(100.0, "등급 하향(상대 약화)")
        1 -> Factor(30.0, "등급 상향")
        else -> Factor(60.0, "등급 유지")
    }

    /** 1착 3점, 2착 2점, 3착 1점. 반환 타입을 Int 로 고정해 sumOf 오버로드 모호성을 피한다. */
    private fun placePoints(finishPos: Int): Int = when (finishPos) { 1 -> 3; 2 -> 2; 3 -> 1; else -> 0 }

    private fun clamp(v: Double) = v.coerceIn(0.0, 100.0)
    private fun round1(v: Double) = Math.round(v * 10) / 10.0
    private fun pct(v: Double) = "${Math.round(v * 1000) / 10.0}%"
}