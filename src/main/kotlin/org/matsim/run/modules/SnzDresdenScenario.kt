/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.run.modules

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import org.matsim.core.config.Config
import org.matsim.core.config.ConfigUtils
import org.matsim.core.config.groups.VspExperimentalConfigGroup
import org.matsim.episim.*
import org.matsim.episim.model.*
import org.matsim.episim.model.activity.ActivityParticipationModel
import org.matsim.episim.model.activity.DefaultParticipationModel
import org.matsim.episim.model.activity.LocationBasedParticipationModel
import org.matsim.episim.model.input.CreateRestrictionsFromCSV
import org.matsim.episim.model.listener.HouseholdSusceptibility
import org.matsim.episim.model.progression.AgeDependentDiseaseStatusTransitionModel
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel
import org.matsim.episim.model.testing.TestType
import org.matsim.episim.model.vaccination.District
import org.matsim.episim.model.vaccination.VaccinationFromRkiData
import org.matsim.episim.model.vaccination.VaccinationModel
import org.matsim.episim.policy.FixedPolicy
import org.matsim.episim.policy.Restriction
import org.matsim.episim.policy.ShutdownPolicy
import org.matsim.run.batch.DresdenCalibration
import java.io.File
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Singleton
import kotlin.math.roundToInt


/**
 * Scenario for Dresden using Senozon data.
 */
open class SnzDresdenScenario(builder: Builder = Builder()) : SnzProductionScenario() {

    class Builder : SnzProductionScenario.Builder<SnzDresdenScenario>() {

        var leisureOffset = 0.0
        var scale = 1.3
        var leisureNightly = false
        var leisureNightlyScale = 1.0
        var householdSusc = 1.0

        init {
            vaccinationModel = VaccinationFromRkiData::class.java
        }

        override fun build() = SnzDresdenScenario(this)
    }

    val sample = builder.sample
    val diseaseImport = builder.diseaseImport
    val restrictions = builder.restrictions
    val tracing = builder.tracing
    val activityHandling = builder.activityHandling
    val infectionModel = builder.infectionModel
    val importOffset = builder.importOffset
    val vaccinationModel = builder.vaccinationModel
    val vaccinations = builder.vaccinations
    val weatherModel = builder.weatherModel
    val imprtFctMult = builder.imprtFctMult
    val leisureOffset = builder.leisureOffset
    val scale = builder.scale
    val leisureNightly = builder.leisureNightly
    val leisureNightlyScale = builder.leisureNightlyScale
    val householdSusc = builder.householdSusc

    val importFactorBeforeJune = builder.importFactorBeforeJune
    val importFactorAfterJune = builder.importFactorAfterJune
    val locationBasedRestrictions = builder.locationBasedRestrictions

    override fun configure() {

        bind(ContactModel::class.java).to(SymmetricContactModel::class.java).`in`(Singleton::class.java)
        bind(DiseaseStatusTransitionModel::class.java).to(AgeDependentDiseaseStatusTransitionModel::class.java).`in`(Singleton::class.java)
        bind(InfectionModel::class.java).to(infectionModel).`in`(Singleton::class.java)
        bind(VaccinationModel::class.java).to(vaccinationModel).`in`(Singleton::class.java)
        bind(ShutdownPolicy::class.java).to(FixedPolicy::class.java).`in`(Singleton::class.java)

        if (activityHandling == EpisimConfigGroup.ActivityHandling.startOfDay) {
            val model = if (locationBasedRestrictions == LocationBasedRestrictions.yes) LocationBasedParticipationModel::class.java else DefaultParticipationModel::class.java
            bind(ActivityParticipationModel::class.java).to(model)
        }

        bind(HouseholdSusceptibility.Config::class.java).toInstance(
            HouseholdSusceptibility.newConfig().withSusceptibleHouseholds(householdSusc, 5.0)
                                                                   )

        // Useless since we are not taking ages into account in vaccine so far
        bind(VaccinationFromRkiData.Config::class.java).toInstance(
            VaccinationFromRkiData.newConfig(District.Dresden)
                    .withAgeGroup("12-17", 28255.8)
                    .withAgeGroup("18-59", 319955.0)
                    .withAgeGroup("60+", 151722.0)
                                                                  )

        Multibinder.newSetBinder(binder(), SimulationListener::class.java).addBinding().to(HouseholdSusceptibility::class.java)
    }

    @Provides
    @Singleton
    fun config(): Config {

        val dresdenFactor = 1.0 // Cologne model has about half as many agents as Berlin model, -> 2_352_480

        val config = ConfigUtils.createConfig(EpisimConfigGroup()).apply {
            // Turn off MATSim related warnings https://github.com/matsim-org/matsim-episim-libs/issues/91
            vspExperimental().vspDefaultsCheckingLevel = VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore
            global().randomSeed = 7564655870752979346L  //
            //            vehicles().vehiclesFile = INPUT.resolve("de_2020-vehicles.xml").toString()
            // Input files
            plans().inputFile = INPUT.resolve("dresden_snz_entirePopulation_emptyPlans_withDistricts_100pt_split_noCoord.xml.gz").toString()
        }
        val episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup::class.java)
        episimConfig {
            addInputEventsFiles(INPUT) {
                "dresden_snz_episim_events_wt_100pt_split.xml.gz" on DayOfWeek.MONDAY..DayOfWeek.FRIDAY
                "dresden_snz_episim_events_sa_100pt_split.xml.gz" on DayOfWeek.SATURDAY
                "dresden_snz_episim_events_so_100pt_split.xml.gz" on DayOfWeek.SUNDAY
            }

            activityHandling = this@SnzDresdenScenario.activityHandling
            // Calibration parameter
            //            calibrationParameter = 1.56E-5 * 0.2
            setStartDate("2020-02-01") // "2020-02-24"
            facilitiesHandling = EpisimConfigGroup.FacilitiesHandling.snz
            sampleSize = sample / 100.0
            hospitalFactor = 0.5
            // Progression config
            progressionConfig = progressionConfig(Transition.config()).build()
            //            progressionConfig = AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build()
            threads = 8
            daysInfectious = Integer.MAX_VALUE

            facilitiesHandling = EpisimConfigGroup.FacilitiesHandling.snz
            sampleSize = 1.0


            // Initial infections and import
            initialInfections = Int.MAX_VALUE

            /* TODO if (this.diseaseImport != DiseaseImport.no) {

//			SnzProductionScenario.configureDiseaseImport(episimConfig, diseaseImport, importOffset,
//					cologneFactor * imprtFctMult, importFactorBeforeJune, importFactorAfterJune);
                //disease import 2020
                Map<LocalDate, Integer> importMap = new HashMap<>();
                double importFactorBeforeJune = 4.0;
                double imprtFctMult = 1.0;
                long importOffset = 0;

                interpolateImport(importMap, cologneFactor * imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-02-24").plusDays(importOffset),
                        LocalDate.parse("2020-03-09").plusDays(importOffset), 0.9, 23.1);
                interpolateImport(importMap, cologneFactor * imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-03-09").plusDays(importOffset),
                        LocalDate.parse("2020-03-23").plusDays(importOffset), 23.1, 3.9);
                interpolateImport(importMap, cologneFactor * imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-03-23").plusDays(importOffset),
                        LocalDate.parse("2020-04-13").plusDays(importOffset), 3.9, 0.1);

                importMap.put(LocalDate.parse("2020-07-19"), (int) (0.5 * 32));
                importMap.put(LocalDate.parse("2020-08-09"), 1);

                episimConfig.setInfections_pers_per_day(importMap);
            }*/

            configureContactIntensities(episimConfig)

            // Policy, restrictions and masks
//            synthetizeMobilityData()
//            val builder: FixedPolicy.ConfigBuilder = CreateRestrictionsFromCSV(episimConfig).run {
//                setInput(INPUT.resolve("mobilityData.csv"))
//                setScale(this@SnzDresdenScenario.scale)
//                setLeisureAsNightly(this@SnzDresdenScenario.leisureNightly)
//                setNightlyScale(this@SnzDresdenScenario.leisureNightlyScale)
//                createPolicy()
//            }

            // Policy, restrictions and masks
            val builder: FixedPolicy.ConfigBuilder = CreateRestrictionsFromCSV(episimConfig).run {
                setInput(INPUT.resolve("DresdenSnzData_daily_until20220824.csv")) // Updated
//                setInput(INPUT.resolve("DresdenSnzData_daily_until20210917.csv"))
                setScale(this@SnzDresdenScenario.scale)
                setLeisureAsNightly(this@SnzDresdenScenario.leisureNightly)
                setNightlyScale(this@SnzDresdenScenario.leisureNightlyScale)
                createPolicy()
            }
            builder.setHospitalScale(this@SnzDresdenScenario.scale)
            policy = builder.build()

            //            builder.restrict(LocalDate.parse("2020-03-16"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_higher", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2020-04-27"), 0.5, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2020-06-29"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2020-08-11"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            //Lueften nach den Sommerferien
            //            builder.restrict(LocalDate.parse("2020-08-11"), Restriction.ofCiCorrection(0.5), "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2020-12-31"), Restriction.ofCiCorrection(1.0), "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //
            //            builder.restrict(LocalDate.parse("2020-10-12"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2020-10-23"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //
            //
            //            builder.restrict(LocalDate.parse("2020-12-23"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-01-11"), 0.5, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-03-29"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-04-10"), 0.5, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-07-05"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-08-17"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            ////		builder.restrict(LocalDate.parse("2021-08-17"), Restriction.ofCiCorrection(0.5), "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //
            //            builder.restrict(LocalDate.parse("2021-10-11"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-10-18"), 1.0, "educ_higher");
            //            builder.restrict(LocalDate.parse("2021-10-23"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-12-24"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_higher", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2022-01-08"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //
            //            builder.restrict(LocalDate.parse("2022-04-11"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2022-04-23"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2022-06-27"), 0.2, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2022-08-09"), 1.0, "educ_primary", "educ_kiga", "educ_secondary", "educ_tertiary", "educ_other");

            val masksCenterDate = LocalDate.of(2020, 4, 27)
            for (i in 0..14) {
                val date = masksCenterDate.plusDays(-14 / 2L + i)
                val clothFraction = 1.0 / 3 * 0.9
                val ffpFraction = 1.0 / 3 * 0.9
                val surgicalFraction = 1.0 / 3 * 0.9

                builder.restrict(date, Restriction.ofMask(mapOf(
                    FaceMask.CLOTH to clothFraction * i / 14,
                    FaceMask.N95 to ffpFraction * i / 14,
                    FaceMask.SURGICAL to surgicalFraction * i / 14)),
                                 "pt", "shop_daily", "shop_other", "errands")
            }
            //            builder.restrict(LocalDate.parse("2021-08-17"), Restriction.ofMask(FaceMask.N95, 0.9), "educ_primary", "educ_secondary", "educ_higher", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-11-02"), Restriction.ofMask(FaceMask.N95, 0.0), "educ_primary", "educ_secondary", "educ_tertiary", "educ_other");
            //            builder.restrict(LocalDate.parse("2021-12-02"), Restriction.ofMask(FaceMask.N95, 0.9), "educ_primary", "educ_secondary", "educ_tertiary", "educ_other");

            //curfew
            //            builder.restrict("2021-04-17", Restriction.ofClosingHours(21, 5), "leisure", "visit");
            //            Map<LocalDate, Double> curfewCompliance = new HashMap<LocalDate, Double>();
            //            curfewCompliance.put(LocalDate.parse("2021-04-17"), 1.0);
            //            curfewCompliance.put(LocalDate.parse("2021-05-31"), 0.0);
            //            episimConfig.setCurfewCompliance(curfewCompliance);

            //tracing
            if (tracing == Tracing.yes)
                configureTracing(config, dresdenFactor)

            //snapshot

             episimConfig.setSnapshotInterval(700); //

//            episimConfig.setStartFromSnap/shot("/bigdata/casus/matsim/matsim-episim-libs/battery/v16/calibration/dresden/output-dresden-snapshot_670/seed_7564655870752979346-OMICRON_BA5_Import_5-OMICRON_BA5_Inf_1.0/episim-snapshot-670-2021-12-01.zip");  // 2020-12-27 put path as the argument zip file after creating episimConfig.setSnapshotInterval

                       // episimConfig.setSnapshotInterval();


            // setInfections_pers_per_day(mapOf(LocalDate.EPOCH to 1)) // base case import


            //inital infections and import


            // Contact intensities
            val spaces = 20.0
            getOrAddContainersParams {
                "pt"("tr") { contactIntensity = 10.0; spacesPerFacility = spaces }
                "work" { contactIntensity = 1.47; spacesPerFacility = spaces }
                "leisure" { contactIntensity = 9.24; spacesPerFacility = spaces; isSeasonal = true }
                "educ_kiga" { contactIntensity = 11.0; spacesPerFacility = spaces }
                "educ_primary" { contactIntensity = 11.0; spacesPerFacility = spaces }
                "educ_secondary" { contactIntensity = 11.0; spacesPerFacility = spaces }
                "educ_tertiary" { contactIntensity = 11.0; spacesPerFacility = spaces }
                "educ_higher" { contactIntensity = 5.5; spacesPerFacility = spaces }
                "educ_other" { contactIntensity = 11.0;spacesPerFacility = spaces }
                "shop_daily" { contactIntensity = 0.88; spacesPerFacility = spaces }
                "shop_other" { contactIntensity = 0.88; spacesPerFacility = spaces }
                "errands" { contactIntensity = 1.47; spacesPerFacility = spaces }
                "business" { contactIntensity = 1.47; spacesPerFacility = spaces }
                "visit" { contactIntensity = 9.24; spacesPerFacility = spaces /* 33/3.57 */ }
                "home" { contactIntensity = 1.0; spacesPerFacility = 1.0 /* 33/33 */ }
                "quarantine_home" { contactIntensity = 1.0; spacesPerFacility = 1.0 /* 33/33 */ }
            }
        }

        // Testing rates
        ConfigUtils.addOrGetModule(config, TestingConfigGroup::class.java).apply {
            strategy = TestingConfigGroup.Strategy.ACTIVITIES
            val actsList = listOf("leisure", "work", "business", "educ_kiga", "educ_primary", "educ_secondary", "educ_tertiary", "educ_other", "educ_higher")
            setActivities(actsList)
            getParams(TestType.RAPID_TEST).apply {
                falseNegativeRate = 0.3
                falsePositiveRate = 0.03
                // Test 10% of persons doing these activities
                testingRate = 0.1
            }
            // All households can get tested
            householdCompliance = 1.0
        }

        if (vaccinations == Vaccinations.yes) {

            val vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup::class.java)
            configureVaccines(vaccinationConfig, 862988)

            if (vaccinationModel == VaccinationFromRkiData::class.java) {

                var url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Bundeslaender_COVID-19-Impfungen.csv"
                val share: MutableMap<LocalDate, Map<VaccinationType, Double>> = mutableMapOf()
                var rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14" && it[3] == "1" }
                var week = mutableMapOf<VaccinationType, Double>()
                var startDate = DresdenCalibration.LocalDate(rows.first()[0])
                var endDate = startDate.plusDays(7)
                for (row in rows) {
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

                val vaccinations: MutableMap<LocalDate, Int> = HashMap()
                url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Landkreise_COVID-19-Impfungen.csv"
                rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14612" && it[3] == "1" }

                startDate = DresdenCalibration.LocalDate("2020-12-28")
                endDate = startDate.plusDays(7)
                var cumulative = 0
                for (row in rows) {
                    val date = DresdenCalibration.LocalDate(row[0])
                    if (date.isBefore(endDate))
                        cumulative += row[4].toInt()
                    else {
                        //                println("$startDate, $cumulative")
                        vaccinations[startDate] = cumulative / 7
                        startDate = endDate
                        endDate = startDate.plusDays(7)
                        cumulative = 0
                    }
                }
                vaccinationConfig.setVaccinationCapacity_pers_per_day(vaccinations)

                //                // Compliance and capacity will come from data
                //                vaccinationConfig.setCompliancePerAge(Map.of(0, 1.0));
                //
                //                vaccinationConfig.setVaccinationCapacity_pers_per_day(Map.of());
                //                vaccinationConfig.setReVaccinationCapacity_pers_per_day(Map.of());
                //
                //                vaccinationConfig.setFromFile(INPUT.resolve("Aktuell_Deutschland_Landkreise_COVID-19-Impfungen.csv").toString());
            }
        }

        return config
    }

//  fun synthetizeMobilityData() {
//        val weekdays = getMobilityData("$berlinMobilityData/LK_mobilityData_weekdays.csv")
//        val weekends = getMobilityData("$berlinMobilityData/LK_mobilityData_weekends.csv")
//        val mobilityData = (weekdays + weekends) as ArrayList<Pair<LocalDate, Int>>
//        mobilityData.sortBy { it.first }
//        val missing = ArrayList<Pair<LocalDate, Int>>()
//        for ((day, reduction) in mobilityData)
//            when (day.dayOfWeek) {
//                DayOfWeek.FRIDAY -> repeat(4) { missing += day.minusDays(1L + it) to reduction }
//                DayOfWeek.SUNDAY -> missing += day.minusDays(1) to reduction
//                else -> error("invalid day present: $day (${day.dayOfWeek})")
//            }
//        mobilityData += missing
//        mobilityData.sortBy { it.first }
//        File("/bigdata/casus/matsim/matsim-episim-libs/dresden/mobilityData.csv")
//                .writeText(buildString {
//                    appendLine("date\tLandkreis\tpercentageChangeComparedToBeforeCorona")
//                    mobilityData.forEach {
//                        val date = it.first.format(formatter)
//                        val percentageChange = it.second
//                        appendLine("$date\tDresden\t$percentageChange")
//                    }
//                })
//    }
//
//    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
//    fun getMobilityData(csv: String): ArrayList<Pair<LocalDate, Int>> {
//        val days = csvReader().readAll(URL(csv).readText())
//                .map { it[0].split(';') }
//                .filter { it[1] == "Dresden" }
//                .map {
//                    val reduction = it.last()
//                    check('.' !in reduction && ',' !in reduction) { "reduction is not an integer!" }
//                    LocalDate.parse(it.first(), formatter) to reduction.toInt()
//                }.toCollection(ArrayList())
//        val missing = ArrayList<Pair<LocalDate, Int>>()
//        var prev = days.first().first
//        for (i in days.indices.drop(1)) {
//            val curr = days[i].first
//            val prevPlus7 = prev.plusDays(7)
//            if (prevPlus7 != curr) {
//                //            println("$prevPlus7 missing")
//                check(prevPlus7.plusDays(7) == curr) { "more then a week missing" }
//                check(i - 2 in days.indices && i + 2 in days.indices) {
//                    "too close to the end or the begin of data to calculate average of missing week"
//                }
//                val average = days.filterIndexed { index, _ -> index in i - 2..i + 2 }.map { it.second }.average().toInt()
//                missing += prevPlus7 to average
//                //            println("filled missing data with $prevPlus7 to $average")
//            }
//            prev = curr
//        }
//        days += missing
//        days.sortBy { it.first }
//        return days
//    }


    companion object {
        /**
         * Path pointing to the input folder. Needs to be adapted or set using the EPISIM_INPUT environment variable.
         */
        // public static final Path INPUT = EpisimUtils.resolveInputPath("../shared-svn/projects/episim/matsim-files/snz/Dresden/episim-input");
        val INPUT = EpisimUtils.resolveInputPath("dresden")

        //val berlinMobilityData = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/episim/mobilityData/landkreise"

        fun interpolateImport(importMap: HashMap<LocalDate, Int>, importFactor: Double, start: LocalDate, end: LocalDate, a: Double, b: Double) {
            val days = end.dayOfYear - start.dayOfYear
            for (i in 1..days) {
                val fraction = i.toDouble() / days
                importMap[start.plusDays(i.toLong())] = (importFactor * (a + fraction * (b - a))).roundToInt()
            }
        }
    }
}
