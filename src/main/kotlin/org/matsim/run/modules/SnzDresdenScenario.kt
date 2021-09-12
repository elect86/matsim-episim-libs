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
import dev.misfitlabs.kotlinguice4.KotlinModule
import org.matsim.core.config.Config
import org.matsim.core.config.ConfigUtils
import org.matsim.episim.*
import org.matsim.episim.TracingConfigGroup.CapacityType
import org.matsim.episim.model.*
import org.matsim.episim.model.input.CreateRestrictionsFromCSV
import org.matsim.episim.model.testing.TestType
import org.matsim.episim.policy.FixedPolicy
import org.matsim.episim.policy.Restriction
import org.matsim.run.modules.SnzBerlinScenario25pct2020.BasePolicyBuilder
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Singleton

/**
 * Scenario for Dresden using Senozon data.
 */
class SnzDresdenScenario  // public static final Path INPUT = Path.of("/home/abhishek/Desktop/episim-dresden-libs/dresden-data");
/**
 * Empty constructor is needed for running scenario from command line.
 */
    : KotlinModule() {
    override fun configure() {
        bind<ContactModel>().to<SymmetricContactModel>().`in`<Singleton>()
        bind<ProgressionModel>().to<AgeDependentProgressionModel>().`in`<Singleton>()
        bind<InfectionModel>().to<AgeDependentInfectionModelWithSeasonality>().`in`<Singleton>()
        bind<VaccinationModel>().to<VaccinationByAge>().`in`<Singleton>()
    }

    @Provides @Singleton
    fun config(): Config {
        val config = ConfigUtils.createConfig(EpisimConfigGroup())
        val episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup::class.java)
        episimConfig {
            facilitiesHandling = EpisimConfigGroup.FacilitiesHandling.snz
            sampleSize = 1.0

            // Input files
            config.plans().inputFile = INPUT.resolve("dresden_snz_entirePopulation_emptyPlans_withDistricts_100pt_split_noCoord.xml.gz").toString()

            addInputEventsFiles(INPUT) {
                "dresden_snz_episim_events_wt_100pt_split.xml.gz" on DayOfWeek.MONDAY..DayOfWeek.FRIDAY
                "dresden_snz_episim_events_sa_100pt_split.xml.gz" on DayOfWeek.SATURDAY
                "dresden_snz_episim_events_so_100pt_split.xml.gz" on DayOfWeek.SUNDAY
            }


            // Calibration parameter
            calibrationParameter = 2.5E-5 * 0.8 // TODO  //2.5E-5 * 0.8(calibrated)
            setStartDate("2020-03-02")

            //snapshot

            // episimConfig.setSnapshotInterval(100); // At every 100 days it will create a snapshot

            // episimConfig.setStartFromSnapshot();  // 2020-12-27 put path as the argument zip file after creating episimConfig.setSnapshotInterval

            // episimConfig.setSnapshotInterval();


            // Progression config
            progressionConfig = AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build()


            // Initial infections and import
            initialInfections = Int.MAX_VALUE
            setInfections_pers_per_day(mapOf(LocalDate.EPOCH to 1)) // base case import

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
                "educ_higher"{ contactIntensity = 5.5; spacesPerFacility = spaces }
                "educ_other"{ contactIntensity = 11.0;spacesPerFacility = spaces }
                "shop_daily"{ contactIntensity = 0.88; spacesPerFacility = spaces }
                "shop_other"{ contactIntensity = 0.88; spacesPerFacility = spaces }
                "errands"{ contactIntensity = 1.47; spacesPerFacility = spaces }
                "business" { contactIntensity = 1.47; spacesPerFacility = spaces }
                "visit"{ contactIntensity = 9.24; spacesPerFacility = spaces /* 33/3.57 */ }
                "home" { contactIntensity = 1.0; spacesPerFacility = 1.0 /* 33/33 */ }
                "quarantine_home"{ contactIntensity = 1.0; spacesPerFacility = 1.0 /* 33/33 */ }
            }
        }

        // Tracing
        ConfigUtils.addOrGetModule(config, TracingConfigGroup::class.java).apply {
            //			int offset = (int) (ChronoUnit.DAYS.between(episimConfig.getStartDate(), LocalDate.parse("2020-04-01")) + 1);
            val offset = 46
            putTraceablePersonsInQuarantineAfterDay = offset
            setTracingProbability(0.5)
            setTracingPeriod_days(2)
            setMinContactDuration_sec(15 * 60.0)
            setQuarantineHouseholdMembers(true)
            equipmentRate = 1.0
            setTracingDelay_days(5)
            traceSusceptible = true
            capacityType = CapacityType.PER_PERSON
            val tracingCapacity = 200
            setTracingCapacity_pers_per_day(mapOf(
                LocalDate.of(2020, 4, 1) to (tracingCapacity * 0.2).toInt(),
                LocalDate.of(2020, 6, 15) to tracingCapacity))
        }


        // Vaccination capacity
        val vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup::class.java).apply {
            effectiveness = 0.9
            daysBeforeFullEffect = 28
        }
        val vaccinations: MutableMap<LocalDate, Int> = HashMap()
        val population = 862987 // Berlin: 4_800_000;

        val url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Landkreise_COVID-19-Impfungen.csv"
        val rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14612" && it[3] == "1" }

        var startDate = LocalDate.parse("2020-12-28")
        var endDate = startDate.plusDays(7)
        var cumulative = 0
        for (row in rows) {
            val date = LocalDate.parse(row[0])
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


        //vaccinations.put(LocalDate.parse("2020-01-01"), 0);
        //        vaccinations[LocalDate.parse("2020-12-28")] = (823 / 7)
        //        // the following is for Berlin; if we have data we can change it for dresden
        //        vaccinations[LocalDate.parse("2021-01-04")] = (2310 / 7)
        //        vaccinations[LocalDate.parse("2021-01-11")] = (4155 / 7)
        //        vaccinations[LocalDate.parse("2021-01-18")] = (4400 / 7)
        //        vaccinations[LocalDate.parse("2021-01-25")] = (4567 / 7)
        //        vaccinations[LocalDate.parse("2021-02-01")] = (6304 / 7)
        //        vaccinations[LocalDate.parse("2021-02-08")] = (5937 / 7)
        //        vaccinations[LocalDate.parse("2021-02-15")] = (6780 / 7)
        //        vaccinations[LocalDate.parse("2021-02-22")] = (9034 / 7)
        //        vaccinations[LocalDate.parse("2021-03-01")] = (9871 / 7)
        //        vaccinations[LocalDate.parse("2021-03-08")] = (7900 / 7)
        //        vaccinations[LocalDate.parse("2021-03-15")] = (7627 / 7)
        //        vaccinations[LocalDate.parse("2021-03-22")] = (10898 / 7)
        //        vaccinations[LocalDate.parse("2021-03-29")] = (8464 / 7)
        //        vaccinations[LocalDate.parse("2021-04-05")] = (17411 / 7)
        //        vaccinations[LocalDate.parse("2021-04-12")] = (21983 / 7)
        //        vaccinations[LocalDate.parse("2021-04-19")] = (19564 / 7)
        //        vaccinations[LocalDate.parse("2021-04-26")] = (25340 / 7)
        //        vaccinations[LocalDate.parse("2021-05-03")] = (27635 / 7)
        //        vaccinations[LocalDate.parse("2021-05-10")] = (25711 / 7)
        //        vaccinations[LocalDate.parse("2021-05-17")] = (27500 / 7)
        //        vaccinations[LocalDate.parse("2021-05-24")] = (25708 / 7)
        //        vaccinations[LocalDate.parse("2021-05-31")] = (28428 / 7)
        //        vaccinations[LocalDate.parse("2021-06-07")] = (28296 / 7)
        //        vaccinations[LocalDate.parse("2021-06-14")] = (30633 / 7)
        //        vaccinations[LocalDate.parse("2021-06-21")] = (29838 / 7)
        //        vaccinations[LocalDate.parse("2021-06-28")] = (28438 / 7)
        //        vaccinations[LocalDate.parse("2021-07-05")] = (26057 / 7)
        //        vaccinations[LocalDate.parse("2021-07-12")] = (22527 / 7)
        //        vaccinations[LocalDate.parse("2021-07-19")] = (20560 / 7)
        //        vaccinations[LocalDate.parse("2021-07-26")] = (20779 / 7)
        //        vaccinations[LocalDate.parse("2021-08-02")] = (13450 / 7)


        // int population =862987;  // Berlin: 4_800_000;
        //		vaccinations.put(LocalDate.parse("2020-01-01"), 0);
        //		vaccinations.put(LocalDate.parse("2020-12-27"), (int) (0.003 * population / 6));
        //// the following is for Berlin; if we have data we can change it for dresden
        //		vaccinations.put(LocalDate.parse("2021-01-02"), (int) ((0.007 - 0.004) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-01-09"), (int) ((0.013 - 0.007) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-01-16"), (int) ((0.017 - 0.013) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-01-23"), (int) ((0.024 - 0.017) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-01-30"), (int) ((0.030 - 0.024) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-02-06"), (int) ((0.034 - 0.030) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-02-13"), (int) ((0.039 - 0.034) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-02-20"), (int) ((0.045 - 0.039) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-02-27"), (int) ((0.057 - 0.045) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-03-06"), (int) ((0.071 - 0.057) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-03-13"), (int) ((0.088 - 0.071) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-03-20"), (int) ((0.105 - 0.088) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-03-27"), (int) ((0.120 - 0.105) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-04-03"), (int) ((0.140 - 0.120) * population / 7));
        //		vaccinations.put(LocalDate.parse("2021-04-10"), (int) ((0.183 - 0.140) * population / 7));


        //mutations and vaccinations
        val infPerDayB117: MutableMap<LocalDate, Int> = hashMapOf(
            LocalDate.parse("2020-01-01") to 0,
            LocalDate.parse("2020-10-01") to 1) // "2020-11-30")
        episimConfig.setInfections_pers_per_day(VirusStrain.B117, infPerDayB117)

        // VaccinationConfigGroup vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
        val vaccineEff = vaccinationConfig.effectiveness
        val virusStrainConfigGroup = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java).apply {
            getOrAddParams(VirusStrain.B117).apply {
                infectiousness = 1.2 // 1.8
                vaccineEffectiveness = 1.0
                factorSeriouslySick = 1.5
                factorSeriouslySickVaccinated = 0.05 / (1 - vaccineEff)
            }
            getOrAddParams(VirusStrain.SARS_CoV_2).factorSeriouslySickVaccinated = 0.05 / (1 - vaccineEff)
        }
        val infPerDayMUTB: MutableMap<LocalDate, Int> = hashMapOf(
            LocalDate.parse("2020-01-01") to 0,
            LocalDate.parse("2021-02-01") to 1) // 1 person  //2021-04-07
        episimConfig.setInfections_pers_per_day(VirusStrain.MUTB, infPerDayMUTB)
        virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).apply {
            infectiousness = 2.5
            vaccineEffectiveness = 0.8 // we can tweak it
            reVaccineEffectiveness = 1.0
            factorSeriouslySickVaccinated = 0.05 / (1 - 0.8)
        }

        // Vaccination compliance by age
        vaccinationConfig.setVaccinationCapacity_pers_per_day(vaccinations)

        // Vaccinate everybody with age above 0
        val vaccinationCompliance: MutableMap<Int, Double> = hashMapOf(0 to 1.0) // Age group wise vaccination?
        vaccinationConfig.setCompliancePerAge(vaccinationCompliance)


        // Policy and restrictions
        val restrictions = CreateRestrictionsFromCSV(episimConfig)
        // restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210531.csv"));
        restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210709.csv"))

        // restrictions.setExtrapolation(EpisimUtils.Extrapolation.linear); // TODO
        //

        // Using the same base policy as berlin
        val builder = BasePolicyBuilder(episimConfig)
        builder.activityParticipation = restrictions
        val policy = builder.build()

        // Set compliance rate of 90% for cloth masks
        policy.restrict(LocalDate.parse("2020-04-01"), Restriction.ofMask(FaceMask.CLOTH, 0.9), "pt")

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

        //LocalDate testingDate = LocalDate.parse("2021-04-19");
        episimConfig.setPolicy(FixedPolicy::class.java, policy.build())
        config.controler().outputDirectory = "output-snz-dresden"
        return config
    }

    companion object {
        /**
         * Path pointing to the input folder. Needs to be adapted or set using the EPISIM_INPUT environment variable.
         */
        // public static final Path INPUT = EpisimUtils.resolveInputPath("../shared-svn/projects/episim/matsim-files/snz/Dresden/episim-input");
        val INPUT = EpisimUtils.resolveInputPath("dresden")
    }

    private fun getEpisimConfig(episimConfig: EpisimConfigGroup): EpisimConfigGroup = episimConfig
}