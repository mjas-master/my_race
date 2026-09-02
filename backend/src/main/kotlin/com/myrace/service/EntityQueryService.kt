package com.myrace.service

import com.myrace.api.ApiException
import com.myrace.api.dto.*
import com.myrace.domain.Entry
import com.myrace.domain.Meet
import com.myrace.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class EntityQueryService(
    private val horses: HorseRepository,
    private val jockeys: JockeyRepository,
    private val trainers: TrainerRepository,
    private val entries: EntryRepository,
    private val predictions: PredictionService,
) {
    fun horse(hrNo: String, premium: Boolean): HorseDetail {
        val h = horses.findByHrNo(hrNo) ?: throw ApiException(404, "HORSE_NOT_FOUND", "경주마를 찾을 수 없습니다.")
        val hist = entries.historyOfHorse(hrNo, LocalDate.now().plusDays(1))
        val limit = if (premium) 10 else 3
        val runs = hist.take(limit).map { e ->
            HorseRun(e.race.raceDate, e.race.meet, e.race.raceNo, e.race.distance, e.finishPos!!, fieldSize(e), e.finishTime, e.jockey.name, e.jockey.jkNo, e.winOdds)
        }
        val career = Career(hist.size, hist.count { it.finishPos == 1 }, hist.count { it.finishPos == 2 }, hist.count { it.finishPos == 3 })
        val byDist = hist.groupBy { it.race.distance }.map { (d, l) -> DistanceStat(d, l.size, l.count { it.finishPos == 1 }, l.count { it.finishPos == 2 }) }.sortedBy { it.distance }
        return HorseDetail(h.hrNo, h.name, h.country, h.sex, h.age, h.rating, career, byDist, runs, runsLocked = !premium, runsLimit = limit)
    }

    fun jockey(jkNo: String): PersonDetail {
        val j = jockeys.findByJkNo(jkNo) ?: throw ApiException(404, "JOCKEY_NOT_FOUND", "기수를 찾을 수 없습니다.")
        return person(j.jkNo, j.name, j.meet, j.debutYear,
            { f, t -> entries.runsOfJockey(jkNo, f, t) }, { f, t -> entries.upcomingOfJockey(jkNo, f, t) })
    }

    fun trainer(trNo: String): PersonDetail {
        val t = trainers.findByTrNo(trNo) ?: throw ApiException(404, "TRAINER_NOT_FOUND", "조교사를 찾을 수 없습니다.")
        return person(t.trNo, t.name, t.meet, null, { f, to -> entries.runsOfTrainer(trNo, f, to) }, { _, _ -> emptyList() })
    }

    private fun person(no: String, name: String, meet: Int, debut: Int?,
                       runs: (LocalDate, LocalDate) -> List<Entry>, upcoming: (LocalDate, LocalDate) -> List<Entry>): PersonDetail {
        val today = LocalDate.now()
        val season = runs(today.withDayOfYear(1), today)
        val last30 = season.filter { it.race.raceDate >= today.minusDays(30) }
        val weekly = runs(today.minusWeeks(12), today).groupBy { it.race.raceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .map { (w, l) -> WeekRate(w, l.size, rate(l) { it.finishPos == 1 }) }.sortedBy { it.weekStart }
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val up = upcoming(monday, monday.plusDays(6)).map { e ->
            val rank = predictions.latestFor(e.race.id!!)?.items?.firstOrNull { it.entry.id == e.id }?.rank
            UpcomingEntry(e.race.id!!, e.race.raceDate, e.race.raceNo, e.gateNo, e.horse.name, e.horse.hrNo, rank)
        }.sortedWith(compareBy({ it.date }, { it.raceNo }))
        return PersonDetail(no, name, meet, Meet.name(meet), debut, stat(season), stat(last30), weekly, up)
    }

    private fun stat(l: List<Entry>) = SeasonStat(l.size, l.count { it.finishPos == 1 }, rate(l) { it.finishPos == 1 }, rate(l) { (it.finishPos ?: 99) <= 2 })
    private fun rate(l: List<Entry>, f: (Entry) -> Boolean) = if (l.isEmpty()) 0.0 else Math.round(l.count(f).toDouble() / l.size * 1000) / 1000.0
    private fun fieldSize(e: Entry) = entries.findAllByRaceIdOrderByGateNoAsc(e.race.id!!).count { !it.scratched }
}
