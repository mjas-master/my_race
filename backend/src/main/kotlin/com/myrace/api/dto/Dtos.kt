package com.myrace.api.dto

import com.myrace.domain.NotifyPrefs
import com.myrace.domain.Platform
import com.myrace.service.HitFlags
import java.time.Instant
import java.time.LocalDate

const val DISCLAIMER = "예측은 참고 정보이며 결과를 보장하지 않습니다."

data class TopPick(val gateNo: Int, val horseName: String, val hrNo: String, val score: Double)
data class RaceSummary(
    val id: Long, val meet: Int, val meetName: String, val raceNo: Int, val distance: Int, val track: String, val grade: String,
    val startTime: String?, val status: String, val entryCount: Int, val topPick: TopPick?,
)
data class RaceListResponse(val meet: Int?, val date: LocalDate, val updatedAt: Instant, val races: List<RaceSummary>)

data class EntryResult(val finishPos: Int, val finishTime: String?, val winOdds: Double?, val placeOdds: Double?)
data class EntryView(
    val rank: Int?, val gateNo: Int, val hrNo: String, val horseName: String,
    val jkNo: String, val jockeyName: String, val trNo: String, val trainerName: String,
    val weightCarried: Double, val horseWeight: Double?, val scratched: Boolean,
    val score: Double?, val factors: Map<String, String>?, val locked: Boolean, val result: EntryResult?,
)
data class RaceDetail(
    val id: Long, val meet: Int, val meetName: String, val date: LocalDate, val raceNo: Int, val distance: Int, val track: String,
    val grade: String, val startTime: String?, val status: String, val engineVersion: String?, val computedAt: Instant?,
    val premium: Boolean, val freeRankLimit: Int, val entries: List<EntryView>, val hit: HitFlags?, val disclaimer: String = DISCLAIMER,
)

data class ResultTop(val finishPos: Int, val gateNo: Int, val horseName: String, val finishTime: String?, val winOdds: Double?, val placeOdds: Double?)
data class RaceResultView(val id: Long, val raceNo: Int, val distance: Int, val hit: HitFlags?, val ourTopPick: TopPick?, val top3: List<ResultTop>)
data class ResultsResponse(val meet: Int?, val date: LocalDate, val races: List<RaceResultView>)

data class HorseRun(val date: LocalDate, val meet: Int, val raceNo: Int, val distance: Int, val finishPos: Int, val fieldSize: Int,
                    val finishTime: String?, val jockeyName: String, val jkNo: String, val winOdds: Double?)
data class DistanceStat(val distance: Int, val starts: Int, val wins: Int, val seconds: Int)
data class Career(val starts: Int, val wins: Int, val seconds: Int, val thirds: Int)
data class HorseDetail(val hrNo: String, val name: String, val country: String, val sex: String, val age: Int, val rating: Int,
                       val career: Career, val byDistance: List<DistanceStat>, val recentRuns: List<HorseRun>, val runsLocked: Boolean, val runsLimit: Int)

data class SeasonStat(val starts: Int, val wins: Int, val winRate: Double, val placeRate: Double)
data class WeekRate(val weekStart: LocalDate, val starts: Int, val rate: Double)
data class UpcomingEntry(val raceId: Long, val date: LocalDate, val raceNo: Int, val gateNo: Int, val horseName: String, val hrNo: String, val ourRank: Int?)
data class PersonDetail(val no: String, val name: String, val meet: Int, val meetName: String, val debutYear: Int?,
                        val season: SeasonStat, val last30d: SeasonStat, val weeklyWinRate: List<WeekRate>, val thisWeekEntries: List<UpcomingEntry>)

data class DeviceRequest(val deviceId: String, val platform: Platform = Platform.ANDROID, val adultConfirmed: Boolean,
                         val preferredMeet: Int? = null, val pushToken: String? = null, val prefs: NotifyPrefs? = null)
data class DeviceResponse(val deviceId: String, val premium: Boolean, val premiumUntil: Instant?, val adultConfirmedAt: Instant?,
                          val preferredMeet: Int, val prefs: NotifyPrefs, val adsEnabled: Boolean, val freeRankLimit: Int)

data class VerifyRequest(val platform: Platform, val productId: String, val purchaseToken: String)
data class VerifyResponse(val active: Boolean, val expiresAt: Instant, val productId: String)
