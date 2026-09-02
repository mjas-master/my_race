package com.myrace.api.controller

import com.myrace.api.ApiException
import com.myrace.api.dto.*
import com.myrace.config.DeviceInterceptor
import com.myrace.config.MyRaceProperties
import com.myrace.domain.Device
import com.myrace.repository.DeviceRepository
import com.myrace.service.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

private fun HttpServletRequest.device(): Device = getAttribute(DeviceInterceptor.DEVICE_ATTR) as Device

@RestController
@RequestMapping("/v1/devices")
class DeviceController(private val devices: DeviceRepository, private val ent: EntitlementService, private val props: MyRaceProperties) {

    @PostMapping
    fun register(@RequestBody req: DeviceRequest): DeviceResponse {
        require(req.deviceId.isNotBlank()) { "deviceId 필수" }
        val d = devices.findByDeviceId(req.deviceId) ?: Device(deviceId = req.deviceId)
        d.platform = req.platform
        if (req.adultConfirmed && !d.adultConfirmed) d.adultConfirmedAt = Instant.now()
        d.adultConfirmed = req.adultConfirmed
        req.preferredMeet?.let { d.preferredMeet = it }
        req.pushToken?.let { d.pushToken = it }
        req.prefs?.let { d.prefs = it }
        return toResp(devices.save(d))
    }

    @GetMapping("/me")
    fun me(req: HttpServletRequest) = toResp(req.device())

    private fun toResp(d: Device) = DeviceResponse(d.deviceId, ent.isPremium(d.deviceId), ent.premiumUntil(d.deviceId), d.adultConfirmedAt,
        d.preferredMeet, d.prefs, props.flags.adsEnabled, props.prediction.freeRankLimit)
}

@RestController
@RequestMapping("/v1")
class RaceController(private val q: RaceQueryService, private val ent: EntitlementService) {

    @GetMapping("/races")
    fun list(@RequestParam(required = false) meet: Int?,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?): RaceListResponse =
        q.listRaces(meet, date ?: LocalDate.now())

    @GetMapping("/races/{id}")
    fun detail(@PathVariable id: Long, req: HttpServletRequest) = q.raceDetail(id, ent.isPremium(req.device().deviceId))

    @GetMapping("/races/{id}/entries")
    fun entries(@PathVariable id: Long, @RequestParam(defaultValue = "gate") order: String, req: HttpServletRequest) =
        q.raceDetail(id, ent.isPremium(req.device().deviceId), order)

    /** 과거조회용: 경주가 있는 날짜 목록(캘린더 표시). 기본 최근 1년. */
    @GetMapping("/races/dates")
    fun dates(@RequestParam(required = false) meet: Int?,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?): Map<String, Any> {
        val t = to ?: LocalDate.now().plusDays(7); val f = from ?: t.minusYears(1)
        return mapOf("meet" to (meet ?: 0), "from" to f, "to" to t, "dates" to q.raceDates(meet, f, t))
    }

    @GetMapping("/results")
    fun results(@RequestParam(required = false) meet: Int?,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?): ResultsResponse =
        q.results(meet, date ?: LocalDate.now().minusDays(1))
}

@RestController
@RequestMapping("/v1/hits")
class HitController(private val hits: HitEvaluationService, private val ent: EntitlementService, private val props: MyRaceProperties) {

    @GetMapping("/summary")
    fun summary(@RequestParam(required = false) meet: Int?, @RequestParam(defaultValue = "4") weeks: Int, req: HttpServletRequest): HitSummary =
        hits.summary(meet, weeks.coerceIn(1, 52), props.prediction.engine, ent.isPremium(req.device().deviceId))

    @GetMapping("/races")
    fun races(req: HttpServletRequest): Map<String, Any> {
        if (!ent.isPremium(req.device().deviceId)) throw ApiException(403, "PREMIUM_REQUIRED", "프리미엄 구독이 필요합니다.")
        return mapOf("items" to emptyList<Any>(), "note" to "A13: 경주별 상세 이력 구현 예정")
    }
}

@RestController
@RequestMapping("/v1")
class EntityController(private val q: EntityQueryService, private val ent: EntitlementService) {
    @GetMapping("/horses/{hrNo}") fun horse(@PathVariable hrNo: String, req: HttpServletRequest) = q.horse(hrNo, ent.isPremium(req.device().deviceId))
    @GetMapping("/jockeys/{jkNo}") fun jockey(@PathVariable jkNo: String) = q.jockey(jkNo)
    @GetMapping("/trainers/{trNo}") fun trainer(@PathVariable trNo: String) = q.trainer(trNo)
}

@RestController
@RequestMapping("/v1/subscriptions")
class SubscriptionController(private val ent: EntitlementService) {
    @PostMapping("/verify")
    fun verify(@RequestBody body: VerifyRequest, req: HttpServletRequest): VerifyResponse {
        val s = ent.registerPurchase(req.device().deviceId, body.platform, body.productId, body.purchaseToken)
        return VerifyResponse(s.active, s.expiresAt, s.productId)
    }
}

@RestController
@RequestMapping("/v1/admin")
class AdminController(
    private val ingest: IngestService, private val pred: PredictionService, private val hits: HitEvaluationService,
    private val kra: com.myrace.external.KraApiClient, private val props: MyRaceProperties,
) {
    private fun auth(key: String?) { if (key != props.adminKey) throw ApiException(403, "FORBIDDEN", "관리자 키가 올바르지 않습니다.") }

    @PostMapping("/ingest/entries")
    fun entries(@RequestParam meet: Int, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
                @RequestHeader("X-Admin-Key", required = false) key: String?): IngestReport {
        auth(key); val r = ingest.ingestEntries(meet, date); r.changedRaceIds.forEach { pred.computeForRace(it) }; return r
    }

    @PostMapping("/ingest/results")
    fun results(@RequestParam meet: Int, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
                @RequestHeader("X-Admin-Key", required = false) key: String?): Map<String, Any> {
        auth(key); val ids = ingest.ingestResults(meet, date); ids.forEach { hits.evaluateRace(it) }; return mapOf("finished" to ids.size)
    }

    /**
     * 과거 구간 일괄 적재(백테스트/과거조회용): from~to 의 금·토·일 마다
     * 출전(성적표 기반) 적재 → 결과일자 기준 예측(누출 없음) → 결과 반영 → 적중 평가.
     * 하루당 경마장별 1~2 API 호출이라 1년치도 개발계정 일일 한도(3,000) 내에서 가능.
     */
    @PostMapping("/backfill")
    fun backfill(@RequestParam(required = false) meet: Int?,
                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
                 @RequestHeader("X-Admin-Key", required = false) key: String?): Map<String, Any> {
        auth(key)
        val meets = meet?.let { listOf(it) } ?: props.ingest.meets
        var days = 0; var races = 0; var finished = 0; val errors = mutableListOf<String>()
        var d = from
        while (!d.isAfter(to)) {
            if (d.dayOfWeek in setOf(java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)) {
                meets.forEach { m ->
                    try {
                        val rep = ingest.ingestEntries(m, d)
                        if (rep.races > 0) {
                            races += rep.races; days++
                            pred.computeForDate(m, d)
                            val ids = ingest.ingestResults(m, d); ids.forEach { hits.evaluateRace(it) }; finished += ids.size
                        }
                    } catch (e: Exception) { errors += "$m/$d: ${e.message}" }
                }
            }
            d = d.plusDays(1)
        }
        return mapOf("meetDays" to days, "races" to races, "finished" to finished, "errors" to errors.take(20))
    }

    @PostMapping("/predict")
    fun predict(@RequestParam meet: Int, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
                @RequestHeader("X-Admin-Key", required = false) key: String?) = run { auth(key); mapOf("predicted" to pred.computeForDate(meet, date)) }

    @PostMapping("/evaluate")
    fun evaluate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
                 @RequestHeader("X-Admin-Key", required = false) key: String?) = run { auth(key); mapOf("evaluated" to hits.evaluateDate(date)) }

    @GetMapping("/health/upstream")
    fun upstream(@RequestHeader("X-Admin-Key", required = false) key: String?) = run { auth(key); mapOf("healthy" to kra.healthy(), "mock" to props.ingest.isMock(props.kra.serviceKey), "client" to kra.javaClass.simpleName) }

    /** 실키 넣은 뒤 첫 점검용: 각 KRA 엔드포인트 호출 → 응답 건수/필드명 확인 */
    @GetMapping("/kra/probe")
    fun probe(@RequestParam(defaultValue = "1") meet: Int,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
              @RequestHeader("X-Admin-Key", required = false) key: String?) = run { auth(key); kra.probe(meet, date ?: lastSunday()) }

    @PostMapping("/ingest/persons")
    fun persons(@RequestParam(defaultValue = "1") meet: Int, @RequestHeader("X-Admin-Key", required = false) key: String?) =
        run { auth(key); mapOf("updated" to ingest.refreshPersonStats(meet)) }

    private fun lastSunday(): LocalDate { var d = LocalDate.now(); while (d.dayOfWeek != java.time.DayOfWeek.SUNDAY) d = d.minusDays(1); return d }

    @GetMapping("/flags")
    fun flags(@RequestHeader("X-Admin-Key", required = false) key: String?) = run { auth(key); mapOf("adsEnabled" to props.flags.adsEnabled, "freeRankLimit" to props.prediction.freeRankLimit) }
}
