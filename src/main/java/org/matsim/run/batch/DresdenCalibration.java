package org.matsim.run.batch;

import com.google.inject.AbstractModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.*;
import org.matsim.run.RunParallel;
import org.matsim.run.modules.SnzDresdenScenario_;

import javax.annotation.Nullable;


/**
 * Calibration for Dresden scenario
 */
public class DresdenCalibration implements BatchRun<DresdenCalibration.Params> {

	@Override
	public AbstractModule getBindings(int id, @Nullable Params params) {
		return new SnzDresdenScenario_();
	}

	@Override
	public Metadata getMetadata() {
		return Metadata.of("dresden", "calibration");
	}

//	@Override
//	public int getOffset() {
//		return 10000;
//	}

	@Override
	public Config prepareConfig(int id, Params params) {

		SnzDresdenScenario_ module = new SnzDresdenScenario_();

		Config config = module.config();
		config.global().setRandomSeed(params.seed);

		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		episimConfig.setCalibrationParameter(episimConfig.getCalibrationParameter() * params.thetaFactor);

		//episimConfig.setHospitalFactor(); TODO

		return config;
	}

	public static final class Params {

		@GenerateSeeds(1)
		public long seed;

		@Parameter({0.5/*, 0.6/*, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5*/})

		//@Parameter({1})
		double thetaFactor;

	}

	public static void main(String[] args) {
		String[] args2 = {
				RunParallel.OPTION_SETUP, DresdenCalibration.class.getName(),
				RunParallel.OPTION_PARAMS, Params.class.getName(),
				RunParallel.OPTION_TASKS, Integer.toString(1),
				RunParallel.OPTION_ITERATIONS, Integer.toString(40),
				RunParallel.OPTION_METADATA
		};

		RunParallel.main(args2);
	}


}

