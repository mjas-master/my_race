package com.myrace.config

import com.myrace.api.ApiException
import com.myrace.repository.DeviceRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsUtils
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 모든 /v1 요청은 X-Device-Id 필수. 성인 확인 안 된 기기는 403.
 * 단, 브라우저(Flutter 웹)의 CORS 프리플라이트(OPTIONS)에는 커스텀 헤더가 실리지 않으므로 검사 없이 통과시킨다.
 */
@Component
class DeviceInterceptor(private val devices: DeviceRepository) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (CorsUtils.isPreFlightRequest(request)) return true
        val id = request.getHeader(DEVICE_HEADER)?.takeIf { it.isNotBlank() }
            ?: throw ApiException(401, "DEVICE_REQUIRED", "X-Device-Id 헤더가 필요합니다.")
        val device = devices.findByDeviceId(id)
            ?: throw ApiException(403, "ADULT_NOT_CONFIRMED", "먼저 POST /v1/devices 로 성인 확인을 완료하세요.")
        if (!device.adultConfirmed) throw ApiException(403, "ADULT_NOT_CONFIRMED", "성인 확인이 필요합니다.")
        request.setAttribute(DEVICE_ATTR, device)
        return true
    }
    companion object {
        const val DEVICE_HEADER = "X-Device-Id"
        const val DEVICE_ATTR = "myrace.device"
    }
}