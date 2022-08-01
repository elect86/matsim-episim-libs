package org.matsim.run.modules

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.matsim.episim.EpisimConfigGroup
import org.matsim.episim.EpisimConfigGroup.InfectionParams
import org.matsim.episim.model.VaccinationType
import java.net.URL
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter


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
    //    val url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Bundeslaender_COVID-19-Impfungen.csv"
    //    val share: MutableMap<LocalDate, Map<VaccinationType, Double>> = mutableMapOf()
    //    val rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14" && it[3] == "1" }
    //    var week = mutableMapOf<VaccinationType, Double>()
    //    var startDate = LocalDate.parse(rows.first()[0])
    //    var endDate = startDate.plusDays(7)
    //    for (row in rows) {
    //        val date = LocalDate.parse(row[0])
    //        if (date.isBefore(endDate)) {
    //            val type = row[2].vaxType
    //            week[type] = week.getOrDefault(type, 0.0) + row[4].toDouble()
    //        } else {
    //            val mRna = week.getOrDefault(VaccinationType.mRNA, 0.0)
    //            val vector = week.getOrDefault(VaccinationType.vector, 0.0)
    //            val total = mRna + vector
    //            week[VaccinationType.mRNA] = mRna / total
    //            week[VaccinationType.vector] = vector / total
    //            share[startDate] = week
    //            week = mutableMapOf()
    //            startDate = endDate
    //            endDate = startDate.plusDays(7)
    //        }
    //    }
    val weekdays = getMobilityData("${SnzDresdenScenario.berlinMobilityData}/LK_mobilityData_weekdays.csv")
    val weekends = getMobilityData("${SnzDresdenScenario.berlinMobilityData}/LK_mobilityData_weekends.csv")
    val mobilityData = (weekdays + weekends) as ArrayList<Pair<LocalDate, Int>>
    mobilityData.sortBy { it.first }
    for (i in mobilityData.indices) {
        mobilityData.getOrNull(i + 1)?.let { (nextDate, _) ->
            val anchor = mobilityData[i]
            var currDatePlusDelta = anchor.first.plusDays(1)
            while (currDatePlusDelta < nextDate) {
                mobilityData += currDatePlusDelta to anchor.second
                currDatePlusDelta = currDatePlusDelta.plusDays(1)
//                println("added $currDatePlusDelta to ${anchor.second}")
            }
        }
    }
    /*
    val we = csvReader().readAll(URL(weekends).readText()).filter { it[1] == "Dresden" }
            .map { LocalDate.parse(it.first(), formatter) to it.last()}
    val days: List<LocalDate> = (wd + we).map { it.first }.sorted()
    var date = days.first().minusDays(1)
    for (day in days) {
        check(day == date.plusDays(1))
        date = day
    }*/
    println()
}

fun getMobilityData(csv: String): ArrayList<Pair<LocalDate, Int>> {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val wd = csvReader().readAll(URL(csv).readText())
            .map { it[0].split(';') }
            .filter { it[1] == "Dresden" }
            .map {
                val reduction = it.last()
                check('.' !in reduction && ',' !in reduction)
                LocalDate.parse(it.first(), formatter) to reduction.toInt()
            }.toCollection(ArrayList())
    val missing = ArrayList<Pair<LocalDate, Int>>()
    var prev = wd.first().first
    for (i in wd.indices.drop(1)) {
        val curr = wd[i].first
        val prevPlus7 = prev.plusDays(7)
        if (prevPlus7 != curr) {
            println("$prevPlus7 missing")
            check(prevPlus7.plusDays(7) == curr) { "more then a week missing" }
            check(i - 2 in wd.indices && i + 2 in wd.indices) {
                "too close to the end or the begin of data to calculate average of missing week"
            }
            val average = wd.filterIndexed { index, _ -> index in i - 2..i + 2 }.map { it.second }.average().toInt()
            missing += prevPlus7 to average
//            println("filled missing data with $prevPlus7 to $average")
        }
        prev = curr
    }
    wd += missing
    wd.sortBy { it.first }
    return wd
}

val String.vaxType: VaccinationType
    get() = when (this) {
        "Comirnaty", "Moderna" -> VaccinationType.mRNA
        "AstraZeneca", "Janssen" -> VaccinationType.vector
        "Novavax" -> VaccinationType.subunit
        else -> error("invalid $this")
    }