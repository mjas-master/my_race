package com.myrace.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.myrace.domain.*
import com.myrace.prediction.*
import com.myrace.repository.EntryRepository
import com.myrace.repository.PredictionRepository
import com.myrace.repository.RaceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PredictionService(
    private val engine: ScoreEngine,
    private val races: RaceRepository,
    private val entries: EntryRepository,
    private val predictions: PredictionRepository,
    private val om: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 경주 하나 예측. asOf = 경주일(백테스트 시 시간 누수 방지: 경주일 이전 데이터만 사용). */
    @Transactional
    fun computeForRace(raceId: Long): Prediction? {
        val race = races.findById(raceId).orElse(null) ?: return null
        val runners = entries.findAllByRaceIdOrderByGateNoAsc(raceId).filter { !it.scratched }
        if (runners.size < 2) return null
        val asOf = race.raceDate
        val ctx = RaceContext(raceId, asOf, race.distance, runners.map { toRunnerContext(it, race, asOf) })
        val scored = engine.score(ctx)
        val byId = runners.associateBy { it.id!! }

        val p = Prediction(race = race, engineVersion = engine.version(),
            inputSnapshotJson = om.writeValueAsString(mapOf("asOf" to asOf.toString(), "runners" to runners.map { it.id })))
        scored.forEachIndexed { i, s ->
            p.items += PredictionItem(prediction = p, entry = byId.getValue(s.entryId), rank = i + 1, score = s.score, factorsJson = om.writeValueAsString(s.factors))
        }
        return predictions.save(p)
    }

    @Transactional
    fun computeForDate(meet: Int, date: LocalDate): Int =
        races.findAllByMeetAndRaceDateAndStatus(meet, date, RaceStatus.ENTRIES_FIXED).count { computeForRace(it.id!!) != null }
            .also { log.info("predicted meet={} date={} races={}", meet, date, it) }

    fun latestFor(raceId: Long): Prediction? = predictions.findFirstByRaceIdAndEngineVersionOrderByComputedAtDesc(raceId, engine.version())
        ?: predictions.findFirstByRaceIdOrderByComputedAtDesc(raceId)

    fun parseFactors(json: String): Map<String, Factor> =
        om.readValue(json, om.typeFactory.constructMapType(LinkedHashMap::class.java, String::class.java, Factor::class.java))

    // ---- 컨텍스트 조립 (모두 asOf 이전 데이터만) ----
    private fun toRunnerContext(e: Entry, race: Race, asOf: LocalDate): RunnerContext {
        val hist = entries.historyOfHorse(e.horse.hrNo, asOf).take(10)
        val recent = hist.map { h -> PastRun(h.race.raceDate, h.finishPos!!, fieldOf(h.race), h.race.distance, h.weightCarried) }
        val jk30 = stat(entries.runsOfJockey(e.jockey.jkNo, asOf.minusDays(30), asOf.minusDays(1)))
        val jkSeason = stat(entries.runsOfJockey(e.jockey.jkNo, asOf.withDayOfYear(1), asOf.minusDays(1)))
        val trSeason = stat(entries.runsOfTrainer(e.trainer.trNo, asOf.withDayOfYear(1), asOf.minusDays(1)))
        val classMove = hist.firstOrNull()?.let { gradeRank(race.grade).compareTo(gradeRank(it.race.grade)).coerceIn(-1, 1) } ?: 0
        return RunnerContext(e.id!!, e.gateNo, e.weightCarried, recent, jk30, jkSeason, trSeason, classMove)
    }

    private fun fieldOf(r: Race) = entries.findAllByRaceIdOrderByGateNoAsc(r.id!!).count { !it.scratched }.coerceAtLeast(2)
    private fun stat(runs: List<Entry>) = Stat(runs.size, runs.count { it.finishPos == 1 })
    /** 등급 숫자 작을수록 상위. "국6"→6, "혼2"→2. 승급 = rank 감소 → +1 */
    private fun gradeRank(g: String) = -(g.filter { it.isDigit() }.toIntOrNull() ?: 6)
}
