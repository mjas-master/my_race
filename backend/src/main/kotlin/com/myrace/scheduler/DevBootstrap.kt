package com.myrace.scheduler

import com.myrace.config.MyRaceProperties
import com.myrace.domain.Device
import com.myrace.repository.DeviceRepository
import com.myrace.service.HitEvaluationService
import com.myrace.service.IngestService
import com.myrace.service.PredictionService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDate

/**
 * mock 모드에서 기동 시 과거 8주 결과 + 향후 1주 출마표를 적재하고 예측·적중 평가까지 수행.
 * 개발용 디바이스 "dev-device" 를 성인확인 완료 상태로 등록.
 */
@Configuration
class DevBootstrap {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun seed(ingest: IngestService, pred: PredictionService, hits: HitEvaluationService, devices: DeviceRepository, props: MyRaceProperties) = ApplicationRunner {
        // 개발 디바이스는 항상 등록, 샘플 데이터 시드는 mock 모드에서만
        if (devices.findByDeviceId("dev-device") == null)
            devices.save(Device(deviceId = "dev-device", adultConfirmed = true, adultConfirmedAt = Instant.now()))
        if (!props.ingest.isMock(props.kra.serviceKey)) {
            val weeks = props.ingest.backfillWeeksOnStart
            if (weeks <= 0) { log.info("real KRA mode: backfill disabled"); return@ApplicationRunner }
            // 실API: 별도 스레드에서 과거 N주 + 향후 7일 적재(기동 지연 방지). 실패는 로그만.
            Thread {
                val today = LocalDate.now(); var d = today.minusWeeks(weeks.toLong())
                log.info("real KRA mode: backfill {} weeks start", weeks)
                while (!d.isAfter(today.plusDays(7))) {
                    props.ingest.meets.forEach { meet ->
                        try {
                            val rep = ingest.ingestEntries(meet, d)
                            if (rep.races > 0) {
                                pred.computeForDate(meet, d)
                                if (d.isBefore(today)) ingest.ingestResults(meet, d).forEach { hits.evaluateRace(it) }
                            }
                        } catch (e: Exception) { log.warn("backfill {}/{} failed: {}", meet, d, e.message) }
                    }
                    d = d.plusDays(1)
                }
                props.ingest.meets.forEach { runCatching { ingest.refreshPersonStats(it) } }
                log.info("real KRA mode: backfill done")
            }.apply { isDaemon = true; name = "kra-backfill" }.start()
            return@ApplicationRunner
        }
        val today = LocalDate.now()
        val start = today.minusWeeks(8)
        var d = start
        while (!d.isAfter(today.plusDays(7))) {
            props.ingest.meets.forEach { meet ->
                val rep = ingest.ingestEntries(meet, d)
                if (rep.races > 0) {
                    pred.computeForDate(meet, d)
                    if (d.isBefore(today)) ingest.ingestResults(meet, d).forEach { hits.evaluateRace(it) }
                }
            }
            d = d.plusDays(1)
        }
        if (devices.findByDeviceId("dev-device") == null)
            devices.save(Device(deviceId = "dev-device", adultConfirmed = true, adultConfirmedAt = Instant.now()))
        val s = hits.summary(null, 8, props.prediction.engine, true)
        log.info("[mock seed] races={} win={} place={} show={}", s.races, s.winRate, s.placeRate, s.showRate)
    }
}
