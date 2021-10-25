package org.matsim.run.modules

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.matsim.episim.EpisimConfigGroup
import org.matsim.episim.EpisimConfigGroup.InfectionParams
import org.matsim.episim.model.VaccinationType
import java.net.URL
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate


inline operator fun EpisimConfigGroup.invoke(block: EpisimConfigGroup.() -> Unit) {
    apply(block)
}

inline fun EpisimConfigGroup.addInputEventsFiles(root: Path, block: EventFileParamsBuilder.() -> Unit) {
    EventFileParamsBuilder.config = this
    EventFileParamsBuilder.root = root
    EventFileParamsBuilder.block()
}

object EventFileParamsBuilder {
    lateinit var config: EpisimConfigGroup
    lateinit var root: Path

    infix fun String.on(day: DayOfWeek) = on(*arrayOf(day))

    fun String.on(vararg days: DayOfWeek) = config.addInputEventsFile(root.resolve(this).toString()).addDays(*days)

    infix fun String.on(days: ClosedRange<DayOfWeek>) = on(*DayOfWeek.values().filter { it in days }.toTypedArray())
}

inline operator fun EpisimConfigGroup.InfectionParams.invoke(block: EpisimConfigGroup.InfectionParams.() -> Unit) {
    apply(block)
}

inline fun EpisimConfigGroup.getOrAddContainersParams(block: InfectionParamsBuilder.() -> Unit) {
    InfectionParamsBuilder.config = this
    InfectionParamsBuilder.block()
}

object InfectionParamsBuilder {

    lateinit var config: EpisimConfigGroup

    inline operator fun String.invoke(vararg mappedNames: String, block: InfectionParams.() -> Unit) =
        config.getOrAddContainerParams(this, *mappedNames).block()
}

fun main() {
    val url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Bundeslaender_COVID-19-Impfungen.csv"
    val share: MutableMap<LocalDate, Map<VaccinationType, Double>> = mutableMapOf()
    val rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14" && it[3] == "1" }
    var week = mutableMapOf<VaccinationType, Double>()
    var startDate = LocalDate.parse(rows.first()[0])
    var endDate = startDate.plusDays(7)
    for (row in rows) {
        val date = LocalDate.parse(row[0])
        if (date.isBefore(endDate)) {
            val type = row[2].vaxType
            week[type] = week.getOrDefault(type, 0.0) + row[4].toDouble()
        } else {
            share[startDate] = week
            week = mutableMapOf()
            startDate = endDate
            endDate = startDate.plusDays(7)
        }
    }
}

val String.vaxType: VaccinationType
    get() = when (this) {
        "Comirnaty", "Moderna" -> VaccinationType.mRNA
        "AstraZeneca", "Janssen" -> VaccinationType.vector
        else -> error("invalid")
    }