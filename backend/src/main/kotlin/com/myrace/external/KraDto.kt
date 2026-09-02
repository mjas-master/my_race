package com.myrace.external

import java.time.LocalDate

/**
 * KRA 공공데이터 응답을 서비스 계층이 소비하는 정규화 DTO.
 * 원본 JSON 필드명(rcDate, rcNo, chulNo, hrNo, hrName, jkNo, jkName, trNo, trName, wgBudam, wgHr, ord, rcTime, winOdds, plcOdds 등)은
 * HttpKraApiClient 내부 매핑에서만 다룬다. 실키 발급 후 A03 항목에서 필드명 검증 필요.
 */
data class KraRacePlanDto(
    val meet: Int, val raceDate: LocalDate, val raceNo: Int,
    val distance: Int, val track: String, val grade: String, val startTime: String?,
)

data class KraEntryDto(
    val meet: Int, val raceDate: LocalDate, val raceNo: Int,
    val distance: Int, val track: String, val grade: String, val startTime: String?,
    val gateNo: Int, val hrNo: String, val hrName: String, val hrCountry: String, val hrSex: String, val hrAge: Int, val rating: Int,
    val jkNo: String, val jkName: String, val trNo: String, val trName: String,
    val weightCarried: Double, val horseWeight: Double?, val scratched: Boolean = false,
)

data class KraResultDto(
    val meet: Int, val raceDate: LocalDate, val raceNo: Int, val gateNo: Int, val hrNo: String,
    val finishPos: Int?, val finishTime: String?, val winOdds: Double?, val placeOdds: Double?,
    val horseWeight: Double? = null, val horseWeightDiff: Int? = null, val rating: Int? = null, val margin: String? = null,
)

data class KraPersonStatDto(
    val no: String, val name: String, val meet: Int,
    val careerStarts: Int, val careerWins: Int, val careerSeconds: Int,
    val yearStarts: Int, val yearWins: Int, val yearWinRate: Double?, val yearPlaceRate: Double?,
)

/** 엔드포인트 점검 결과(관리자용) */
data class KraProbeResult(val endpoint: String, val path: String, val ok: Boolean, val count: Int, val sampleKeys: List<String>, val error: String?)

data class KraHorseDto(
    val hrNo: String, val name: String, val country: String, val sex: String, val age: Int, val rating: Int,
    val starts: Int, val wins: Int, val seconds: Int, val thirds: Int,
)
