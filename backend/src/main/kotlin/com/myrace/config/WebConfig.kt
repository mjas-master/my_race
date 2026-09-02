package com.myrace.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class WebConfig(
    private val props: MyRaceProperties,
    private val deviceInterceptor: DeviceInterceptor,
    /** 허용 출처 목록(application.yml `myrace.cors.allowed-origins`). 패턴 사용 가능: http://localhost:* */
    @Value("\${myrace.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private val allowedOrigins: List<String>,
) : WebMvcConfigurer {

    @Bean
    fun kraWebClient(): WebClient {
        val http = HttpClient.create().responseTimeout(Duration.ofMillis(props.kra.timeoutMs))
        return WebClient.builder()
            .baseUrl(props.kra.baseUrl)
            .clientConnector(ReactorClientHttpConnector(http))
            .codecs { it.defaultCodecs().maxInMemorySize(8 * 1024 * 1024) }
            .build()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(deviceInterceptor)
            .addPathPatterns("/v1/**")
            .excludePathPatterns("/v1/devices", "/v1/admin/**")
    }

    /**
     * Flutter 웹(Chrome)은 프론트 포트와 8080 이 달라 교차 출처가 된다.
     * 개발: localhost 모든 포트 허용 / 운영: CORS_ORIGINS 환경변수로 프론트 주소 지정.
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(*allowedOrigins.map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")                 // X-Device-Id, X-Admin-Key 등 커스텀 헤더 프리플라이트 허용
            .exposedHeaders("*")
            .maxAge(3600)                        // 프리플라이트 캐시 1시간
    }
}