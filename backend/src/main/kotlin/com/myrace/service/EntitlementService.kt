package com.myrace.service

import com.myrace.domain.Platform
import com.myrace.domain.Subscription
import com.myrace.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/** 스토어 영수증 검증 stub. A20에서 Google Play Developer API / App Store Server API 로 교체. */
interface StoreReceiptVerifier {
    fun verify(platform: Platform, productId: String, purchaseToken: String): Instant?  // expiresAt, null=invalid
}

@Service
class StubStoreReceiptVerifier : StoreReceiptVerifier {
    override fun verify(platform: Platform, productId: String, purchaseToken: String): Instant? {
        if (purchaseToken.isBlank()) return null
        val days = if (productId.contains("yearly")) 365L else 30L
        return Instant.now().plus(days, ChronoUnit.DAYS)
    }
}

@Service
class EntitlementService(private val subs: SubscriptionRepository, private val verifier: StoreReceiptVerifier) {
    fun isPremium(deviceId: String): Boolean =
        subs.findFirstByDeviceIdAndActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(deviceId, Instant.now()) != null

    fun premiumUntil(deviceId: String): Instant? =
        subs.findFirstByDeviceIdAndActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(deviceId, Instant.now())?.expiresAt

    fun registerPurchase(deviceId: String, platform: Platform, productId: String, token: String): Subscription {
        val exp = verifier.verify(platform, productId, token)
            ?: throw com.myrace.api.ApiException(400, "INVALID_RECEIPT", "영수증 검증에 실패했습니다.")
        return subs.save(Subscription(deviceId = deviceId, platform = platform, productId = productId, purchaseToken = token, expiresAt = exp))
    }
}
