package com.myrace.domain

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class RaceStatus { PLANNED, ENTRIES_FIXED, FINISHED, CANCELED }
enum class Platform { ANDROID, IOS }

object Meet {
    const val SEOUL = 1
    const val JEJU = 2
    const val BUSAN = 3
    fun name(meet: Int) = when (meet) { 1 -> "서울"; 2 -> "제주"; 3 -> "부경"; else -> "?" }
}

@Entity
@Table(name = "race", uniqueConstraints = [UniqueConstraint(columnNames = ["meet", "race_date", "race_no"])])
class Race(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    var meet: Int,
    @Column(name = "race_date") var raceDate: LocalDate,
    @Column(name = "race_no") var raceNo: Int,
    var distance: Int,
    var track: String = "모래",
    var grade: String = "",
    var startTime: LocalTime? = null,
    @Enumerated(EnumType.STRING) var status: RaceStatus = RaceStatus.PLANNED,
    var entriesHash: String? = null,
    var updatedAt: Instant = Instant.now(),
) {
    @OneToMany(mappedBy = "race", cascade = [CascadeType.ALL], orphanRemoval = true)
    var entries: MutableList<Entry> = mutableListOf()
}

@Entity
@Table(name = "horse")
class Horse(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true) var hrNo: String,
    var name: String,
    var country: String = "한국",
    var sex: String = "",
    var age: Int = 0,
    var rating: Int = 0,
    var careerStarts: Int = 0,
    var careerWins: Int = 0,
    var careerSeconds: Int = 0,
    var careerThirds: Int = 0,
)

@Entity
@Table(name = "jockey")
class Jockey(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true) var jkNo: String,
    var name: String,
    var meet: Int = 1,
    var debutYear: Int? = null,
    var careerStarts: Int = 0, var careerWins: Int = 0, var careerSeconds: Int = 0,
    var yearStarts: Int = 0, var yearWins: Int = 0,
)

@Entity
@Table(name = "trainer")
class Trainer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true) var trNo: String,
    var name: String,
    var meet: Int = 1,
    var careerStarts: Int = 0, var careerWins: Int = 0, var careerSeconds: Int = 0,
    var yearStarts: Int = 0, var yearWins: Int = 0,
)

@Entity
@Table(name = "entry", uniqueConstraints = [UniqueConstraint(columnNames = ["race_id", "gate_no"])])
class Entry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "race_id") var race: Race,
    @Column(name = "gate_no") var gateNo: Int,
    @ManyToOne(fetch = FetchType.LAZY) var horse: Horse,
    @ManyToOne(fetch = FetchType.LAZY) var jockey: Jockey,
    @ManyToOne(fetch = FetchType.LAZY) var trainer: Trainer,
    var weightCarried: Double = 0.0,
    var horseWeight: Double? = null,
    var horseWeightDiff: Int? = null,
    var rating: Int? = null,
    var margin: String? = null,
    var scratched: Boolean = false,
    var finishPos: Int? = null,
    var finishTime: String? = null,
    var finishTimeMs: Int? = null,
    var winOdds: Double? = null,
    var placeOdds: Double? = null,
)

@Entity
@Table(name = "prediction")
class Prediction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) var race: Race,
    var engineVersion: String,
    var computedAt: Instant = Instant.now(),
    @Column(columnDefinition = "text") var inputSnapshotJson: String = "{}",
) {
    @OneToMany(mappedBy = "prediction", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("rank ASC")
    var items: MutableList<PredictionItem> = mutableListOf()
}

@Entity
@Table(name = "prediction_item")
class PredictionItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) var prediction: Prediction,
    @ManyToOne(fetch = FetchType.LAZY) var entry: Entry,
    @Column(name = "rank_no") var rank: Int,
    var score: Double,
    @Column(columnDefinition = "text") var factorsJson: String = "{}",
)

@Entity
@Table(name = "hit_record")
class HitRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY) var prediction: Prediction,
    var meet: Int,
    var raceDate: LocalDate,
    var engineVersion: String,
    var winHit: Boolean,
    var placeHit: Boolean,
    var showHit: Boolean,
    var top3Exact: Boolean,
    var excluded: Boolean = false,
    var reason: String? = null,
    var evaluatedAt: Instant = Instant.now(),
)

@Embeddable
class NotifyPrefs(
    var notifyEntries: Boolean = true,
    var notifyRankUpdate: Boolean = true,
    var notifyPreRace: Boolean = false,
)

@Entity
@Table(name = "device")
class Device(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(unique = true) var deviceId: String,
    @Enumerated(EnumType.STRING) var platform: Platform = Platform.ANDROID,
    var pushToken: String? = null,
    var preferredMeet: Int = Meet.SEOUL,
    var adultConfirmed: Boolean = false,
    var adultConfirmedAt: Instant? = null,
    @Embedded var prefs: NotifyPrefs = NotifyPrefs(),
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "subscription")
class Subscription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    var deviceId: String,
    @Enumerated(EnumType.STRING) var platform: Platform,
    var productId: String,
    @Column(length = 2000) var purchaseToken: String,
    var expiresAt: Instant,
    var active: Boolean = true,
    var verifiedAt: Instant = Instant.now(),
)
