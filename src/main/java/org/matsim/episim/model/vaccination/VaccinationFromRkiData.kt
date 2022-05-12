package org.matsim.episim.model.vaccination

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.inject.Inject
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.doubles.DoubleList
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import org.apache.logging.log4j.LogManager
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Person
import org.matsim.episim.EpisimPerson
import org.matsim.episim.EpisimUtils
import org.matsim.episim.InfectionEventHandler.EpisimFacility
import org.matsim.episim.InfectionEventHandler.EpisimVehicle
import org.matsim.episim.VaccinationConfigGroup
import org.matsim.episim.model.VaccinationType
import org.matsim.facilities.ActivityFacility
import org.matsim.run.batch.DresdenCalibration
import org.matsim.run.modules.vaxType
import org.matsim.vehicles.Vehicle
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.Table
import tech.tablesaw.io.csv.CsvReadOptions
import java.net.URL
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors
import kotlin.math.min

/**
 * Read vaccination from file for each age group.
 */
class VaccinationFromRkiData @Inject constructor(rnd: SplittableRandom?, vaccinationConfig: VaccinationConfigGroup?,
                                                 /**
                                                  * Config for this class.
                                                  */
                                                 private val config: Config) : VaccinationByAge(rnd, vaccinationConfig) {
    /**
     * All known age groups.
     */
    private lateinit var ageGroups: MutableList<AgeGroup>

    /**
     * Entries for each day.
     */
    private lateinit var entries: TreeMap<LocalDate, DoubleList>

    /**
     * Entries with booster vaccinations for each day.
     */
    private lateinit var booster: TreeMap<LocalDate, DoubleList>

    /**
     * Fallback to random vaccinations.
     */
    private val random = RandomVaccination(rnd, vaccinationConfig)

    override fun init(rnd: SplittableRandom, persons: Map<Id<Person>, EpisimPerson>, facilities: Map<Id<ActivityFacility>, EpisimFacility>, vehicles: Map<Id<Vehicle>, EpisimVehicle>) {

        var url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Bundeslaender_COVID-19-Impfungen.csv"
        val share = mutableMapOf<LocalDate, Map<VaccinationType, Double>>()
        val csv = csvReader().readAll(URL(url).readText()).filter { it[1] == config.district.bundesland && it[3] == "1" }
        var week = mutableMapOf<VaccinationType, Double>()
        var startDate = DresdenCalibration.LocalDate(csv.first()[0])
        var endDate = startDate.plusDays(7)
        for (row in csv) {
            val date = DresdenCalibration.LocalDate(row[0])
            if (date.isBefore(endDate)) {
                val type = row[2].vaxType
                week[type] = week.getOrDefault(type, 0.0) + row[4].toDouble()
            } else {
                val mRna = week.getOrDefault(VaccinationType.mRNA, 0.0)
                val vector = week.getOrDefault(VaccinationType.vector, 0.0)
                val subunit = week.getOrDefault(VaccinationType.subunit, 0.0)
                val total = mRna + vector + subunit
                week[VaccinationType.mRNA] = mRna / total
                week[VaccinationType.vector] = vector / total
                if (subunit != 0.0)
                    week[VaccinationType.subunit] = subunit / total
                share[startDate] = week
                week = mutableMapOf()
                startDate = endDate
                endDate = startDate.plusDays(7)
            }
        }

        vaccinationConfig.setVaccinationShare(share)

        //        requireNotNull(vaccinationConfig.fromFile) { "Vaccination file must be set, but was null" }
        ageGroups = ArrayList()
        entries = TreeMap()
        booster = TreeMap()
        val types = arrayOf<ColumnType>(ColumnType.LOCAL_DATE, ColumnType.STRING, ColumnType.STRING, ColumnType.INTEGER, ColumnType.INTEGER)
        ageGroups.add(AgeGroup(12, 17))
        ageGroups.add(AgeGroup(18, 59))
        ageGroups.add(AgeGroup(60, MAX_AGE - 1))

        url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Landkreise_COVID-19-Impfungen.csv"
        val rkiData = Table.read().usingOptions(CsvReadOptions.builder(URL(url)).tableName("rkidata").columnTypes(types))
        val locationColumn = rkiData.stringColumn("LandkreisId_Impfort")
        val location = locationColumn.isEqualTo(config.district.region)
        val table = rkiData.where(location)
        val vaccinationNo = table.intColumn("Impfschutz")
        val firstVaccinations = vaccinationNo.isEqualTo(1.0)
        var filtered = table.where(firstVaccinations)
        mergeData(filtered, entries, "12-17", config.groups.getDouble("12-17"), 0)
        mergeData(filtered, entries, "18-59", config.groups.getDouble("18-59"), 1)
        mergeData(filtered, entries, "60+", config.groups.getDouble("60+"), 2)
        val boosterVaccinations = vaccinationNo.isEqualTo(3.0)
        filtered = table.where(boosterVaccinations)
        mergeData(filtered, booster, "12-17", config.groups.getDouble("12-17"), 0)
        mergeData(filtered, booster, "18-59", config.groups.getDouble("18-59"), 1)
        mergeData(filtered, booster, "60+", config.groups.getDouble("60+"), 2)

        // collect population sizes
        for (p in persons.values)
            findAgeGroup(p.age)?.apply { size++ }
        log.info("Using age-groups: {}", ageGroups)
    }

    private fun findAgeGroup(age: Int): AgeGroup? = ageGroups.find { age >= it.from && age <= it.to }

    override fun handleVaccination(persons: Map<Id<Person>, EpisimPerson>, reVaccination: Boolean, availableVaccinations: Int, date: LocalDate, iteration: Int, now: Double): Int {

        // If available vaccination is given, data will be ignored and vaccination by age executed
        if (availableVaccinations >= 0) return random.handleVaccination(persons, reVaccination, availableVaccinations, date, iteration, now)
        val map = if (reVaccination) booster else entries
        val entry = EpisimUtils.findValidEntry(map as Map<LocalDate, DoubleList?>?, null, date) ?: return 0 // No vaccinations today

        // reset count
        ageGroups.forEach { it.vaccinated = 0 }
        val perAge: Array<MutableList<EpisimPerson>> = Array(MAX_AGE) { ArrayList() }
        for (p in persons.values) {
            val ag = findAgeGroup(p.age) ?: continue
            if (p.vaccinationStatus == EpisimPerson.VaccinationStatus.yes && (!reVaccination || p.reVaccinationStatus == EpisimPerson.VaccinationStatus.yes)) {
                ag.vaccinated++
                continue
            }
            if (p.isVaccinable && p.diseaseStatus == EpisimPerson.DiseaseStatus.susceptible &&
                //!p.isRecentlyRecovered(iteration) &&
                (p.vaccinationStatus == if (reVaccination) EpisimPerson.VaccinationStatus.yes else EpisimPerson.VaccinationStatus.no) &&
                (if (reVaccination) p.daysSince(EpisimPerson.VaccinationStatus.yes, iteration) >= vaccinationConfig.getParams(p.vaccinationType).boostWaitPeriod else true)
            )
                perAge[p.age] += p
        }
        val prob = vaccinationConfig.getVaccinationTypeProb(date)
        var totalVaccinations = 0
        for (ii in ageGroups.indices) {
            val ag = ageGroups[ii]
            val share = entry.getDouble(ii)
            var vaccinationsLeft = (ag.size * share - ag.vaccinated).toInt()
            var age = ag.to
            while (vaccinationsLeft > 0 && age >= ag.from) {
                val candidates = perAge[age]

                // list is shuffled to avoid eventual bias
                if (candidates.size > vaccinationsLeft) perAge[age].shuffle(Random(EpisimUtils.getSeed(rnd)))
                for (i in 0 until min(candidates.size, vaccinationsLeft)) {
                    val person = candidates[i]
                    vaccinate(person, iteration, if (reVaccination) null else VaccinationModel.chooseVaccinationType(prob, rnd), reVaccination)
                    vaccinationsLeft--
                    totalVaccinations++
                }
                age--
            }
        }
        return totalVaccinations
    }

    private class AgeGroup constructor(val from: Int, val to: Int) {
        var size = 0
        var vaccinated = 0
        override fun toString() = "AgeGroup{from=$from, to=$to, size=$size}"
    }

    /**
     * Holds config options for this class.
     */
    class Config(val district: District) {
        val groups: Object2DoubleMap<String> = Object2DoubleLinkedOpenHashMap()

        /**
         * Define an age group and reference size in the population.
         * @param ageGroup string that must be exactly like in the data
         * @param referenceSize unscaled reference size of this age group.
         */
        fun withAgeGroup(ageGroup: String, referenceSize: Double): Config {
            groups[ageGroup] = referenceSize
            return this
        }
    }

    companion object {
        private val log = LogManager.getLogger(VaccinationFromRkiData::class.java)
        fun filterData(table: Table, ageGroup: String, population: Double): Table {
            val selection = table.stringColumn("Altersgruppe").isEqualTo(ageGroup)
            var data = table.where(selection)
            val dateColumn = data.dateColumn("Impfdatum")
            val startDate = dateColumn.min()
            val endDate = dateColumn.max()
            val dates = startDate.datesUntil(endDate).collect(Collectors.toList())
            for (date in dates) {
                val thisDateSelection = dateColumn.isEqualTo(date)
                val thisDateTable = data.where(thisDateSelection)
                if (thisDateTable.rowCount() == 0) {
                    val singleRow = data.emptyCopy(1)
                    for (row in singleRow) {
                        row.setDate("Impfdatum", date)
                        row.setString("Altersgruppe", ageGroup)
                        row.setInt("Anzahl", 0)
                    }
                    data.append(singleRow)
                }
            }
            data = data.sortAscendingOn("Impfdatum")
            val cumsumeAnzahl = data.intColumn("Anzahl").cumSum()
            val quota = cumsumeAnzahl.divide(population)
            quota.setName(ageGroup)
            data.addColumns(quota)
            data.removeColumns("Impfschutz", "LandkreisId_Impfort", "Altersgruppe", "Anzahl")
            data.column("Impfdatum").setName("date")
            return data
        }

        fun mergeData(filtered: Table, entries: TreeMap<LocalDate, DoubleList>, ageGroup: String, population: Double, i: Int) {
            for (row in filterData(filtered, ageGroup, population)) {
                val date = row.getDate(0)
                val values = entries.computeIfAbsent(date) { DoubleArrayList(DoubleArray(3)) }
                values[i] = row.getDouble(1)
            }
        }

        /**
         * Create a new configuration, that needs to be bound with guice.
         */
        @JvmStatic fun newConfig(locationId: String) = Config(District.values().first { it.region == locationId })
    }
}