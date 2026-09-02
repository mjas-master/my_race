package com.myrace.external

import java.time.LocalDate

interface KraApiClient {
    fun fetchRacePlan(meet: Int, from: LocalDate, to: LocalDate): List<KraRacePlanDto>
    fun fetchEntries(meet: Int, date: LocalDate): List<KraEntryDto>
    fun fetchResults(meet: Int, date: LocalDate): List<KraResultDto>
    fun fetchHorse(hrNo: String): KraHorseDto?
    fun fetchJockey(jkNo: String, meet: Int): KraPersonStatDto? = null
    fun fetchTrainer(trNo: String, meet: Int): KraPersonStatDto? = null
    fun healthy(): Boolean
    fun probe(meet: Int, date: LocalDate): List<KraProbeResult> = emptyList()
}
