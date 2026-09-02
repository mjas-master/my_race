package com.myrace.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "myrace")
data class MyRaceProperties(
    val timezone: String = "Asia/Seoul",
    val kra: Kra = Kra(),
    val ingest: Ingest = Ingest(),
    val prediction: Prediction = Prediction(),
    val flags: Flags = Flags(),
    val adminKey: String = "dev-admin",
) {
    /**
     * endpoints: 서비스명 → 경로(baseUrl 기준). 공공데이터포털 명세가 바뀌면 yml만 수정.
     * verified=true 인 것은 2026-09 포털 명세로 필드까지 확인됨. 나머지는 /v1/admin/kra/probe 로 검증.
     */
    data class Kra(
        val baseUrl: String = "https://apis.data.go.kr/B551015",
        val serviceKey: String = "",
        val timeoutMs: Long = 8000,
        val pageSize: Int = 1000,
        val endpoints: Map<String, String> = emptyMap(),
    )
    /** mock: "true" | "false" | "auto"(키 있으면 실API, 없으면 mock) */
    data class Ingest(val mock: String = "auto", val meets: List<Int> = listOf(1, 3, 2),
                      /** 실API 모드 기동 시 자동 백필 주 수(0=끔). 과거조회·적중률 화면용 데이터 확보 */
                      val backfillWeeksOnStart: Int = 8) {
        fun isMock(serviceKey: String): Boolean = when (mock.lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> serviceKey.isBlank()
        }
    }
    data class Prediction(
        val engine: String = "v1-rule",
        val freeRankLimit: Int = 3,
        val weights: Map<String, Double> = emptyMap(),
    )
    data class Flags(val adsEnabled: Boolean = true)
}
