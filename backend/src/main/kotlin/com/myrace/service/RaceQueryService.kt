package com.myrace.service

import com.myrace.api.ApiException
import com.myrace.api.dto.*
import com.myrace.config.MyRaceProperties
import com.myrace.domain.Meet
import com.myrace.domain.Race
import com.myrace.domain.RaceStatus
import com.myrace.repository.EntryRepository
import com.myrace.repository.RaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class RaceQueryService(
    private val races: RaceRepository,
    private val entries: EntryRepository,
    private val predictions: PredictionService,
    private val hitEval: HitEvaluationService,
    private val props: MyRaceProperties,
) {
    private val freeLimit get() = props.prediction.freeRankLimit

    fun listRaces(meet: Int?, date: LocalDate): RaceListResponse {
        val list = if (meet == null) races.findAllByRaceDateOrderByMeetAscRaceNoAsc(date) else races.findAllByMeetAndRaceDateOrderByRaceNoAsc(meet, date)
        val items = list.map { r ->
            val es = entries.findAllByRaceIdOrderByGateNoAsc(r.id!!)
            val top = predictions.latestFor(r.id!!)?.items?.minByOrNull { it.rank }
            RaceSummary(r.id!!, r.meet, Meet.name(r.meet), r.raceNo, r.distance, r.track, r.grade, r.startTime?.toString(), r.status.name,
                es.count { !it.scratched }, top?.let { TopPick(it.entry.gateNo, it.entry.horse.name, it.entry.horse.hrNo, it.score) })
        }
        return RaceListResponse(meet, date, list.maxOfOrNull { it.updatedAt } ?: java.time.Instant.now(), items)
    }

    fun raceDetail(id: Long, premium: Boolean, order: String = "rank"): RaceDetail {
        val r = races.findById(id).orElseThrow { ApiException(404, "RACE_NOT_FOUND", "경주를 찾을 수 없습니다.") }
        val es = entries.findAllByRaceIdOrderByGateNoAsc(id)
        val p = predictions.latestFor(id)
        val rankOf = p?.items?.associate { it.entry.id!! to it } ?: emptyMap()
        val views = es.map { e ->
            val item = rankOf[e.id]
            val rank = item?.rank
            val showScore = premium || (rank != null && rank <= freeLimit)
            val showFactors = premium
            EntryView(rank, e.gateNo, e.horse.hrNo, e.horse.name, e.jockey.jkNo, e.jockey.name, e.trainer.trNo, e.trainer.name,
                e.weightCarried, e.horseWeight, e.scratched,
                if (showScore) item?.score else null,
                if (showFactors && item != null) predictions.parseFactors(item.factorsJson).mapValues { it.value.text } else null,
                locked = item != null && !premium && (rank == null || rank > freeLimit),
                result = e.finishPos?.let { EntryResult(it, e.finishTime, e.winOdds, e.placeOdds) })
        }
        val sorted = if (order == "gate") views.sortedBy { it.gateNo } else views.sortedWith(compareBy<EntryView, Int?>(nullsLast()) { it.rank })
        return RaceDetail(r.id!!, r.meet, Meet.name(r.meet), r.raceDate, r.raceNo, r.distance, r.track, r.grade, r.startTime?.toString(),
            r.status.name, p?.engineVersion, p?.computedAt, premium, freeLimit, sorted,
            if (r.status == RaceStatus.FINISHED) hitEval.flagsFor(id) else null)
    }

    fun raceDates(meet: Int?, from: LocalDate, to: LocalDate): List<LocalDate> = races.raceDates(meet, from, to)

    fun results(meet: Int?, date: LocalDate): ResultsResponse {
        val list = (if (meet == null) races.findAllByRaceDateOrderByMeetAscRaceNoAsc(date) else races.findAllByMeetAndRaceDateOrderByRaceNoAsc(meet, date))
            .filter { it.status == RaceStatus.FINISHED }
        return ResultsResponse(meet, date, list.map { r -> resultView(r) })
    }

    private fun resultView(r: Race): RaceResultView {
        val es = entries.findAllByRaceIdOrderByGateNoAsc(r.id!!)
        val top3 = es.filter { it.finishPos != null }.sortedBy { it.finishPos }.take(3)
            .map { ResultTop(it.finishPos!!, it.gateNo, it.horse.name, it.finishTime, it.winOdds, it.placeOdds) }
        val pick = predictions.latestFor(r.id!!)?.items?.minByOrNull { it.rank }
        return RaceResultView(r.id!!, r.raceNo, r.distance, hitEval.flagsFor(r.id!!),
            pick?.let { TopPick(it.entry.gateNo, it.entry.horse.name, it.entry.horse.hrNo, it.score) }, top3)
    }
}
