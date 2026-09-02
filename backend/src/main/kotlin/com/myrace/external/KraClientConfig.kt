package com.myrace.external

import com.myrace.config.MyRaceProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * KRA 클라이언트 선택.
 *  - myrace.ingest.mock=auto(기본): KRA_SERVICE_KEY 가 있으면 실API, 없으면 Mock
 *  - true / false 로 강제 가능
 * → 사용자는 환경변수 KRA_SERVICE_KEY 하나만 넣으면 실데이터로 전환된다.
 */
@Configuration
class KraClientConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun kraApiClient(web: WebClient, props: MyRaceProperties): KraApiClient {
        val mock = props.ingest.isMock(props.kra.serviceKey)
        log.info("KRA client = {} (mock={}, keyPresent={})", if (mock) "Mock" else "Http", props.ingest.mock, props.kra.serviceKey.isNotBlank())
        return if (mock) MockKraApiClient() else HttpKraApiClient(web, props)
    }
}
