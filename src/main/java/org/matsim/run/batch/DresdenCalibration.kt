package org.matsim.run.batch

import org.matsim.core.config.Config
import org.matsim.episim.BatchRun
import org.matsim.episim.EpisimConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.episim.BatchRun.GenerateSeeds
import org.matsim.episim.BatchRun.IntParameter
import org.matsim.episim.BatchRun.Parameter
import org.matsim.episim.BatchRun.StringParameter
import org.matsim.episim.EpisimPerson
import org.matsim.episim.TestingConfigGroup
import org.matsim.episim.VaccinationConfigGroup
import org.matsim.episim.VirusStrainConfigGroup
import org.matsim.episim.model.Transition
import org.matsim.episim.model.VaccinationType
import org.matsim.episim.model.VirusStrain
import org.matsim.episim.model.testing.TestType
import org.matsim.episim.policy.FixedPolicy
import org.matsim.episim.policy.Restriction
import kotlin.jvm.JvmStatic
import org.matsim.run.RunParallel
import org.matsim.run.modules.SnzDresdenScenario
import java.time.LocalDate

/**
 * Calibration for Dresden scenario
 */
class DresdenCalibration : BatchRun<DresdenCalibration.Params?> {

    override fun getBindings(id: Int, params: Params?): SnzDresdenScenario = SnzDresdenScenario.Builder().run {
        scale = params?.scale ?: 1.0
        setActivityHandling(EpisimConfigGroup.ActivityHandling.startOfDay)
//				.setLeisureOffset( params == null ? 0d : params.leisureOffset)
//				.setLeisureNightly(leisureNightly)
//				.setLeisureNightlyScale(leisureNightlyScale)
        build()
    }

    override fun getMetadata(): BatchRun.Metadata = BatchRun.Metadata.of("dresden", "calibration")

    //	@Override
    //	public int getOffset() {
    //		return 10000;
    //	}
    override fun prepareConfig(id: Int, params: Params?): Config? {
//        val module = SnzDresdenScenario()
//        val config = module.config()
//        config.global().randomSeed = params.seed
//        val episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup::class.java)
//        episimConfig.calibrationParameter = episimConfig.calibrationParameter * params.thetaFactor
//        episimConfig.calibrationParameter = episimConfig.calibrationParameter * params.OMI_inf
//
//        //episimConfig.setHospitalFactor(); TODO
//        return config

        val module = getBindings(id, params)

        val config = module.config()

        config.global().randomSeed = params!!.seed

        val episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup::class.java).apply {
            progressionConfig = progressionConfig(params, Transition.config()).build()
            daysInfectious = Integer.MAX_VALUE
//            calibrationParameter *= 0.83 * params.thetaFactor
            calibrationParameter = 1.56E-5 * 0.2 *0.2* params.thetaFactor
        }

        //restrictions
        val builder = FixedPolicy.parse(episimConfig.policy)

        // TODO
        //builder.setHospitalScale(2.0);

        builder.restrict("2021-04-17", Restriction.ofClosingHours(21, 5), "leisure", "visit")
        val curfewCompliance = mapOf<LocalDate, Double>(
                LocalDate("2021-04-17") to 1.0,
                LocalDate("2021-05-31") to 0.0)
        episimConfig.setCurfewCompliance(curfewCompliance);

        builder.restrict(LocalDate("2021-10-18"), 1.0, "educ_higher")
        builder.restrict(LocalDate("2021-12-20"), 0.2, "educ_higher")
        builder.restrict(LocalDate("2022-01-02"), 1.0, "educ_higher")

//        builder.apply("2020-10-15", "2020-12-14", { d, e ->
//            e["fraction"] = 1 - params.leisureFactor * (1 - e["fraction"] as Double)
//        }, "leisure")

        episimConfig.policy = builder.build()

//        val importMap = HashMap<LocalDate, Int>()
//        val importFactorBeforeJune = 1.0
//        val imprtFctMult = 1.0
//        val importOffset = 0L
//        val dresdenFactor = 1
//
//        SnzDresdenScenario.interpolateImport(importMap, dresdenFactor * imprtFctMult * importFactorBeforeJune,
//                LocalDate("2020-02-24").plusDays(importOffset),
//                LocalDate("2020-03-09").plusDays(importOffset), 0.9, 23.1)
//        SnzDresdenScenario.interpolateImport(importMap, dresdenFactor * imprtFctMult * importFactorBeforeJune,
//                LocalDate("2020-03-09").plusDays(importOffset),
//                LocalDate("2020-03-23").plusDays(importOffset), 23.1, 3.9)
//        SnzDresdenScenario.interpolateImport(importMap, dresdenFactor * imprtFctMult * importFactorBeforeJune,
//                LocalDate("2020-03-23").plusDays(importOffset),
//                LocalDate("2020-04-13").plusDays(importOffset), 3.9, 0.1)
//
////        importMap[LocalDate("2020-07-19")] = (params.summerImportFactor * 32).toInt()
////        importMap[LocalDate("2020-08-09")] = 1
////
//        episimConfig.setInfections_pers_per_day(importMap)

        //weather model
//        episimConfig.leisureOutdoorFraction = EpisimUtils.getOutDoorFractionFromDateAndTemp2(
//                SnzDresdenScenario_test.INPUT.resolve("cologneWeather.csv").toFile(),
//                SnzDresdenScenario_test.INPUT.resolve("weatherDataAvgCologne2000-2020.csv").toFile(),
//                0.5, 18.5, 25.0, 18.5, 25.0, 5.0, params.alpha)

        //mutations and vaccinations
        val vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup::class.java)
        val virusStrainConfigGroup = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java)

        val infPerDayBase: MutableMap<LocalDate, Int> = hashMapOf(
                LocalDate.parse("2020-02-24") to 4, //    LocalDate.parse("2020-01-01") to 0,
                LocalDate.parse("2020-04-02") to 0) // "2020-10-01")
        episimConfig.setInfections_pers_per_day(VirusStrain.SARS_CoV_2, infPerDayBase)


        val infPerDayB117 = hashMapOf<LocalDate, Int>(
                LocalDate("2020-01-01") to 0,
                LocalDate("2020-09-07") to params.importB117_1,
//                LocalDate("2021-01-01") to 0,
                LocalDate(params.alphaDate) to params.importB117_2 )
        episimConfig.setInfections_pers_per_day(VirusStrain.B117, infPerDayB117)   // Alpha variant (UK VAriant)

        virusStrainConfigGroup.getOrAddParams(VirusStrain.B117).apply {
            infectiousness = 1.45
//            factorSeriouslySick = 1.0
        }

        val infPerDayMUTB = hashMapOf<LocalDate, Int>(
                LocalDate("2020-01-01") to 0,
                LocalDate("2021-07-01") to 1,
                LocalDate("2021-10-01") to 2)

//        val importFactor = 0.0
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 1.0, LocalDate("2021-06-14").plusDays(0),
//                LocalDate("2021-06-21").plusDays(0), 1.0, 0.5 * importFactor * 1.6)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-06-21").plusDays(0),
//                LocalDate("2021-06-28").plusDays(0), 1.6, 2.8)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-06-28").plusDays(0),
//                LocalDate("2021-07-05").plusDays(0), 2.8, 4.6)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-07-05").plusDays(0),
//                LocalDate("2021-07-12").plusDays(0), 4.6, 5.9)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-07-12").plusDays(0),
//                LocalDate("2021-07-19").plusDays(0), 5.9, 7.3)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-07-19").plusDays(0),
//                LocalDate("2021-07-26").plusDays(0), 7.3, 10.2)
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 0.5 * importFactor, LocalDate("2021-07-26").plusDays(0),
//                LocalDate("2021-08-02").plusDays(0), 10.2, 13.2)
//
//        SnzDresdenScenario.interpolateImport(infPerDayMUTB, 1.0, LocalDate("2021-08-09").plusDays(0),
//                LocalDate("2021-08-31").plusDays(0), 0.5 * importFactor * 13.2, 1.0)


        episimConfig.setInfections_pers_per_day(VirusStrain.MUTB, infPerDayMUTB)
        virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).apply {
            infectiousness = params.deltaInf
            factorSeriouslySick = params.deltaSeriouslySick
        }




        val infPerDayOMICRON: MutableMap<LocalDate, Int> = hashMapOf(
                LocalDate.parse("2020-01-01") to 0,
                LocalDate.parse("2021-12-10") to 2) // 1 person  //Need to change the date

        episimConfig.setInfections_pers_per_day(VirusStrain.OMICRON, infPerDayOMICRON)


        ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup::class.java)
                .getOrAddParams(VirusStrain.OMICRON).infectiousness = params.OMI_inf



        val effectivnessMRNA = params.deltaVacEffect
        val factorShowingSymptomsMRNA = 0.12 / (1 - effectivnessMRNA)
        val factorSeriouslySickMRNA = 0.02 / ((1 - effectivnessMRNA) * factorShowingSymptomsMRNA)
        val fullEffectMRNA = 7 * 7; //second shot after 6 weeks, full effect one week after second shot
        vaccinationConfig.getOrAddParams(VaccinationType.mRNA)
                .setDaysBeforeFullEffect(fullEffectMRNA)
                .setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 0.0)
                        .atDay(fullEffectMRNA - 7, effectivnessMRNA / 2.0)
                        .atFullEffect(effectivnessMRNA)
                        .atDay(fullEffectMRNA + 5 * 365, 0.0)) //10% reduction every 6 months (source: TC)
                .setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectMRNA - 7, 1.0 - ((1.0 - factorShowingSymptomsMRNA) / 2.0))
                        .atFullEffect(factorShowingSymptomsMRNA)
                        .atDay(fullEffectMRNA + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)
                .setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectMRNA - 7, 1.0 - ((1.0 - factorSeriouslySickMRNA) / 2.0))
                        .atFullEffect(factorSeriouslySickMRNA)
                        .atDay(fullEffectMRNA + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)

        val effectivnessVector = params.deltaVacEffect * 0.5 / 0.7
        val factorShowingSymptomsVector = 0.32 / (1 - effectivnessVector)
        val factorSeriouslySickVector = 0.15 / ((1 - effectivnessVector) * factorShowingSymptomsVector)
        val fullEffectVector = 10 * 7; //second shot after 9 weeks, full effect one week after second sho

        vaccinationConfig.getOrAddParams(VaccinationType.vector)
                .setDaysBeforeFullEffect(fullEffectVector)
                .setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 0.0)
                        .atDay(fullEffectVector - 7, effectivnessVector / 2.0)
                        .atFullEffect(effectivnessVector)
                        .atDay(fullEffectVector + 5 * 365, 0.0)) //10% reduction every 6 months (source: TC)
                .setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectVector - 7, 1.0 - ((1.0 - factorShowingSymptomsVector) / 2.0))
                        .atFullEffect(factorShowingSymptomsVector)
                        .atDay(fullEffectVector + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)
                .setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectVector - 7, 1.0 - ((1.0 - factorSeriouslySickVector) / 2.0))
                        .atFullEffect(factorSeriouslySickVector)
                        .atDay(fullEffectVector + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)

        val effectivnesssubunit = params.deltaVacEffect * 0.5 / 0.7
        val factorShowingSymptomssubunit = 0.32 / (1 - effectivnesssubunit)
        val factorSeriouslySicksubunit = 0.15 / ((1 - effectivnesssubunit) * factorShowingSymptomssubunit)
        val fullEffectsubunit = 10 * 7; //second shot after 9 weeks, full effect one week after second sho

        vaccinationConfig.getOrAddParams(VaccinationType.subunit)
                .setDaysBeforeFullEffect(fullEffectsubunit)
                .setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 0.0)
                        .atDay(fullEffectsubunit - 7, effectivnesssubunit / 2.0)
                        .atFullEffect(effectivnesssubunit)
                        .atDay(fullEffectsubunit + 5 * 365, 0.0)) //10% reduction every 6 months (source: TC)
                .setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectsubunit - 7, 1.0 - ((1.0 - factorShowingSymptomssubunit) / 2.0))
                        .atFullEffect(factorShowingSymptomssubunit)
                        .atDay(fullEffectsubunit + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)
                .setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.MUTB)
                        .atDay(1, 1.0)
                        .atDay(fullEffectsubunit - 7, 1.0 - ((1.0 - factorSeriouslySicksubunit) / 2.0))
                        .atFullEffect(factorSeriouslySicksubunit)
                        .atDay(fullEffectsubunit + 5 * 365, 1.0)) //10% reduction every 6 months (source: TC)

//        val vaccinationCompliance = hashMapOf<Integer, Double>()
//        for (i in 0 until 12) vaccinationCompliance.put(i, 0.0);
//        for (int i = 12; i < 18; i++) vaccinationCompliance.put(i, 0.7);
//        for (int i = 18; i < 25; i++) vaccinationCompliance.put(i, 0.7);
//        for (int i = 25; i < 40; i++) vaccinationCompliance.put(i, 0.75);
//        for (int i = 40; i < 65; i++) vaccinationCompliance.put(i, 0.8);
//        for (int i = 65; i <= 120; i++) vaccinationCompliance.put(i, 0.9);
//        vaccinationConfig.setCompliancePerAge(vaccinationCompliance);

        //testing
        val rapidTest: TestingConfigGroup.TestingParams
        val pcrTest: TestingConfigGroup.TestingParams
        ConfigUtils.addOrGetModule(config, TestingConfigGroup::class.java).apply {

//		TestType testType = TestType.valueOf(params.testType);

            rapidTest = getOrAddParams(TestType.RAPID_TEST).apply {
                falseNegativeRate = 0.3
                falsePositiveRate = 0.03
            }
            pcrTest = getOrAddParams(TestType.PCR).apply {
                falseNegativeRate = 0.1
                falsePositiveRate = 0.01
            }

            strategy = TestingConfigGroup.Strategy.ACTIVITIES

            setActivities(listOf("leisure", "work", "business", "educ_kiga", "educ_primary", "educ_secondary", "educ_tertiary", "educ_other", "educ_higher"))

            householdCompliance = 1.0
        }

        val testingStartDate = LocalDate("2021-03-19")

        val leisureTests = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)
        val workTests = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)
        val eduTests = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)

        for (i in 1..31) {
            val date = testingStartDate.plusDays(i.toLong())
            leisureTests[date] = 0.25 * i / 31.0
            workTests[date] = 0.25 * i / 31.0
            eduTests[date] = 0.8 * i / 31.0
        }


        eduTests[LocalDate("2021-06-24")] = 0.0
        workTests[LocalDate("2021-06-04")] = 0.05
//		workTests.put(LocalDate.parse("2021-09-06"),  params.rapidTestWork);


        leisureTests[LocalDate("2021-06-04")] = 0.05
//		leisureTests.put(LocalDate.parse("2021-08-23"),  0.2);

//		leisureTests.put(LocalDate.parse("2021-09-06"),  params.rapidTestLeis);


        eduTests[LocalDate("2021-08-06")] = 0.6
        eduTests[LocalDate("2021-08-30")] = 0.4
//		eduTests.put(LocalDate.parse("2021-09-06"),  params.rapidTestEdu);


        rapidTest.setTestingRatePerActivityAndDate(mapOf(
                "leisure" to leisureTests,
                "work" to workTests,
                "business" to workTests,
                "educ_kiga" to eduTests,
                "educ_primary" to eduTests,
                "educ_secondary" to eduTests,
                "educ_tertiary" to eduTests,
                "educ_higher" to eduTests,
                "educ_other" to eduTests))

        val leisureTestsPCR = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)
        val workTestsPCR = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)
        val eduTestsPCR = hashMapOf<LocalDate, Double>(LocalDate("2020-01-01") to 0.0)

//		eduTestsPCR.put(LocalDate.parse("2021-09-06"),  params.pcrTestEdu);
//		workTestsPCR.put(LocalDate.parse("2021-09-06"),  params.pcrTestWork);
//		leisureTestsPCR.put(LocalDate.parse("2021-09-06"),  params.pcrTestLeis);


//		eduTestsPCR.put(LocalDate.parse("2021-08-06"), 0.1);

        pcrTest.setTestingRatePerActivityAndDate(mapOf(
                "leisure" to leisureTestsPCR,
                "work" to workTestsPCR,
                "business" to workTestsPCR,
                "educ_kiga" to eduTestsPCR,
                "educ_primary" to eduTestsPCR,
                "educ_secondary" to eduTestsPCR,
                "educ_tertiary" to eduTestsPCR,
                "educ_higher" to eduTestsPCR,
                "educ_other" to eduTestsPCR))

        rapidTest.setTestingCapacity_pers_per_day(mapOf(
                LocalDate.of(1970, 1, 1) to 0,
                testingStartDate to Integer.MAX_VALUE))

        pcrTest.setTestingCapacity_pers_per_day(mapOf(
                LocalDate.of(1970, 1, 1) to 0,
                testingStartDate to Integer.MAX_VALUE))

        return config
    }

    class Params {
        @GenerateSeeds(1)
        var seed = 0L

        //		@Parameter({4.0})
//		double importFactor;

        @Parameter(1.0)
        var thetaFactor = 0.0

        @Parameter(1.0)
        var scale = 1.0

//        @Parameter(1.7, 1.8, 1.9, 2.0)
//        var leisureFactor = 0.0

        @Parameter(3.0)
        var OMI_inf = 0.0
        @IntParameter(0,1,2,3,4,5,6,7)
        val  importB117_2 = 0

        @IntParameter(0,1,2,3,4,5,6,7)
        val  importB117_1 = 0
        @StringParameter("2021-03-01")
        lateinit var alphaDate: String





//        @Parameter(1.45)
//        var alphaInf = 0.0
//		@StringParameter({"true-1.0", "true-1.1", "true-1.2", "true-1.3", "true-1.4", "false"})
//		String leisureNightly;

//		@Parameter({0.25, 0.3, 0.35})
//		double leisureOffset;



//        @Parameter(1.0)
//        var alpha = 0.0

        @Parameter(2.5)
        var deltaInf = 0.0

        @Parameter(0.7)
        var deltaVacEffect = 0.0

//        @Parameter(0.1,0.2,0.3,0.4,0.5)
//        var summerImportFactor = 0.0

//		@Parameter({0.25})
//		double tesRateLeisureWork;
//
//		@Parameter({0.05})
//		double tesRateLeisureWork2;

//		@StringParameter({"alpha", "0.5"})
//		String delta1Vac;

//		@StringParameter({"no"})
//		String schoolMasks;
//
//		@StringParameter({"2021-05-01"})
//		String deltaDate;

        @Parameter(2.0)
        var deltaSeriouslySick = 0.0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val args2 = arrayOf(
                    RunParallel.OPTION_SETUP, DresdenCalibration::class.java.name,
                    RunParallel.OPTION_PARAMS, Params::class.java.name,
                    RunParallel.OPTION_TASKS, 1.toString(),
                    RunParallel.OPTION_ITERATIONS, 5.toString(),
                    RunParallel.OPTION_METADATA)
            RunParallel.main(args2)
        }

        /**
         * Adds progression config to the given builder.
         * @param params
         */
        private fun progressionConfig(params: Params, builder: Transition.Builder): Transition.Builder {

            val transitionRecSus = Transition.logNormalWithMedianAndStd(180.0, 10.0)

            return builder
                    // Inkubationszeit: Die Inkubationszeit [ ... ] liegt im Mittel (Median) bei 5–6 Tagen (Spannweite 1 bis 14 Tage)
                    .from(EpisimPerson.DiseaseStatus.infectedButNotContagious,
                            Transition.to(EpisimPerson.DiseaseStatus.contagious, Transition.fixed(0)))

                    // Dauer Infektiosität:: Es wurde geschätzt, dass eine relevante Infektiosität bereits zwei Tage vor Symptombeginn vorhanden ist und die höchste Infektiosität am Tag vor dem Symptombeginn liegt
                    // Dauer Infektiosität: Abstrichproben vom Rachen enthielten vermehrungsfähige Viren bis zum vierten, aus dem Sputum bis zum achten Tag nach Symptombeginn
                    .from(EpisimPerson.DiseaseStatus.contagious,
                            Transition.to(EpisimPerson.DiseaseStatus.showingSymptoms, Transition.logNormalWithMedianAndStd(6.0, 6.0)),    //80%
                            Transition.to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(8.0, 8.0)))            //20%

                    // Erkankungsbeginn -> Hospitalisierung: Eine Studie aus Deutschland zu 50 Patienten mit eher schwereren Verläufen berichtete für alle Patienten eine mittlere (Median) Dauer von vier Tagen (IQR: 1–8 Tage)
                    .from(EpisimPerson.DiseaseStatus.showingSymptoms,
                            Transition.to(EpisimPerson.DiseaseStatus.seriouslySick, Transition.logNormalWithMedianAndStd(5.0, 5.0)),
                            Transition.to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(8.0, 8.0)))

                    // Hospitalisierung -> ITS: In einer chinesischen Fallserie betrug diese Zeitspanne im Mittel (Median) einen Tag (IQR: 0–3 Tage)
                    .from(EpisimPerson.DiseaseStatus.seriouslySick,
                            Transition.to(EpisimPerson.DiseaseStatus.critical, Transition.logNormalWithMedianAndStd(1.0, 1.0)),
                            Transition.to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(14.0, 14.0)))

                    // Dauer des Krankenhausaufenthalts: „WHO-China Joint Mission on Coronavirus Disease 2019“ wird berichtet, dass milde Fälle im Mittel (Median) einen Krankheitsverlauf von zwei Wochen haben und schwere von 3–6 Wochen
                    .from(EpisimPerson.DiseaseStatus.critical,
                            Transition.to(EpisimPerson.DiseaseStatus.seriouslySickAfterCritical, Transition.logNormalWithMedianAndStd(21.0, 21.0)))

                    .from(EpisimPerson.DiseaseStatus.seriouslySickAfterCritical,
                            Transition.to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(7.0, 7.0)))

                    .from(EpisimPerson.DiseaseStatus.recovered,
                            Transition.to(EpisimPerson.DiseaseStatus.susceptible, transitionRecSus))
        }

        fun LocalDate(date: String) = LocalDate.parse(date)
    }
}
