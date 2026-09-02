package com.myrace.service

import com.myrace.domain.HitRecord
import com.myrace.domain.Meet
import com.myrace.domain.RaceStatus
import com.myrace.repository.EntryRepository
import com.myrace.repository.HitRecordRepository
import com.myrace.repository.RaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HitFlags(val winHit: Boolean, val placeHit: Boolean, val showHit: Boolean)
data class MeetHit(val meet: Int, val meetName: String, val races: Int, val winRate: Double)
data class WeekHit(val weekStart: LocalDate, val races: Int, val winRate: Double)
data class HitSummary(
    val from: LocalDate, val to: LocalDate, val engineVersion: String, val races: Int,
    val winRate: Double, val placeRate: Double, val showRate: Double,
    val byMeet: List<MeetHit>, val weekly: List<WeekHit>, val detailLocked: Boolean,
)

@Service
class HitEvaluationService(
    private val races: RaceRepository,
    private val entries: EntryRepository,
    private val predictions: PredictionService,
    private val hits: HitRecordRepository,
) {
    @Transactional
    fun evaluateRace(raceId: Long): HitRecord? {
        val race = races.findById(raceId).orElse(null) ?: return null
        if (race.status != RaceStatus.FINISHED) return null
        val p = predictions.latestFor(raceId) ?: return null
        if (hits.existsByPredictionId(p.id!!)) return null
        val actual = entries.findAllByRaceIdOrderByGateNoAsc(raceId).filter { it.finishPos != null }.sortedBy { it.finishPos }
        val a = actual.take(3).map { it.gateNo }
        if (a.isEmpty()) return null
        val picks = p.items.sortedBy { it.rank }
        val p1 = picks.first().entry
        val rec = if (p1.scratched) HitRecord(prediction = p, meet = race.meet, raceDate = race.raceDate, engineVersion = p.engineVersion,
            winHit = false, placeHit = false, showHit = false, top3Exact = false, excluded = true, reason = "SCRATCHED")
        else HitRecord(prediction = p, meet = race.meet, raceDate = race.raceDate, engineVersion = p.engineVersion,
            winHit = p1.gateNo == a[0], placeHit = p1.gateNo in a.take(2), showHit = p1.gateNo in a,
            top3Exact = picks.take(3).map { it.entry.gateNo }.toSet() == a.toSet())
        return hits.save(rec)
    }

    @Transactional
    fun evaluateDate(date: LocalDate): Int =
        races.findAllByRaceDateBetweenAndStatus(date, date, RaceStatus.FINISHED).count { evaluateRace(it.id!!) != null }

    fun flagsFor(raceId: Long): HitFlags? {
        val p = predictions.latestFor(raceId) ?: return null
        val actual = entries.findAllByRaceIdOrderByGateNoAsc(raceId).filter { it.finishPos != null }.sortedBy { it.finishPos }.map { it.gateNo }
        if (actual.isEmpty()) return null
        val g = p.items.minBy { it.rank }.entry.gateNo
        return HitFlags(g == actual[0], g in actual.take(2), g in actual.take(3))
    }

    fun summary(meet: Int?, weeks: Int, engineVersion: String, premium: Boolean): HitSummary {
        val to = LocalDate.now()
        val from = to.minusWeeks(weeks.toLong()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val all = (if (meet == null) hits.findAllByRaceDateBetweenAndExcludedFalse(from, to)
                   else hits.findAllByMeetAndRaceDateBetweenAndExcludedFalse(meet, from, to))
            .filter { it.engineVersion == engineVersion }
        fun rate(l: List<HitRecord>, f: (HitRecord) -> Boolean) = if (l.isEmpty()) 0.0 else Math.round(l.count(f).toDouble() / l.size * 1000) / 1000.0
        return HitSummary(from, to, engineVersion, all.size,
            rate(all) { it.winHit }, rate(all) { it.placeHit }, rate(all) { it.showHit },
            all.groupBy { it.meet }.map { (m, l) -> MeetHit(m, Meet.name(m), l.size, rate(l) { it.winHit }) }.sortedBy { it.meet },
            all.groupBy { it.raceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
                .map { (w, l) -> WeekHit(w, l.size, rate(l) { it.winHit }) }.sortedBy { it.weekStart },
            detailLocked = !premium)
    }
}
