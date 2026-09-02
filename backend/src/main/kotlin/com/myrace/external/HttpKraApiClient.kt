package com.myrace.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.myrace.config.MyRaceProperties
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 한국마사회 공공데이터(apis.data.go.kr/B551015) 클라이언트.
 *
 * 엔드포인트 경로는 application.yml `myrace.kra.endpoints` 에서 주입(포털 명세 변경 시 코드 수정 없이 대응).
 *
 *  raceResult (검증됨, API227 경주별상세성적표) : /racedetailresult/getracedetailresult?meet&rc_date[&rc_no]
 *      필드: rcDate rcNo stOrd chulNo hrNo hrName meet prdCtyNm sex age wgBudam jkNo jkSymbol jkName jkMeet trNo trName trMeet
 *            owNo owName differ wgHr df win plc rcTime hrRating hrTool chulYn(1출전/0미출전)
 *  racePlan   (미검증, 경주계획표 15056499)        : /API72_2/racePlan_2?meet&rc_date|rc_month|rc_year
 *  entrySheet (미검증, 출전표)                      : /API26_2/entrySheet_2?meet&rc_date
 *  horse      (미검증, 경주마 성적)                  : /API8_2/raceHorseInfo_2?hr_no
 *  jockey     (경로 미검증, 필드 명세 15056591)       : /API12_1/jockeyInfo_1?jk_no&meet
 *  trainer    (미검증)                              : /API19_1/trainerInfo_1?tr_no&meet
 *
 * 필드명은 후보 목록(alias)으로 조회하므로 명세 차이에 어느 정도 강건하다. 실키 투입 후 GET /v1/admin/kra/probe 로 검증.
 *
 * 전략: 출전표 API가 비어 있으면(권한 없음/명세 상이) 경주 당일 이후엔 성적표에서 출전 정보를 복원한다.
 *       성적표 하나만으로도 백테스트·적중 평가·경주마/기수 이력 전부 가능하다.
 */
class HttpKraApiClient(private val web: WebClient, private val props: MyRaceProperties) : KraApiClient {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ymd = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val om = ObjectMapper()

    private val defaults = mapOf(
        "raceResult" to "/racedetailresult/getracedetailresult",
        "racePlan" to "/API72_2/racePlan_2",
        "entrySheet" to "/API26_2/entrySheet_2",
        "horse" to "/API8_2/raceHorseInfo_2",
        "jockey" to "/API12_1/jockeyInfo_1",
        "trainer" to "/API19_1/trainerInfo_1",
    )
    private fun path(name: String) = props.kra.endpoints[name] ?: defaults.getValue(name)

    // ---------- HTTP ----------

    private fun uri(path: String, params: Map<String, String>, page: Int): URI {
        // serviceKey 는 포털 "Decoding" 키를 그대로 넣는다. 이미 %-인코딩된(Encoding) 키가 들어오면 이중 인코딩되므로 감지해 디코딩.
        val key = if (props.kra.serviceKey.contains("%")) java.net.URLDecoder.decode(props.kra.serviceKey, "UTF-8") else props.kra.serviceKey
        val b = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(props.kra.baseUrl).path(path)
            .queryParam("serviceKey", key).queryParam("_type", "json")
            .queryParam("numOfRows", props.kra.pageSize).queryParam("pageNo", page)
        params.forEach { (k, v) -> b.queryParam(k, v) }
        return b.build().encode().toUri()
    }

    private fun call(path: String, params: Map<String, String>, page: Int): JsonNode? =
        web.get().uri(uri(path, params, page)).retrieve()
            .bodyToMono<String>()
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(16)))
            .block()?.let { body ->
                // 게이트웨이 오류는 XML 로 오는 경우가 있음 → 메시지 추출 후 예외
                if (body.trimStart().startsWith("<")) {
                    val msg = Regex("<returnAuthMsg>(.*?)</returnAuthMsg>|<errMsg>(.*?)</errMsg>|<resultMsg>(.*?)</resultMsg>").find(body)
                        ?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() } ?: body.take(200)
                    throw KraApiException("KRA gateway error: $msg")
                }
                om.readTree(body)
            }

    /** 모든 페이지를 순회하여 item 목록 반환 */
    private fun items(name: String, params: Map<String, String>): List<JsonNode> {
        val out = mutableListOf<JsonNode>()
        var page = 1
        while (true) {
            val node = call(path(name), params, page) ?: break
            val header = node.path("response").path("header")
            val code = header.path("resultCode").asText("00")
            if (code != "00" && code != "0") {
                if (code == "03") break // NODATA_ERROR
                throw KraApiException("KRA $name resultCode=$code ${header.path("resultMsg").asText()}")
            }
            val body = node.path("response").path("body")
            val item = body.path("items").path("item")
            val list = when { item.isArray -> item.toList(); item.isObject -> listOf(item); else -> emptyList() }
            out += list
            val total = body.path("totalCount").asInt(out.size)
            if (list.isEmpty() || out.size >= total) break
            page++
        }
        return out
    }

    // ---------- 필드 헬퍼(별칭 허용) ----------

    private fun JsonNode.s(vararg keys: String): String {
        for (k in keys) { val v = path(k); if (!v.isMissingNode && !v.isNull) { val t = v.asText("").trim(); if (t.isNotEmpty()) return t } }
        return ""
    }
    private fun JsonNode.i(vararg keys: String): Int? = s(*keys).replace(Regex("[^0-9-]"), "").toIntOrNull()
    private fun JsonNode.d(vararg keys: String): Double? = s(*keys).replace(",", "").toDoubleOrNull()
    private fun date(s: String) = LocalDate.parse(s.replace("-", "").take(8), ymd)
    private fun meetCode(v: String, fallback: Int) = when (v) { "1", "서울" -> 1; "2", "제주" -> 2; "3", "부산경남", "부경", "부산" -> 3; "4", "영천" -> 4; else -> fallback }
    /** "11:10" / "1110" / "11시10분" → "HH:mm" */
    private fun time(v: String): String? {
        val dg = v.replace(Regex("[^0-9]"), ""); if (dg.length < 4) return null
        return "${dg.take(2)}:${dg.substring(2, 4)}"
    }
    private fun ageNum(v: String) = v.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    private fun country(v: String) = when { v.startsWith("한") -> "한국"; v.startsWith("미") -> "미국"; v.isBlank() -> "한국"; else -> v }

    // ---------- API ----------

    override fun fetchRacePlan(meet: Int, from: LocalDate, to: LocalDate): List<KraRacePlanDto> {
        val out = mutableListOf<KraRacePlanDto>()
        // 경주계획표는 일자/월 단위 조회. 월 단위로 순회.
        val months = generateSequence(from.withDayOfMonth(1)) { it.plusMonths(1) }.takeWhile { !it.isAfter(to) }
        months.forEach { m ->
            runCatching { items("racePlan", mapOf("meet" to "$meet", "rc_month" to m.format(DateTimeFormatter.ofPattern("yyyyMM")))) }
                .onFailure { log.warn("racePlan failed {}: {}", m, it.message) }
                .getOrDefault(emptyList()).forEach {
                    val rd = runCatching { date(it.s("rcDate", "rc_date")) }.getOrNull() ?: return@forEach
                    if (rd.isBefore(from) || rd.isAfter(to)) return@forEach
                    out += KraRacePlanDto(meet, rd, it.i("rcNo", "rc_no") ?: return@forEach, it.i("rcDist", "rc_dist") ?: 0,
                        it.s("track").ifBlank { "모래" }, it.s("rank", "rcRank", "grade"), time(it.s("schStTime", "stTime", "rcTime")))
                }
        }
        return out.distinctBy { Triple(it.meet, it.raceDate, it.raceNo) }
    }

    override fun fetchEntries(meet: Int, date: LocalDate): List<KraEntryDto> {
        val sheet = runCatching { items("entrySheet", mapOf("meet" to "$meet", "rc_date" to date.format(ymd))) }
            .onFailure { log.warn("entrySheet unavailable ({}), fallback to raceResult", it.message) }.getOrDefault(emptyList())
        val src = if (sheet.isNotEmpty()) sheet else items("raceResult", mapOf("meet" to "$meet", "rc_date" to date.format(ymd)))
        return src.mapNotNull { n ->
            val rcNo = n.i("rcNo", "rc_no") ?: return@mapNotNull null
            val gate = n.i("chulNo", "chul_no") ?: return@mapNotNull null
            val hrNo = n.s("hrNo", "hr_no"); if (hrNo.isBlank()) return@mapNotNull null
            KraEntryDto(
                meet = meet, raceDate = date, raceNo = rcNo,
                distance = n.i("rcDist", "rc_dist") ?: 0, track = n.s("track").ifBlank { "모래" },
                grade = n.s("rank", "rcRank", "grade"), startTime = time(n.s("schStTime", "stTime")),
                gateNo = gate, hrNo = hrNo, hrName = n.s("hrName", "hr_name"),
                hrCountry = country(n.s("prdCtyNm", "name", "country")), hrSex = n.s("sex"), hrAge = ageNum(n.s("age")),
                rating = n.i("hrRating", "rating") ?: 0,
                jkNo = n.s("jkNo", "jk_no"), jkName = n.s("jkName", "jk_name"), trNo = n.s("trNo", "tr_no"), trName = n.s("trName", "tr_name"),
                weightCarried = n.d("wgBudam", "wg_budam") ?: 0.0, horseWeight = n.d("wgHr", "wg_hr"),
                scratched = n.s("chulYn") == "0" || n.s("cancel", "cancelYn") == "Y",
            )
        }
    }

    override fun fetchResults(meet: Int, date: LocalDate): List<KraResultDto> =
        items("raceResult", mapOf("meet" to "$meet", "rc_date" to date.format(ymd))).mapNotNull { n ->
            val rcNo = n.i("rcNo") ?: return@mapNotNull null
            val gate = n.i("chulNo") ?: return@mapNotNull null
            val ran = n.s("chulYn") != "0"
            KraResultDto(
                meet, date, rcNo, gate, n.s("hrNo"),
                finishPos = if (ran) n.i("stOrd", "ord") else null,
                finishTime = n.s("rcTime").ifBlank { null },
                winOdds = n.d("win", "winOdds"), placeOdds = n.d("plc", "plcOdds"),
                horseWeight = n.d("wgHr"), horseWeightDiff = n.i("df", "wgHrDiff"), rating = n.i("hrRating"),
                margin = n.s("differ").ifBlank { null },
            )
        }

    override fun fetchHorse(hrNo: String): KraHorseDto? =
        runCatching { items("horse", mapOf("hr_no" to hrNo)) }.getOrDefault(emptyList()).firstOrNull()?.let {
            KraHorseDto(hrNo, it.s("hrName"), country(it.s("name", "prdCtyNm")), it.s("sex"), ageNum(it.s("age")), it.i("rating", "hrRating") ?: 0,
                it.i("rcCntT", "rcCnt") ?: 0, it.i("ord1CntT", "ord1Cnt") ?: 0, it.i("ord2CntT", "ord2Cnt") ?: 0, it.i("ord3CntT", "ord3Cnt") ?: 0)
        }

    private fun person(node: JsonNode, no: String, meet: Int, name: String) = KraPersonStatDto(
        no = no, name = name, meet = meet,
        careerStarts = node.i("rcCntT", "rcCnt", "totRcCnt") ?: 0, careerWins = node.i("ord1CntT", "ord1Cnt") ?: 0, careerSeconds = node.i("ord2CntT", "ord2Cnt") ?: 0,
        yearStarts = node.i("rcCntY", "rcCnt1y") ?: 0, yearWins = node.i("ord1CntY", "ord1Cnt1y") ?: 0,
        yearWinRate = node.d("winRateY", "winRate1y"), yearPlaceRate = node.d("qnlRateY", "plcRate1y"),
    )

    override fun fetchJockey(jkNo: String, meet: Int): KraPersonStatDto? =
        runCatching { items("jockey", mapOf("jk_no" to jkNo, "meet" to "$meet")) }.getOrDefault(emptyList()).firstOrNull()
            ?.let { person(it, jkNo, meet, it.s("jkName")) }

    override fun fetchTrainer(trNo: String, meet: Int): KraPersonStatDto? =
        runCatching { items("trainer", mapOf("tr_no" to trNo, "meet" to "$meet")) }.getOrDefault(emptyList()).firstOrNull()
            ?.let { person(it, trNo, meet, it.s("trName")) }

    override fun healthy(): Boolean = try {
        call(path("raceResult"), mapOf("meet" to "1", "rc_date" to LocalDate.now().format(ymd)), 1) != null
    } catch (e: Exception) { log.warn("KRA upstream unhealthy: {}", e.message); false }

    override fun probe(meet: Int, date: LocalDate): List<KraProbeResult> {
        val d = date.format(ymd)
        val tests = listOf(
            "raceResult" to mapOf("meet" to "$meet", "rc_date" to d),
            "racePlan" to mapOf("meet" to "$meet", "rc_date" to d),
            "entrySheet" to mapOf("meet" to "$meet", "rc_date" to d),
            "horse" to emptyMap(), "jockey" to mapOf("meet" to "$meet"), "trainer" to mapOf("meet" to "$meet"),
        )
        return tests.map { (name, params) ->
            try {
                val list = items(name, params)
                KraProbeResult(name, path(name), true, list.size, list.firstOrNull()?.fieldNames()?.asSequence()?.toList() ?: emptyList(), null)
            } catch (e: Exception) {
                KraProbeResult(name, path(name), false, 0, emptyList(), e.message)
            }
        }
    }
}

class KraApiException(msg: String) : RuntimeException(msg)
