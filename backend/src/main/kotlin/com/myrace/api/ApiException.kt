package com.myrace.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

class ApiException(val status: Int, val code: String, message: String) : RuntimeException(message)

data class ErrorBody(val code: String, val message: String, val traceId: String)

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun api(e: ApiException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(e.status).body(ErrorBody(e.code, e.message ?: "", trace()))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badParam(e: IllegalArgumentException): ResponseEntity<ErrorBody> =
        ResponseEntity.badRequest().body(ErrorBody("INVALID_PARAM", e.message ?: "잘못된 요청", trace()))

    @ExceptionHandler(Exception::class)
    fun other(e: Exception): ResponseEntity<ErrorBody> {
        val t = trace(); log.error("[$t] unhandled", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorBody("INTERNAL", "서버 오류", t))
    }

    private fun trace() = UUID.randomUUID().toString().take(8)
}
