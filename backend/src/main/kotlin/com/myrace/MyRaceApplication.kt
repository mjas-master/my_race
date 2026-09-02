package com.myrace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableCaching
@ConfigurationPropertiesScan
class MyRaceApplication

fun main(args: Array<String>) {
    runApplication<MyRaceApplication>(*args)
}
