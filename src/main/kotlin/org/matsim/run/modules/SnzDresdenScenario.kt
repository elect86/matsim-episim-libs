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
import org.matsim.core.config.groups.VspExperimentalConfigGroup
import org.matsim.episim.*
import org.matsim.episim.TracingConfigGroup.CapacityType
import org.matsim.episim.model.*
import org.matsim.episim.model.input.CreateRestrictionsFromCSV
import org.matsim.episim.model.progression.AgeDependentDiseaseStatusTransitionModel
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel
import org.matsim.episim.model.testing.TestType
import org.matsim.episim.model.vaccination.VaccinationByAge
import org.matsim.episim.model.vaccination.VaccinationModel
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
        bind<DiseaseStatusTransitionModel>().to<AgeDependentDiseaseStatusTransitionModel>().`in`<Singleton>()
        bind<InfectionModel>().to<AgeDependentInfectionModelWithSeasonality>().`in`<Singleton>()
        bind<VaccinationModel>().to<VaccinationByAge>().`in`<Singleton>()
    }

    @Provides
    @Singleton
    fun config(): Config {
        val config = ConfigUtils.createConfig(EpisimConfigGroup())
        // Turn off MATSim related warnings https://github.com/matsim-org/matsim-episim-libs/issues/91
        config.vspExperimental().vspDefaultsCheckingLevel = VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore
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
            calibrationParameter =1.56E-5*0.2 //
            setStartDate("2020-02-24")

            //snapshot

            // episimConfig.setSnapshotInterval(350); // At every 100 days it will create a snapshot

            //            episimConfig.setStartFromSnapshot("output-snz-dresden/episim-snapshot-350-2021-02-07.zip");  // 2020-12-27 put path as the argument zip file after creating episimConfig.setSnapshotInterval

            // episimConfig.setSnapshotInterval();


            // Progression config
            progressionConfig = AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build()


            // Initial infections and import
            initialInfections = Int.MAX_VALUE
            // setInfections_pers_per_day(mapOf(LocalDate.EPOCH to 1)) // base case import


            val infPerDayBase: MutableMap<LocalDate, Int> = hashMapOf(
                    LocalDate.parse("2020-02-24") to 5, //    LocalDate.parse("2020-01-01") to 0,
                    LocalDate.parse("2020-04-02") to 0,
                    LocalDate.parse("2020-10-01") to 1,
                    LocalDate.parse("2020-10-15") to 2) // "2020-10-01")
            episimConfig.setInfections_pers_per_day(VirusStrain.SARS_CoV_2, infPerDayBase)


            //inital infections and import

            /*  val imprtFctMult = 1.0
              val importFactorBeforeJune = 4.0
              val importFactorAfterJune = 0.5
              val importOffset = 0
              episimConfig.initialInfections = Int.MAX_VALUE
              //if (this.diseaseImport != DiseaseImport.no) {
                  episimConfig.initialInfectionDistrict = null
                  val importMap: Map<LocalDate, Int> = java.util.HashMap()
                  SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-02-24").plusDays(importOffset.toLong()),
                          LocalDate.parse("2020-03-09").plusDays(importOffset.toLong()), 0.9, 23.1)
                  SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-03-09").plusDays(importOffset.toLong()),
                          LocalDate.parse("2020-03-23").plusDays(importOffset.toLong()), 23.1, 3.9)
                  SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorBeforeJune, LocalDate.parse("2020-03-23").plusDays(importOffset.toLong()),
                          LocalDate.parse("2020-04-13").plusDays(importOffset.toLong()), 3.9, 0.1)
               //   if (this.diseaseImport == DiseaseImport.yes) {
                      SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorAfterJune, LocalDate.parse("2020-06-08").plusDays(importOffset.toLong()),
                              LocalDate.parse("2020-07-13").plusDays(importOffset.toLong()), 0.1, 2.7)
                      SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorAfterJune, LocalDate.parse("2020-07-13").plusDays(importOffset.toLong()),
                              LocalDate.parse("2020-08-10").plusDays(importOffset.toLong()), 2.7, 17.9)
                      SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorAfterJune, LocalDate.parse("2020-08-10").plusDays(importOffset.toLong()),
                              LocalDate.parse("2020-09-07").plusDays(importOffset.toLong()), 17.9, 6.1)
                      SnzBerlinProductionScenario.interpolateImport(importMap, imprtFctMult * importFactorAfterJune, LocalDate.parse("2020-10-26").plusDays(importOffset.toLong()),
                              LocalDate.parse("2020-12-21").plusDays(importOffset.toLong()), 6.1, 1.1)
               //   }
                  episimConfig.setInfections_pers_per_day(importMap)*/
            // }


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


        val vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup::class.java)

        configureVaccines(vaccinationConfig)

        //mutations and vaccinations
        val infPerDayB117: MutableMap<LocalDate, Int> = hashMapOf(
                LocalDate.parse("2020-01-01") to 0,
                LocalDate.parse("2020-09-1") to 1 ) // "2020-09-21")
        episimConfig.setInfections_pers_per_day(VirusStrain.B117, infPerDayB117)   // Alpha variant (UK VAriant)


        ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java).getOrAddParams(VirusStrain.B117).apply {
            infectiousness = 1.7 // 1.8
        }


        val infPerDayDELTA: MutableMap<LocalDate, Int> = hashMapOf(
                LocalDate.parse("2020-01-01") to 0,
                LocalDate.parse("2021-03-01") to 2,
                LocalDate.parse("2021-05-01") to 0,
                LocalDate.parse("2021-07-01") to 2 ) // 1 person  //Need to change the date

        episimConfig.setInfections_pers_per_day(VirusStrain.DELTA, infPerDayDELTA)


        ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java)
                .getOrAddParams(VirusStrain.DELTA).infectiousness = 2.5 // 1.8


        val infPerDayOMICRON: MutableMap<LocalDate, Int> = hashMapOf(
                LocalDate.parse("2020-01-01") to 0,
                LocalDate.parse("2021-12-10") to 4) // 1 person  //Need to change the date

        episimConfig.setInfections_pers_per_day(VirusStrain.OMICRON, infPerDayOMICRON)


        ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java)
                .getOrAddParams(VirusStrain.OMICRON).infectiousness = 4.0 //


        // VaccinationConfigGroup vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
        //        val vaccineEff = vaccinationParams.effectiveness


        //                vaccineEffectiveness = 1.0
        //                factorSeriouslySick = 1.5
        //                factorSeriouslySickVaccinated = 0.05 / (1 - vaccineEff)
        //            }
        //            getOrAddParams(VirusStrain.SARS_CoV_2).factorSeriouslySickVaccinated = 0.05 / (1 - vaccineEff)
        //        }
//         val infPerDayMUTB: MutableMap<LocalDate, Int> = hashMapOf(
//                  LocalDate.parse("2020-01-01") to 0,
//                  LocalDate.parse("2021-02-07") to 1) // 1 person  //2021-02-01
//          //    LocalDate.parse("2021-07-01") to 0) // Added
//         episimConfig.setInfections_pers_per_day(VirusStrain.MUTB, infPerDayMUTB)
//        ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java)
//                .getOrAddParams(VirusStrain.MUTB).infectiousness = 2.3 //


        //            vaccineEffectiveness = 0.8 // we can tweak it
        //            reVaccineEffectiveness = 1.0
        //            factorSeriouslySick = 1.5
        //            factorSeriouslySickVaccinated = 0.05 / (1 - 0.8)
        //        }
        //


        //            vaccineEffectiveness = 0.8 // we can tweak it
        //            reVaccineEffectiveness = 1.0

        //            factorSeriouslySick = 1.2
        //            factorSeriouslySickVaccinated = 0.05 / (1 - 0.8)
        //        }
        //
        //
        //        // Vaccination compliance by age
        //        vaccinationConfig.setVaccinationCapacity_pers_per_day(vaccinations)

        // Vaccinate everybody with age above 0
        val vaccinationCompliance: MutableMap<Int, Double> = hashMapOf(0 to 1.0) // Age group wise vaccination?
        vaccinationConfig.setCompliancePerAge(vaccinationCompliance)


        // Policy and restrictions
        val restrictions = CreateRestrictionsFromCSV(episimConfig)
// restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210531.csv"));
        restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210917.csv"))

// restrictions.setExtrapolation(EpisimUtils.Extrapolation.linear); // TODO
//

        // Using the same base policy as berlin
        val builder = BasePolicyBuilder(episimConfig)
        builder.activityParticipation = restrictions
        val policy = builder.buildFixed()

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
//        episimConfig.setPolicy(FixedPolicy::
//        class.java, policy.build())
//        config.controler().outputDirectory = "output-snz-dresden"
        return config
    }

    fun configureVaccines(vaccinationConfig: VaccinationConfigGroup) {
        // Vaccination capacity

        // https://impfdashboard.de/static/data/germany_vaccinations_timeseries_v2.tsv  Information about the share of different types of vaccination // NEED to automate this (Giuseppe)
        class Vax(val type: VaccinationType,
                  val effectiveness: Double,
                  showingSymptoms: Double,
                  seriouslySick: Double,
                  val fullEffect: Int) {
            val factorShowingSymptoms = showingSymptoms / (1 - effectiveness)
            val factorSeriouslySick = seriouslySick / ((1 - effectiveness) * factorShowingSymptoms)
        }
// we assumed that the vaccines has same effect for all the strain
        // We can assign different effect corresponding to different strains following COLOGNE scenario
        val vaccines = listOf(
                Vax(VaccinationType.mRNA, 0.9, 0.05, 0.02, 7*7), //second shot after 6 weeks, full effect one week after second shot
                Vax(VaccinationType.vector, 0.5, 0.25, 0.02, 10 * 7), //second shot after 9 weeks, full effect one week after second shot
                Vax(VaccinationType.subunit, 0.86, 0.05, 0.02, 7*7))  // Look for the information regarding subunit
        for (vax in vaccines) {
            vaccinationConfig.getOrAddParams(vax.type).apply {
                daysBeforeFullEffect = vax.fullEffect
                val fullEffect = vax.fullEffect + 5 * 365
                setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.SARS_CoV_2)
                        .atDay(1, 0.0)
                        .atFullEffect(vax.effectiveness)
                        .atDay(fullEffect, 0.0)) //10% reduction every 6 months (source: TC)
                setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.B117)
                        .atDay(1, 0.0)
                        .atFullEffect(vax.effectiveness)
                        .atDay(fullEffect, 0.0)) //10% reduction every 6 months (source: TC)

                setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.DELTA)
                        .atDay(1, 0.0)
                        .atFullEffect(vax.effectiveness)
                        .atDay(fullEffect, 0.0)) //10% reduction every 6 months (source: TC)
                setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
                        .atDay(1, 0.0)
                        .atFullEffect(vax.effectiveness)
                        .atDay(fullEffect, 0.0)) //10% reduction every 6 months (source: TC)


                setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.SARS_CoV_2)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorShowingSymptoms)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)
                setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.B117)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorShowingSymptoms)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)

                setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.DELTA)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorShowingSymptoms)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)
                setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorShowingSymptoms)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)






                setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.SARS_CoV_2)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorSeriouslySick)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)
                setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.B117)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorSeriouslySick)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)
                setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.DELTA)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorSeriouslySick)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)
                setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
                        .atDay(1, 1.0)
                        .atFullEffect(vax.factorSeriouslySick)
                        .atDay(fullEffect, 1.0)) //10% reduction every 6 months (source: TC)

            }
        }

//

        var url = "https://raw.githubusercontent.com/robert-koch-institut/COVID-19-Impfungen_in_Deutschland/master/Aktuell_Deutschland_Bundeslaender_COVID-19-Impfungen.csv"
        val share: MutableMap<LocalDate, Map<VaccinationType, Double>> = mutableMapOf()
        var rows = csvReader().readAll(URL(url).readText()).filter { it[1] == "14" && it[3] == "1" }
        var week = mutableMapOf<VaccinationType, Double>()
        var startDate = LocalDate.parse(rows.first()[0])
        var endDate = startDate.plusDays(7)
        for (row in rows) {
            val date = LocalDate.parse(row[0])
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

        startDate = LocalDate.parse("2020-12-28")
        endDate = startDate.plusDays(7)
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
        vaccinationConfig.setVaccinationCapacity_pers_per_day(vaccinations)
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
