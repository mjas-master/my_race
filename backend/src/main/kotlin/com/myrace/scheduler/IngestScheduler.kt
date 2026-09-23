package com.myrace.scheduler

import com.myrace.config.MyRaceProperties
import com.myrace.service.HitEvaluationService
import com.myrace.service.IngestService
import com.myrace.service.PredictionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/** 05_logic.md §1.1 크론. 모두 Asia/Seoul. */
@Component
class IngestScheduler(
    private val ingest: IngestService,
    private val pred: PredictionService,
    private val hits: HitEvaluationService,
    private val props: MyRaceProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private fun today() = LocalDate.now(ZoneId.of(props.timezone))

    /** 경주 계획: 월 06:00 — 이번 주·다음 주 */
    @Scheduled(cron = "0 0 6 * * MON", zone = "Asia/Seoul")
    fun planJob() = safe("plan") {
        val d = today()
        props.ingest.meets.forEach { ingest.ingestPlan(it, d, d.plusDays(13)) }
    }

    /** 출마표: 화·수·목 12:00/18:00, 금·토·일 07:00 → 변경 경주 재예측 */
    @Scheduled(cron = "0 0 12,18 * * TUE-THU", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 7 * * FRI-SUN", zone = "Asia/Seoul")
    fun entriesJob() = safe("entries") {
        val d = today()
        (0..6).map { d.plusDays(it.toLong()) }.forEach { date ->
            props.ingest.meets.forEach { meet -> ingest.ingestEntries(meet, date).changedRaceIds.forEach { pred.computeForRace(it) } }
        }
    }

    /** 결과: 금·토·일 10:30~19:00 매 10분 → 적중 평가 */
    @Scheduled(cron = "0 */10 10-19 * * FRI-SUN", zone = "Asia/Seoul")
    fun resultsJob() = safe("results") {
        val d = today()
        props.ingest.meets.forEach { meet -> ingest.ingestResults(meet, d).forEach { hits.evaluateRace(it) } }
    }

    /** 기수·조교사 누적 성적 갱신: 매주 월 06:30 */
    @Scheduled(cron = "0 30 6 * * MON", zone = "Asia/Seoul")
    fun personStatsJob() = safe("personStats") { props.ingest.meets.forEach { ingest.refreshPersonStats(it) } }

    private fun safe(name: String, block: () -> Unit) = try { block() } catch (e: Exception) { log.error("job {} failed: {}", name, e.message, e) }

    /** 매일 11:50 최신 경주정보 통합 다운로드 및 처리 */
    @Scheduled(cron = "0 50 11 * * *", zone = "Asia/Seoul")
    fun dailySyncJob() = safe("dailySync") {
        val d = today()
        props.ingest.meets.forEach { meet ->
            ingest.ingestPlan(meet, d, d.plusDays(13))
            (0..6).map { d.plusDays(it.toLong()) }.forEach { date ->
                ingest.ingestEntries(meet, date).changedRaceIds.forEach { pred.computeForRace(it) }
            }
            ingest.ingestResults(meet, d.minusDays(1)).forEach { hits.evaluateRace(it) }
            ingest.ingestResults(meet, d).forEach { hits.evaluateRace(it) }
        }
    }
}
