package com.myrace.service

import com.myrace.domain.*
import com.myrace.external.KraApiClient
import com.myrace.external.KraEntryDto
import com.myrace.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class IngestReport(val meet: Int, val date: LocalDate, val races: Int, val entries: Int, val changedRaceIds: List<Long>)

@Service
class IngestService(
    private val kra: KraApiClient,
    private val races: RaceRepository,
    private val entries: EntryRepository,
    private val horses: HorseRepository,
    private val jockeys: JockeyRepository,
    private val trainers: TrainerRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 출마표 수집. 변경된(해시가 바뀐) 경주 id 목록을 리턴 → 호출자가 재예측. */
    @Transactional
    fun ingestEntries(meet: Int, date: LocalDate): IngestReport {
        val dtos = kra.fetchEntries(meet, date)
        val changed = mutableListOf<Long>()
        var entryCount = 0
        dtos.groupBy { it.raceNo }.forEach { (raceNo, list) ->
            val first = list.first()
            val race = races.findByMeetAndRaceDateAndRaceNo(meet, date, raceNo)
                ?: races.save(Race(meet = meet, raceDate = date, raceNo = raceNo, distance = first.distance))
            race.distance = first.distance; race.track = first.track; race.grade = first.grade
            race.startTime = first.startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

            val existing = entries.findAllByRaceIdOrderByGateNoAsc(race.id!!).associateBy { it.gateNo }
            list.forEach { d ->
                val e = existing[d.gateNo] ?: Entry(race = race, gateNo = d.gateNo, horse = upsertHorse(d), jockey = upsertJockey(d), trainer = upsertTrainer(d))
                e.horse = upsertHorse(d); e.jockey = upsertJockey(d); e.trainer = upsertTrainer(d)
                e.weightCarried = d.weightCarried; e.horseWeight = d.horseWeight; e.scratched = d.scratched
                entries.save(e); entryCount++
            }
            val hash = hashOf(list)
            if (race.entriesHash != hash) { race.entriesHash = hash; changed += race.id!! }
            if (race.status == RaceStatus.PLANNED) race.status = RaceStatus.ENTRIES_FIXED
            race.updatedAt = Instant.now()
            races.save(race)
        }
        log.info("entries ingested meet={} date={} races={} entries={} changed={}", meet, date, dtos.map { it.raceNo }.distinct().size, entryCount, changed.size)
        return IngestReport(meet, date, dtos.map { it.raceNo }.distinct().size, entryCount, changed)
    }

    /** 결과 수집. FINISHED 된 경주 id 리턴 → 호출자가 적중 평가. */
    @Transactional
    fun ingestResults(meet: Int, date: LocalDate): List<Long> {
        val results = kra.fetchResults(meet, date)
        if (results.isEmpty()) return emptyList()
        val finished = mutableListOf<Long>()
        results.groupBy { it.raceNo }.forEach { (raceNo, list) ->
            val race = races.findByMeetAndRaceDateAndRaceNo(meet, date, raceNo) ?: run {
                // 출마표 없이 결과만 온 경우(백테스트 적재): 출마표 먼저 수집
                ingestEntries(meet, date); races.findByMeetAndRaceDateAndRaceNo(meet, date, raceNo)
            } ?: return@forEach
            val byGate = entries.findAllByRaceIdOrderByGateNoAsc(race.id!!).associateBy { it.gateNo }
            list.forEach { r ->
                val e = byGate[r.gateNo] ?: return@forEach
                e.finishPos = r.finishPos; e.finishTime = r.finishTime; e.finishTimeMs = r.finishTime?.let(::parseMs)
                e.winOdds = r.winOdds; e.placeOdds = r.placeOdds
                r.horseWeight?.let { e.horseWeight = it }; e.horseWeightDiff = r.horseWeightDiff; e.rating = r.rating; e.margin = r.margin
                entries.save(e)
            }
            if (list.any { it.finishPos != null } && race.status != RaceStatus.FINISHED) {
                race.status = RaceStatus.FINISHED; race.updatedAt = Instant.now(); races.save(race); finished += race.id!!
            }
        }
        log.info("results ingested meet={} date={} finished={}", meet, date, finished.size)
        return finished
    }

    /** 기수/조교사 누적 성적 갱신(주 1회). 실API에서만 의미 있음. */
    @Transactional
    fun refreshPersonStats(meet: Int): Int {
        var n = 0
        jockeys.findAll().filter { it.meet == meet }.forEach { j ->
            kra.fetchJockey(j.jkNo, meet)?.let { d ->
                j.careerStarts = d.careerStarts; j.careerWins = d.careerWins; j.careerSeconds = d.careerSeconds
                j.yearStarts = d.yearStarts; j.yearWins = d.yearWins; jockeys.save(j); n++
            }
        }
        trainers.findAll().filter { it.meet == meet }.forEach { t ->
            kra.fetchTrainer(t.trNo, meet)?.let { d ->
                t.careerStarts = d.careerStarts; t.careerWins = d.careerWins; t.careerSeconds = d.careerSeconds
                t.yearStarts = d.yearStarts; t.yearWins = d.yearWins; trainers.save(t); n++
            }
        }
        return n
    }

    @Transactional
    fun ingestPlan(meet: Int, from: LocalDate, to: LocalDate): Int {
        var n = 0
        kra.fetchRacePlan(meet, from, to).forEach { p ->
            if (races.findByMeetAndRaceDateAndRaceNo(meet, p.raceDate, p.raceNo) == null) {
                races.save(Race(meet = meet, raceDate = p.raceDate, raceNo = p.raceNo, distance = p.distance, track = p.track, grade = p.grade,
                    startTime = p.startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() })); n++
            }
        }
        return n
    }

    private fun upsertHorse(d: KraEntryDto): Horse = horses.findByHrNo(d.hrNo)?.also {
        it.name = d.hrName; it.age = d.hrAge; it.rating = d.rating; it.sex = d.hrSex
    } ?: horses.save(Horse(hrNo = d.hrNo, name = d.hrName, country = d.hrCountry, sex = d.hrSex, age = d.hrAge, rating = d.rating))

    private fun upsertJockey(d: KraEntryDto): Jockey = jockeys.findByJkNo(d.jkNo)
        ?: jockeys.save(Jockey(jkNo = d.jkNo, name = d.jkName, meet = d.meet))

    private fun upsertTrainer(d: KraEntryDto): Trainer = trainers.findByTrNo(d.trNo)
        ?: trainers.save(Trainer(trNo = d.trNo, name = d.trName, meet = d.meet))

    private fun hashOf(list: List<KraEntryDto>): String {
        val s = list.sortedBy { it.gateNo }.joinToString("|") { "${it.gateNo}:${it.hrNo}:${it.jkNo}:${it.weightCarried}:${it.scratched}" }
        return MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun parseMs(t: String): Int? = runCatching {
        val (m, rest) = t.split(":"); val (s, d) = rest.split(".")
        m.toInt() * 60000 + s.toInt() * 1000 + d.padEnd(3, '0').take(3).toInt()
    }.getOrNull()
}
