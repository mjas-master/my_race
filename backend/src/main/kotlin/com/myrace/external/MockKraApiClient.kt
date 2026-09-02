package com.myrace.external

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * KRA 키 없이 개발할 수 있도록 결정적(seeded) 샘플 데이터를 생성.
 * - 경마장별 고정 마필/기수/조교사 풀
 * - 과거 날짜는 결과 포함, 미래 날짜는 출마표만
 * - 각 말에 숨은 "능력치"를 두어 예측 엔진이 유의미한 적중률을 낼 수 있게 함
 */
class MockKraApiClient : KraApiClient {

    private val horseNames = listOf(
        "천년의빛", "강남질주", "글로벌히트", "새벽바람", "파워블레이드", "라온더파이터", "모닝스타", "블루오션",
        "태양의질주", "바람의언덕", "하이랜드", "골든타임", "번개소리", "청룡의꿈", "은하수", "불꽃질주",
        "해피엔딩", "다이아몬드", "질풍노도", "마이웨이", "승리의노래", "용감한자", "폭풍의눈", "새벽별",
        "천하무적", "소리없는질주", "황금들녘", "무한도전", "행운의여신", "별빛바다",
    )
    private val jockeyNames = listOf("문세영", "서승운", "유현명", "김용근", "임기원", "박태종", "조인권", "김혜선", "장추열", "이혁")
    private val trainerNames = listOf("김영관", "박대흥", "지용철", "최기홍", "김순근", "신우철", "송문길", "안우성")
    private val grades = listOf("국6", "국5", "국4", "혼3", "혼2", "국1")
    private val distances = listOf(1000, 1200, 1300, 1400, 1700, 1800, 2000)

    private data class H(val hrNo: String, val name: String, val ability: Double, val sex: String, val age: Int, val rating: Int, val bestDist: Int)
    private data class J(val jkNo: String, val name: String, val skill: Double)
    private data class T(val trNo: String, val name: String, val skill: Double)

    private fun pool(meet: Int): Triple<List<H>, List<J>, List<T>> {
        val r = Random(meet * 7919)
        val horses = (0 until 30).map { i ->
            H("%d%06d".format(meet, 41000 + i), "${horseNames[i]}${if (meet == 1) "" else meet}",
                r.nextDouble(0.2, 1.0), if (r.nextBoolean()) "수" else "암", r.nextInt(3, 7), r.nextInt(20, 90), distances.random(r))
        }
        val jockeys = (0 until 10).map { i -> J("%02d%04d".format(meet + 7, i), jockeyNames[i], r.nextDouble(0.3, 1.0)) }
        val trainers = (0 until 8).map { i -> T("%02d%02d".format(meet, i), trainerNames[i], r.nextDouble(0.3, 1.0)) }
        return Triple(horses, jockeys, trainers)
    }

    private fun racesOn(meet: Int, date: LocalDate): Boolean = when (meet) {
        1 -> date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        3 -> date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        2 -> date.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        else -> false
    }

    private fun raceSeed(meet: Int, date: LocalDate, raceNo: Int) = Random(meet * 1_000_003L + date.toEpochDay() * 101 + raceNo)

    override fun fetchRacePlan(meet: Int, from: LocalDate, to: LocalDate): List<KraRacePlanDto> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            .filter { racesOn(meet, it) }
            .flatMap { d -> fetchEntries(meet, d).distinctBy { it.raceNo }.map { KraRacePlanDto(meet, d, it.raceNo, it.distance, it.track, it.grade, it.startTime) } }
            .toList()

    override fun fetchEntries(meet: Int, date: LocalDate): List<KraEntryDto> {
        if (!racesOn(meet, date)) return emptyList()
        val (horses, jockeys, trainers) = pool(meet)
        val out = mutableListOf<KraEntryDto>()
        for (raceNo in 1..6) {
            val r = raceSeed(meet, date, raceNo)
            val dist = distances[r.nextInt(distances.size)]
            val grade = grades[r.nextInt(grades.size)]
            val start = "%02d:%02d".format(10 + raceNo, listOf(10, 45, 20, 55, 30, 5)[raceNo - 1])
            val field = horses.shuffled(r).take(r.nextInt(8, 13))
            field.forEachIndexed { idx, h ->
                val jk = jockeys[r.nextInt(jockeys.size)]
                val tr = trainers[r.nextInt(trainers.size)]
                out += KraEntryDto(
                    meet, date, raceNo, dist, if (dist >= 1700 && r.nextInt(4) == 0) "잔디" else "모래", grade, start,
                    idx + 1, h.hrNo, h.name, "한국", h.sex, h.age, h.rating,
                    jk.jkNo, jk.name, tr.trNo, tr.name,
                    50.0 + r.nextInt(0, 8), 440.0 + r.nextInt(0, 80), scratched = r.nextInt(40) == 0,
                )
            }
        }
        return out
    }

    override fun fetchResults(meet: Int, date: LocalDate): List<KraResultDto> {
        if (!racesOn(meet, date) || !date.isBefore(LocalDate.now())) return emptyList()
        val (horses, jockeys, trainers) = pool(meet)
        val hMap = horses.associateBy { it.hrNo }; val jMap = jockeys.associateBy { it.jkNo }; val tMap = trainers.associateBy { it.trNo }
        val out = mutableListOf<KraResultDto>()
        fetchEntries(meet, date).groupBy { it.raceNo }.forEach { (raceNo, entries) ->
            val r = Random(raceSeed(meet, date, raceNo).nextLong() xor 0x5EEDL)
            val runners = entries.filter { !it.scratched }
            val ranked = runners.map { e ->
                val h = hMap.getValue(e.hrNo)
                val distFit = 1.0 - (kotlin.math.abs(h.bestDist - e.distance) / 1000.0)
                val perf = h.ability * 0.55 + jMap.getValue(e.jkNo).skill * 0.2 + tMap.getValue(e.trNo).skill * 0.1 +
                    distFit * 0.15 - (e.weightCarried - 50) * 0.01 + r.nextDouble(-0.25, 0.25)
                e to perf
            }.sortedByDescending { it.second }
            val base = e2ms(entries.first().distance)
            ranked.forEachIndexed { i, (e, _) ->
                val ms = base + i * r.nextInt(150, 450)
                val pos = i + 1
                out += KraResultDto(meet, date, raceNo, e.gateNo, e.hrNo, pos, msToTime(ms),
                    winOdds = if (pos == 1) round1(1.5 + r.nextDouble() * 12) else null,
                    placeOdds = if (pos <= 2) round1(1.2 + r.nextDouble() * 6) else null)
            }
        }
        return out
    }

    override fun fetchHorse(hrNo: String): KraHorseDto? {
        val meet = hrNo.first().digitToIntOrNull() ?: return null
        val h = pool(meet).first.firstOrNull { it.hrNo == hrNo } ?: return null
        return KraHorseDto(h.hrNo, h.name, "한국", h.sex, h.age, h.rating, 0, 0, 0, 0)
    }

    override fun healthy() = true

    private fun e2ms(dist: Int) = (dist / 1000.0 * 62_000).toInt()
    private fun msToTime(ms: Int) = "%d:%02d.%d".format(ms / 60000, (ms / 1000) % 60, (ms % 1000) / 100)
    private fun round1(d: Double) = Math.round(d * 10) / 10.0
}
