package com.myrace.repository

import com.myrace.domain.*
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate

interface RaceRepository : JpaRepository<Race, Long> {
    fun findByMeetAndRaceDateAndRaceNo(meet: Int, date: LocalDate, raceNo: Int): Race?
    fun findAllByRaceDateOrderByMeetAscRaceNoAsc(date: LocalDate): List<Race>
    fun findAllByMeetAndRaceDateOrderByRaceNoAsc(meet: Int, date: LocalDate): List<Race>
    fun findAllByMeetAndRaceDateAndStatus(meet: Int, date: LocalDate, status: RaceStatus): List<Race>
    fun findAllByRaceDateBetweenAndStatus(from: LocalDate, to: LocalDate, status: RaceStatus): List<Race>
    @Query("select distinct r.raceDate from Race r where (:meet is null or r.meet = :meet) and r.raceDate between :from and :to order by r.raceDate desc")
    fun raceDates(meet: Int?, from: LocalDate, to: LocalDate): List<LocalDate>
}

interface EntryRepository : JpaRepository<Entry, Long> {
    @EntityGraph(attributePaths = ["horse", "jockey", "trainer"])
    fun findAllByRaceIdOrderByGateNoAsc(raceId: Long): List<Entry>

    @Query("""select e from Entry e join fetch e.race r join fetch e.jockey
              where e.horse.hrNo = :hrNo and r.status = 'FINISHED' and e.finishPos is not null
              and r.raceDate < :before order by r.raceDate desc""")
    fun historyOfHorse(hrNo: String, before: LocalDate): List<Entry>

    @Query("""select e from Entry e join fetch e.race r join fetch e.horse
              where e.jockey.jkNo = :jkNo and r.status = 'FINISHED' and r.raceDate between :from and :to""")
    fun runsOfJockey(jkNo: String, from: LocalDate, to: LocalDate): List<Entry>

    @Query("""select e from Entry e join fetch e.race r join fetch e.horse
              where e.trainer.trNo = :trNo and r.status = 'FINISHED' and r.raceDate between :from and :to""")
    fun runsOfTrainer(trNo: String, from: LocalDate, to: LocalDate): List<Entry>

    @Query("""select e from Entry e join fetch e.race r join fetch e.horse
              where e.jockey.jkNo = :jkNo and r.raceDate between :from and :to and r.status <> 'FINISHED'""")
    fun upcomingOfJockey(jkNo: String, from: LocalDate, to: LocalDate): List<Entry>
}

interface HorseRepository : JpaRepository<Horse, Long> { fun findByHrNo(hrNo: String): Horse? }
interface JockeyRepository : JpaRepository<Jockey, Long> { fun findByJkNo(jkNo: String): Jockey? }
interface TrainerRepository : JpaRepository<Trainer, Long> { fun findByTrNo(trNo: String): Trainer? }

interface PredictionRepository : JpaRepository<Prediction, Long> {
    fun findFirstByRaceIdAndEngineVersionOrderByComputedAtDesc(raceId: Long, engineVersion: String): Prediction?
    fun findFirstByRaceIdOrderByComputedAtDesc(raceId: Long): Prediction?
}

interface HitRecordRepository : JpaRepository<HitRecord, Long> {
    fun existsByPredictionId(predictionId: Long): Boolean
    fun findAllByRaceDateBetweenAndExcludedFalse(from: LocalDate, to: LocalDate): List<HitRecord>
    fun findAllByMeetAndRaceDateBetweenAndExcludedFalse(meet: Int, from: LocalDate, to: LocalDate): List<HitRecord>
}

interface DeviceRepository : JpaRepository<Device, Long> { fun findByDeviceId(deviceId: String): Device? }

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findFirstByDeviceIdAndActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(deviceId: String, now: Instant): Subscription?
}
