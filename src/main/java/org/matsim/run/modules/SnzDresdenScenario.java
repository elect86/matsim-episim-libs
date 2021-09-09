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

package org.matsim.run.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.*;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.model.*;
import org.matsim.episim.model.input.CreateRestrictionsFromCSV;
import org.matsim.episim.model.testing.TestType;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;

import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario for Dresden using Senozon data.
 */
public final class SnzDresdenScenario extends AbstractModule {

	/**
	 * Path pointing to the input folder. Needs to be adapted or set using the EPISIM_INPUT environment variable.
	 */
	// public static final Path INPUT = EpisimUtils.resolveInputPath("../shared-svn/projects/episim/matsim-files/snz/Dresden/episim-input");

	 public static final Path INPUT = EpisimUtils.resolveInputPath("dresden");


	// public static final Path INPUT = Path.of("/home/abhishek/Desktop/episim-dresden-libs/dresden-data");

	/**
	 * Empty constructor is needed for running scenario from command line.
	 */
	@SuppressWarnings("unused")
	public SnzDresdenScenario() {
	}

	@Override
	protected void configure() {
		bind(ContactModel.class).to(SymmetricContactModel.class).in(Singleton.class);
		bind(ProgressionModel.class).to(AgeDependentProgressionModel.class).in(Singleton.class);
		bind(InfectionModel.class).to(AgeDependentInfectionModelWithSeasonality.class).in(Singleton.class);
		bind(VaccinationModel.class).to(VaccinationByAge.class).in(Singleton.class);
	}


	@Provides
	@Singleton
	public Config config() {

		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());

		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.snz);
		episimConfig.setSampleSize(1);
		// Input files

		config.plans().setInputFile(INPUT.resolve("dresden_snz_entirePopulation_emptyPlans_withDistricts_100pt_split_noCoord.xml.gz").toString());

		episimConfig.addInputEventsFile(INPUT.resolve("dresden_snz_episim_events_wt_100pt_split.xml.gz").toString())
				.addDays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

		episimConfig.addInputEventsFile(INPUT.resolve("dresden_snz_episim_events_sa_100pt_split.xml.gz").toString())
				.addDays(DayOfWeek.SATURDAY);

		episimConfig.addInputEventsFile(INPUT.resolve("dresden_snz_episim_events_so_100pt_split.xml.gz").toString())
				.addDays(DayOfWeek.SUNDAY);


		// Calibration parameter

		episimConfig.setCalibrationParameter(2.5E-5 * 0.8); // TODO  //2.5E-5 * 0.8(calibrated)
		episimConfig.setStartDate("2020-03-02");

		//snapshot

		// episimConfig.setSnapshotInterval(100); // At every 100 days it will create a snapshot

		 // episimConfig.setStartFromSnapshot();  // 2020-12-27 put path as the argument zip file after creating episimConfig.setSnapshotInterval

		 // episimConfig.setSnapshotInterval();



		// Progression config

		episimConfig.setProgressionConfig(AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build());


		// Initial infections and import

		episimConfig.setInitialInfections(Integer.MAX_VALUE);
		episimConfig.setInfections_pers_per_day(Map.of(LocalDate.EPOCH, 1)); // base case import

		// Contact intensities

		int spaces = 20;
		episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(10.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("leisure").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
		episimConfig.getOrAddContainerParams("educ_kiga").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("educ_primary").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("educ_secondary").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("educ_tertiary").setContactIntensity(11.).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("educ_higher").setContactIntensity(5.5).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("educ_other").setContactIntensity(11.).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("shop_daily").setContactIntensity(0.88).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("shop_other").setContactIntensity(0.88).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("errands").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("business").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("visit").setContactIntensity(9.24).setSpacesPerFacility(spaces); // 33/3.57
		episimConfig.getOrAddContainerParams("home").setContactIntensity(1.0).setSpacesPerFacility(1); // 33/33
		episimConfig.getOrAddContainerParams("quarantine_home").setContactIntensity(1.0).setSpacesPerFacility(1); // 33/33


		// Tracing

		TracingConfigGroup tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);
//			int offset = (int) (ChronoUnit.DAYS.between(episimConfig.getStartDate(), LocalDate.parse("2020-04-01")) + 1);
		int offset = 46;
		tracingConfig.setPutTraceablePersonsInQuarantineAfterDay(offset);
		tracingConfig.setTracingProbability(0.5);
		tracingConfig.setTracingPeriod_days(2);
		tracingConfig.setMinContactDuration_sec(15 * 60.);
		tracingConfig.setQuarantineHouseholdMembers(true);
		tracingConfig.setEquipmentRate(1.);
		tracingConfig.setTracingDelay_days(5);
		tracingConfig.setTraceSusceptible(true);
		tracingConfig.setCapacityType(CapacityType.PER_PERSON);
		int tracingCapacity = 200;
		tracingConfig.setTracingCapacity_pers_per_day(Map.of(
				LocalDate.of(2020, 4, 1), (int) (tracingCapacity * 0.2),
				LocalDate.of(2020, 6, 15), tracingCapacity
		));


		// Vaccination capacity

		VaccinationConfigGroup vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
		vaccinationConfig.setEffectiveness(0.9);
		vaccinationConfig.setDaysBeforeFullEffect(28);

		Map<LocalDate, Integer> vaccinations = new HashMap<>();

		int population =862987;  // Berlin: 4_800_000;

		//vaccinations.put(LocalDate.parse("2020-01-01"), 0);
		vaccinations.put(LocalDate.parse("2020-12-28"), (int)  (823/7));
// the following is for Berlin; if we have data we can change it for dresden
		vaccinations.put(LocalDate.parse("2021-01-04"), (int) (2310/ 7));
		vaccinations.put(LocalDate.parse("2021-01-11"), (int) ( 4155/ 7));
		vaccinations.put(LocalDate.parse("2021-01-18"), (int) (4400/ 7));
		vaccinations.put(LocalDate.parse("2021-01-25"), (int) (4567/ 7));
		vaccinations.put(LocalDate.parse("2021-02-01"), (int) (6304/ 7));
		vaccinations.put(LocalDate.parse("2021-02-08"), (int) (5937/ 7));
		vaccinations.put(LocalDate.parse("2021-02-15"), (int) ( 6780 / 7));
		vaccinations.put(LocalDate.parse("2021-02-22"), (int) ( 9034 / 7));
		vaccinations.put(LocalDate.parse("2021-03-01"), (int) (9871/ 7));
		vaccinations.put(LocalDate.parse("2021-03-08"), (int) (7900 / 7));
		vaccinations.put(LocalDate.parse("2021-03-15"), (int) (7627 / 7));
		vaccinations.put(LocalDate.parse("2021-03-22"), (int) (10898 / 7));
		vaccinations.put(LocalDate.parse("2021-03-29"), (int) (8464/ 7));
		vaccinations.put(LocalDate.parse("2021-04-05"), (int) (17411/ 7));
		vaccinations.put(LocalDate.parse("2021-04-12"), (int) (21983 / 7));
		vaccinations.put(LocalDate.parse("2021-04-19"), (int) (19564 / 7));
     	vaccinations.put(LocalDate.parse("2021-04-26"), (int) (25340/ 7));
		vaccinations.put(LocalDate.parse("2021-05-03"), (int) (27635 / 7));
		vaccinations.put(LocalDate.parse("2021-05-10"), (int) (25711 / 7));
		vaccinations.put(LocalDate.parse("2021-05-17"), (int) ( 27500/ 7));
		vaccinations.put(LocalDate.parse("2021-05-24"), (int) (25708 / 7));
		vaccinations.put(LocalDate.parse("2021-05-31"), (int) (28428/ 7));
		vaccinations.put(LocalDate.parse("2021-06-07"), (int) (28296/ 7));
		vaccinations.put(LocalDate.parse("2021-06-14"), (int) (30633/ 7));
		vaccinations.put(LocalDate.parse("2021-06-21"), (int) (29838/ 7));
		vaccinations.put(LocalDate.parse("2021-06-28"), (int) (28438/ 7));
		vaccinations.put(LocalDate.parse("2021-07-05"), (int) (26057/ 7));
		vaccinations.put(LocalDate.parse("2021-07-12"), (int) (22527/ 7));
		vaccinations.put(LocalDate.parse("2021-07-19"), (int) (20560/ 7));
		vaccinations.put(LocalDate.parse("2021-07-26"), (int) (20779/ 7));
		vaccinations.put(LocalDate.parse("2021-08-02"), (int) (13450/ 7));


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
		Map<LocalDate, Integer> infPerDayB117 = new HashMap<>();
		infPerDayB117.put(LocalDate.parse("2020-01-01"), 0);
		infPerDayB117.put(LocalDate.parse("2020-10-01"), 1);  // "2020-11-30")
		episimConfig.setInfections_pers_per_day(VirusStrain.B117, infPerDayB117);

		// VaccinationConfigGroup vaccinationConfig = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
		double vaccineEffectiveness = vaccinationConfig.getEffectiveness();

		VirusStrainConfigGroup virusStrainConfigGroup = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup.class);

		virusStrainConfigGroup.getOrAddParams(VirusStrain.B117).setInfectiousness(1.2); // 1.8
		virusStrainConfigGroup.getOrAddParams(VirusStrain.B117).setVaccineEffectiveness(1.0);
		virusStrainConfigGroup.getOrAddParams(VirusStrain.B117).setFactorSeriouslySick(1.5);
		virusStrainConfigGroup.getOrAddParams(VirusStrain.B117).setFactorSeriouslySickVaccinated(0.05 / (1-vaccineEffectiveness));

		virusStrainConfigGroup.getOrAddParams(VirusStrain.SARS_CoV_2).setFactorSeriouslySickVaccinated(0.05 / (1-vaccineEffectiveness));

		Map<LocalDate, Integer> infPerDayMUTB = new HashMap<>();
		infPerDayMUTB.put(LocalDate.parse("2020-01-01"), 0);
		infPerDayMUTB.put(LocalDate.parse("2021-02-01"), 1); // 1 person  //2021-04-07
		episimConfig.setInfections_pers_per_day(VirusStrain.MUTB, infPerDayMUTB);
		virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).setInfectiousness(2.5);
		virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).setVaccineEffectiveness(0.8); // we can tweak it
		virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).setReVaccineEffectiveness(1.0);
		virusStrainConfigGroup.getOrAddParams(VirusStrain.MUTB).setFactorSeriouslySickVaccinated(0.05 / (1- 0.8));

		// Vaccination compliance by age
		Map<Integer, Double> vaccinationCompliance = new HashMap<>();
		vaccinationConfig.setVaccinationCapacity_pers_per_day(vaccinations);

		// Vaccinate everybody with age above 0
		vaccinationCompliance.put(0, 1d);   // Age group wise vaccination?


		vaccinationConfig.setCompliancePerAge(vaccinationCompliance);


		// Policy and restrictions
		CreateRestrictionsFromCSV restrictions = new CreateRestrictionsFromCSV(episimConfig);
		// restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210531.csv"));
		restrictions.setInput(INPUT.resolve("DresdenSnzData_daily_until20210709.csv"));

		// restrictions.setExtrapolation(EpisimUtils.Extrapolation.linear); // TODO
//

		// Using the same base policy as berlin
		SnzBerlinScenario25pct2020.BasePolicyBuilder builder = new SnzBerlinScenario25pct2020.BasePolicyBuilder(episimConfig);
		builder.setActivityParticipation(restrictions);
		FixedPolicy.ConfigBuilder policy = builder.build();

		// Set compliance rate of 90% for cloth masks
		policy.restrict(LocalDate.parse("2020-04-01"), Restriction.ofMask(FaceMask.CLOTH, 0.9), "pt");

		// Testing rates

		TestingConfigGroup testingConfigGroup = ConfigUtils.addOrGetModule(config, TestingConfigGroup.class);
		testingConfigGroup.setStrategy(TestingConfigGroup.Strategy.ACTIVITIES);

		testingConfigGroup.setStrategy(TestingConfigGroup.Strategy.ACTIVITIES);

		List<String> actsList = new ArrayList<String>();
		actsList.add("leisure");
		actsList.add("work");
		actsList.add("business");
		actsList.add("educ_kiga");
		actsList.add("educ_primary");
		actsList.add("educ_secondary");
		actsList.add("educ_tertiary");
		actsList.add("educ_other");
		actsList.add("educ_higher");
		testingConfigGroup.setActivities(actsList);

		testingConfigGroup.getParams(TestType.RAPID_TEST).setFalseNegativeRate(0.3);
		testingConfigGroup.getParams(TestType.RAPID_TEST).setFalsePositiveRate(0.03);

		// Test 10% of persons doing these activities
		testingConfigGroup.getParams(TestType.RAPID_TEST).setTestingRate(0.1);
		// All households can get tested
		testingConfigGroup.setHouseholdCompliance(1.0);

		//LocalDate testingDate = LocalDate.parse("2021-04-19");

		episimConfig.setPolicy(FixedPolicy.class, policy.build());
		config.controler().setOutputDirectory("output-snz-dresden");

		return config;
	}

	private EpisimConfigGroup getEpisimConfig(EpisimConfigGroup episimConfig) {
		return episimConfig;
	}
}
